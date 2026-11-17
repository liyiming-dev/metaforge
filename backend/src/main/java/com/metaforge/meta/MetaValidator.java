package com.metaforge.meta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.EnumOption;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.meta.model.FormGroupDef;
import com.metaforge.meta.model.FormTabDef;
import com.metaforge.meta.model.ListViewDef;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;
import com.metaforge.meta.model.RelationDef;
import com.metaforge.meta.model.ServiceDef;
import com.metaforge.meta.model.ServiceTriggers;
import com.metaforge.meta.model.SystemFields;
import com.metaforge.meta.model.ViewsDef;

/**
 * 元模型的校验规则。保存模块或模型之前都要先过这里，不合规则就抛 400。
 *
 * 为什么校验这么重要：模型标识会变成表名，字段标识会变成列名，之后要拼进 SQL。
 * 这里是它们进入系统的唯一入口，在这里卡住格式，后面拼 SQL 才是安全的。
 *
 * 这个类不访问数据库，只做判断，所以可以直接写单元测试。
 */
public final class MetaValidator {

    /** 标识的格式：小写字母开头，只含小写字母、数字、下划线，最长 30 位 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,29}$");
    private static final String NAME_RULE = "要以小写字母开头，只含小写字母、数字、下划线，最长 30 位";

    private MetaValidator() {
    }

    public static void validateModule(ModuleDef module) {
        if (module == null) {
            throw bad("模块定义不能为空");
        }
        checkName(module.name(), "模块标识");
        checkText(module.displayName(), "模块 " + module.name() + " 的显示名");
    }

    /**
     * 校验一个模型定义。
     *
     * @param model   要保存的定义，可以是还没补默认值的
     * @param old     修改时传原来的定义，新建时传 null
     * @param context 能看到的所有模块和模型（已保存的，加上同一次导入里的）
     */
    public static void validateModel(ModelDef model, ModelDef old, MetaContext context) {
        if (model == null) {
            throw bad("模型定义不能为空");
        }
        checkName(model.name(), "模型标识");
        String where = "模型 " + model.name();
        checkText(model.displayName(), where + " 的显示名");
        if (model.module() == null || !context.moduleNames().contains(model.module())) {
            throw bad(where + " 所属的模块 " + model.module() + " 不存在");
        }
        if (model.fields() == null || model.fields().isEmpty()) {
            throw bad(where + " 至少要有一个字段");
        }

        Set<String> seen = new HashSet<>();
        for (FieldDef field : model.fields()) {
            validateField(model, field, seen, context);
        }
        if (old != null) {
            validateTypesUnchanged(model, old);
            validateRemovedFieldsUnused(model, old, context);
        }
        validateServices(model);
        validateViews(model);
    }

    // ---------- 字段 ----------

    private static void validateField(ModelDef model, FieldDef field, Set<String> seen, MetaContext context) {
        String where = "模型 " + model.name();
        if (field == null) {
            throw bad(where + " 的字段列表里有空项");
        }
        checkName(field.name(), where + " 的字段标识");
        if (SystemFields.NAMES.contains(field.name())) {
            throw bad(field.name() + " 是系统字段，不能用作字段标识");
        }
        if (!seen.add(field.name())) {
            throw bad(where + " 里字段标识 " + field.name() + " 重复");
        }
        String what = where + " 的字段 " + field.name();
        checkText(field.displayName(), what + " 的显示名");
        if (field.type() == null || !FieldTypes.ALL.contains(field.type())) {
            throw bad(what + " 的类型 " + field.type() + " 不支持");
        }

        switch (field.type()) {
            case FieldTypes.STRING -> checkRange(field.length(), 1, 2000, what + " 的长度");
            case FieldTypes.DECIMAL -> validateDecimal(field, what);
            case FieldTypes.ENUM -> validateOptions(field, what);
            case FieldTypes.MANY_TO_ONE, FieldTypes.ONE_TO_MANY -> validateRelation(model, field, what, context);
            default -> {
                // 其它类型没有额外的配置项
            }
        }

        boolean unique = Boolean.TRUE.equals(field.unique());
        if (unique && (FieldTypes.TEXT.equals(field.type()) || FieldTypes.ONE_TO_MANY.equals(field.type()))) {
            throw bad(what + " 是多行文本或一对多类型，不能设为唯一");
        }
    }

