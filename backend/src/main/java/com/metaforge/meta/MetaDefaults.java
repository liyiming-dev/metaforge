package com.metaforge.meta;

import java.util.ArrayList;
import java.util.List;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.meta.model.FormGroupDef;
import com.metaforge.meta.model.FormTabDef;
import com.metaforge.meta.model.FormViewDef;
import com.metaforge.meta.model.ListViewDef;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;
import com.metaforge.meta.model.ServiceDef;
import com.metaforge.meta.model.ServiceTriggers;
import com.metaforge.meta.model.SortItem;
import com.metaforge.meta.model.SystemFields;
import com.metaforge.meta.model.ViewsDef;

/**
 * 补齐默认值。请求里很多键可以不传，保存之前在这里统一补上，
 * 这样后面的代码和前端都不用再处理“没传”的情况。
 *
 * 默认视图的规则写在 docs/contract/meta-model.md 的“默认视图”一节，这里要和它保持一致。
 * 调用顺序：先 MetaValidator 校验，再到这里补默认值。
 */
public final class MetaDefaults {

    public static final int DEFAULT_SORT = 100;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PRECISION = 18;
    /** 业务表的统一前缀，避免和元模型自己的表、SQL 关键字重名 */
    public static final String TABLE_PREFIX = "md_";

    private MetaDefaults() {
    }

    public static ModuleDef normalize(ModuleDef module) {
        return new ModuleDef(
                module.name(),
                module.displayName(),
                module.icon(),
                module.sort() == null ? DEFAULT_SORT : module.sort());
    }

    public static ModelDef normalize(ModelDef model) {
        List<ServiceDef> services = new ArrayList<>();
        if (model.services() != null) {
            for (ServiceDef service : model.services()) {
                services.add(normalize(service));
            }
        }
        return new ModelDef(
                model.name(),
                model.displayName(),
                model.description(),
                model.module(),
                model.showInMenu() == null ? Boolean.TRUE : model.showInMenu(),
                model.sort() == null ? DEFAULT_SORT : model.sort(),
                List.copyOf(model.fields()),
                List.copyOf(services),
                normalizeViews(model.views(), model.fields()),
                TABLE_PREFIX + model.name());
    }

    private static ServiceDef normalize(ServiceDef service) {
        boolean manual = ServiceTriggers.MANUAL.equals(service.trigger());
        String scope = manual
                ? (service.scope() == null ? ServiceTriggers.SCOPE_RECORD : service.scope())
                : null;
        return new ServiceDef(
                service.name(),
                service.displayName(),
                service.trigger(),
                scope,
                service.script(),
                service.enabled() == null ? Boolean.TRUE : service.enabled());
    }

    /** 没传视图就整个用默认的；传了一部分，缺的那部分用默认的。 */
    private static ViewsDef normalizeViews(ViewsDef views, List<FieldDef> fields) {
        ViewsDef defaults = defaultViews(fields);
        if (views == null) {
            return defaults;
        }

        ListViewDef list = views.list();
        if (list == null) {
            list = defaults.list();
        } else {
            list = new ListViewDef(
                    list.columns() == null ? defaults.list().columns() : List.copyOf(list.columns()),
                    list.searchFields() == null ? List.of() : List.copyOf(list.searchFields()),
                    list.defaultSort() == null ? defaults.list().defaultSort() : list.defaultSort(),
                    list.pageSize() == null ? DEFAULT_PAGE_SIZE : list.pageSize());
        }

        FormViewDef form = views.form();
        if (form == null) {
            form = defaults.form();
        } else {
            form = new FormViewDef(
                    form.groups() == null ? defaults.form().groups() : List.copyOf(form.groups()),
                    form.tabs() == null ? List.of() : List.copyOf(form.tabs()));
        }
        return new ViewsDef(list, form);
    }

    /** 按字段生成默认视图。 */
    static ViewsDef defaultViews(List<FieldDef> fields) {
        List<String> columns = new ArrayList<>();
        List<String> searchFields = new ArrayList<>();
        List<String> formFields = new ArrayList<>();
        List<FormTabDef> tabs = new ArrayList<>();

        for (FieldDef field : fields) {
            String type = field.type();
            if (FieldTypes.ONE_TO_MANY.equals(type)) {
                // 一对多不占列，在表单里显示成子表标签页
                tabs.add(new FormTabDef(field.displayName(), field.name()));
                continue;
            }
            formFields.add(field.name());
            if (!FieldTypes.TEXT.equals(type)) {
                columns.add(field.name());
            }
            boolean searchable = FieldTypes.STRING.equals(type) || FieldTypes.ENUM.equals(type);
            if (searchable && searchFields.size() < 3) {
                searchFields.add(field.name());
            }
        }

        ListViewDef list = new ListViewDef(
                List.copyOf(columns),
                List.copyOf(searchFields),
                new SortItem(SystemFields.CREATED_AT, "desc"),
                DEFAULT_PAGE_SIZE);
        FormViewDef form = new FormViewDef(
                List.of(new FormGroupDef("基本信息", 2, List.copyOf(formFields))),
                List.copyOf(tabs));
        return new ViewsDef(list, form);
    }
}