    private static void validateDecimal(FieldDef field, String what) {
        checkRange(field.precision(), 1, 38, what + " 的精度");
        int precision = field.precision() == null ? MetaDefaults.DEFAULT_PRECISION : field.precision();
        checkRange(field.scale(), 0, precision, what + " 的小数位数");
    }

    private static void validateOptions(FieldDef field, String what) {
        List<EnumOption> options = field.options();
        if (options == null || options.isEmpty()) {
            throw bad(what + " 是枚举类型，至少要有一个选项");
        }
        Set<String> values = new HashSet<>();
        for (EnumOption option : options) {
            if (option == null || isBlank(option.value()) || isBlank(option.label())) {
                throw bad(what + " 的每个选项都要有值和显示文字");
            }
            if (!values.add(option.value())) {
                throw bad(what + " 的选项值 " + option.value() + " 重复");
            }
        }
    }

    private static void validateRelation(ModelDef model, FieldDef field, String what, MetaContext context) {
        RelationDef relation = field.relation();
        if (relation == null || isBlank(relation.model())) {
            throw bad(what + " 是关联字段，必须指定关联的模型");
        }
        // 模型可以关联自己，这时要用正在保存的这一版定义
        ModelDef target = relation.model().equals(model.name()) ? model : context.models().get(relation.model());
        if (target == null) {
            throw bad(what + " 关联的模型 " + relation.model() + " 不存在");
        }

        if (FieldTypes.MANY_TO_ONE.equals(field.type())) {
            if (isBlank(relation.displayField())) {
                return;
            }
            FieldDef display = target.findField(relation.displayField());
            if (display == null || !FieldTypes.hasColumn(display.type())) {
                throw bad(what + " 的显示字段 " + relation.displayField() + " 在模型 " + target.name() + " 里不存在");
            }
            return;
        }

        // 一对多：mappedBy 必须是子表模型里指回本模型的多对一字段
        if (isBlank(relation.mappedBy())) {
            throw bad(what + " 是一对多字段，必须用 mappedBy 指定子表里的回指字段");
        }
        FieldDef back = target.findField(relation.mappedBy());
        boolean pointsBack = back != null
                && FieldTypes.MANY_TO_ONE.equals(back.type())
                && back.relation() != null
                && model.name().equals(back.relation().model());
        if (!pointsBack) {
            throw bad(what + " 的 mappedBy 必须是模型 " + target.name() + " 里指向 " + model.name() + " 的多对一字段");
        }
    }

    // ---------- 修改模型时的额外规则 ----------

    private static void validateTypesUnchanged(ModelDef model, ModelDef old) {
        for (FieldDef field : model.fields()) {
            FieldDef before = old.findField(field.name());
            if (before != null && !before.type().equals(field.type())) {
                throw bad("不能修改字段 " + field.name() + " 的类型（原来是 " + before.type() + "，现在是 " + field.type() + "）");
            }
        }
    }

    /** 被删掉的字段如果还被别的模型的关联配置引用着，就不让删。 */
    private static void validateRemovedFieldsUnused(ModelDef model, ModelDef old, MetaContext context) {
        if (old.fields() == null) {
            return;
        }
        for (FieldDef removed : old.fields()) {
            if (model.findField(removed.name()) != null) {
                continue;
            }
            for (ModelDef other : context.models().values()) {
                if (other.name().equals(model.name()) || other.fields() == null) {
                    continue;
                }
                for (FieldDef otherField : other.fields()) {
                    RelationDef relation = otherField == null ? null : otherField.relation();
                    if (relation == null || !model.name().equals(relation.model())) {
                        continue;
                    }
                    boolean used = removed.name().equals(relation.displayField())
                            || removed.name().equals(relation.mappedBy());
                    if (used) {
                        throw bad("不能删除字段 " + removed.name() + "：模型 " + other.name()
                                + " 的字段 " + otherField.name() + " 还在引用它");
                    }
                }
            }
        }
    }

    // ---------- 服务 ----------

    private static void validateServices(ModelDef model) {
        if (model.services() == null) {
            return;
        }
        String where = "模型 " + model.name();
        Set<String> seen = new HashSet<>();
        for (ServiceDef service : model.services()) {
            if (service == null) {
                throw bad(where + " 的服务列表里有空项");
            }
            checkName(service.name(), where + " 的服务标识");
            if (!seen.add(service.name())) {
                throw bad(where + " 里服务标识 " + service.name() + " 重复");
            }
            String what = where + " 的服务 " + service.name();
            checkText(service.displayName(), what + " 的显示名");
            if (service.trigger() == null || !ServiceTriggers.ALL.contains(service.trigger())) {
                throw bad(what + " 的触发时机 " + service.trigger() + " 不支持");
            }
            boolean scopeOk = service.scope() == null
                    || ServiceTriggers.SCOPE_RECORD.equals(service.scope())
                    || ServiceTriggers.SCOPE_MODEL.equals(service.scope());
            if (!scopeOk) {
                throw bad(what + " 的范围只能是 record 或 model");
            }
            checkText(service.script(), what + " 的脚本");
        }
    }

    // ---------- 视图 ----------

    private static void validateViews(ModelDef model) {
        ViewsDef views = model.views();
        if (views == null) {
            return;
        }
        String where = "模型 " + model.name();

        ListViewDef list = views.list();
        if (list != null) {
            for (String column : orEmpty(list.columns())) {
                requireStoredField(model, column, where + " 的列表列", true);
            }
            for (String search : orEmpty(list.searchFields())) {
                requireStoredField(model, search, where + " 的搜索字段", false);
            }
            if (list.defaultSort() != null) {
                requireStoredField(model, list.defaultSort().field(), where + " 的默认排序字段", true);
                String order = list.defaultSort().order();
                if (!"asc".equals(order) && !"desc".equals(order)) {
                    throw bad(where + " 的默认排序方向只能是 asc 或 desc");
                }
            }
            checkRange(list.pageSize(), 1, 200, where + " 的每页条数");
        }

        if (views.form() != null) {
            for (FormGroupDef group : orEmpty(views.form().groups())) {
                if (group == null) {
                    throw bad(where + " 的表单分组里有空项");
                }
                checkText(group.title(), where + " 的表单分组标题");
                checkRange(group.columns(), 1, 4, where + " 的表单分组“" + group.title() + "”每行字段数");
                for (String name : orEmpty(group.fields())) {
                    requireStoredField(model, name, where + " 的表单分组“" + group.title() + "”", false);
                }
            }
            for (FormTabDef tab : orEmpty(views.form().tabs())) {
                FieldDef field = tab == null ? null : model.findField(tab.field());
                if (field == null || !FieldTypes.ONE_TO_MANY.equals(field.type())) {
                    throw bad(where + " 的子表标签页必须对应一个一对多字段，"
                            + (tab == null ? "空项" : tab.field()) + " 不是");
                }
                checkText(tab.title(), where + " 的子表标签页标题");
            }
        }
    }

    /** 视图里引用的字段必须存在，而且要占数据库列（一对多不行）。allowSystem 表示能不能用系统字段。 */
    private static void requireStoredField(ModelDef model, String name, String what, boolean allowSystem) {
        if (allowSystem && SystemFields.NAMES.contains(name)) {
            return;
        }
        FieldDef field = model.findField(name);
        if (field == null) {
            throw bad(what + "里的 " + name + " 不是这个模型的字段");
        }
        if (!FieldTypes.hasColumn(field.type())) {
            throw bad(what + "里不能放一对多字段 " + name);
        }
    }

    // ---------- 小工具 ----------

    private static void checkName(String name, String what) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw bad(what + "“" + name + "”不合规则：" + NAME_RULE);
        }
    }

    private static void checkText(String text, String what) {
        if (isBlank(text)) {
            throw bad(what + "不能为空");
        }
    }

    /** 值可以不填；填了就必须在范围内。 */
    private static void checkRange(Integer value, int min, int max, String what) {
        if (value != null && (value < min || value > max)) {
            throw bad(what + "要在 " + min + " 到 " + max + " 之间");
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static BizException bad(String message) {
        return BizException.badRequest(message);
    }
}
