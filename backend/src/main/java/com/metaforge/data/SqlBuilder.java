package com.metaforge.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.SortItem;
import com.metaforge.meta.model.SystemFields;

/**
 * 拼数据接口要执行的 SQL。这个类只生成字符串和参数表，不执行，所以可以直接写单元测试。
 *
 * 表名和列名是拼进 SQL 的（SQL 不支持把表名当参数），所以这里是整套接口里最需要小心的地方。
 * 防注入靠四道规则，见决策记录 004：
 * 1. 表名、列名只从模型定义里取；请求里的字段名只用来在定义里查找，查不到就 400；
 * 2. 值一律走命名参数，绝不拼进字符串；
 * 3. 比较方式、排序方向用 switch 映射成固定的几种写法，不认识的一律 400；
 * 4. 拼进 SQL 之前再用标识规则查一遍并加反引号，即使前面漏了校验，也不会有奇怪字符进去。
 *
 * 第 4 条的规则和 DdlBuilder 里的那条是同一个，两个类各留一份：
 * 它们属于“建表”和“读写数据”两个不同的环节，共用的话要么把 data 包的方法搬到 schema 包，
 * 要么反过来，都不如各留十行清楚。
 */
@Component
public class SqlBuilder {

    /** 和 MetaDefaults.DEFAULT_PAGE_SIZE 保持一致 */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /** 一页最多取这么多条，防止一次把整张表拉下来 */
    public static final int MAX_PAGE_SIZE = 200;
    /** 下拉框默认取这么多项，和契约里的 options 一致 */
    public static final int DEFAULT_OPTION_LIMIT = 20;
    /** 下拉框一次最多取这么多项，防止前端传一个很大的 limit */
    public static final int MAX_OPTION_LIMIT = 100;

    /** 能拼进 SQL 的名字只能是这个样子 */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]*$");

    /** 支持的比较方式，对应契约里 op 那一列 */
    private static final Set<String> OPS = Set.of("eq", "ne", "like", "gt", "ge", "lt", "le", "in");

    /** 比较方式 → SQL 里的符号。like 和 in 要特殊处理，不在这张表里 */
    private static final Map<String, String> OP_SQL = Map.of(
            "eq", "=",
            "ne", "<>",
            "gt", ">",
            "ge", ">=",
            "lt", "<",
            "le", "<=");

    private final FieldValues values;

    public SqlBuilder(FieldValues values) {
        this.values = values;
    }

    /** 一次查询要发两条 SQL（取一页、取总数）和它们的参数，以及实际用的页码和每页条数。 */
    public record SearchSql(String select, String count, MapSqlParameterSource params, int page, int size) {
    }

    /** options 查询的 SQL 和参数。 */
    public record OptionsSql(String select, MapSqlParameterSource params) {
    }

    // ---------- 查询 ----------

    public SearchSql search(ModelDef model, SearchRequest request) {
        int page = request == null || request.page() == null ? 1 : request.page();
        int size = request == null || request.size() == null ? defaultPageSize(model) : request.size();
        if (page < 1) {
            throw BizException.badRequest("page 从 1 开始");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw BizException.badRequest("size 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }

        Bind bind = new Bind();
        List<String> conditions = new ArrayList<>();
        for (SearchRequest.Filter filter : filtersOf(request)) {
            if (filter != null) {
                conditions.add(condition(model, filter, bind));
            }
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String table = quote(model.tableName());
        bind.params.addValue("limit", size);
        bind.params.addValue("offset", (long) (page - 1) * size);

        String select = "SELECT " + quoteAll(allColumns(model)) + " FROM " + table + where
                + orderBy(model, request) + " LIMIT :limit OFFSET :offset";
        String count = "SELECT COUNT(*) FROM " + table + where;
        return new SearchSql(select, count, bind.params, page, size);
    }

    public String selectById(ModelDef model) {
        return "SELECT " + quoteAll(allColumns(model)) + " FROM " + quote(model.tableName())
                + " WHERE " + quote(SystemFields.ID) + " = :" + SystemFields.ID;
    }

    // ---------- 关联和多对一下拉框 ----------

    /**
     * 多对一显示字段的解析和校验。先看请求里指定的 displayField，再退回目标模型第一个 string 字段。
     * displayField 必须是目标模型里占列的字段（B1 保存模型时也按这个规则校验）；
     * 没有指定时按契约用第一个 string 字段。
     */
    public FieldDef labelField(ModelDef model, String displayField) {
        if (displayField != null && !displayField.isBlank()) {
            FieldDef field = model.findField(displayField);
            if (field == null || !FieldTypes.hasColumn(field.type())) {
                throw BizException.badRequest("显示字段 " + displayField + " 不是模型 " + model.name() + " 的字段");
            }
            return field;
        }
        for (FieldDef field : model.fields()) {
            if (field != null && FieldTypes.STRING.equals(field.type())) {
                return field;
            }
        }
        throw BizException.badRequest("模型 " + model.name() + " 没有 string 类型字段，不能作为显示字段");
    }

    /** 下拉框查询：按显示字段模糊匹配，最多取 limit 条。keyword 里的 % 和 _ 按普通字符处理。 */
    public OptionsSql options(ModelDef model, FieldDef labelField, String keyword, int limit) {
        Bind bind = new Bind();
        String column = quote(labelField.name());
        String where = "";
        if (keyword != null && !keyword.isBlank()) {
            where = " WHERE " + column + " LIKE " + bind.add("%" + escapeLike(keyword.trim()) + "%");
        }
        bind.params.addValue("limit", limit);
        String select = "SELECT " + quote(SystemFields.ID) + ", " + column + " AS `label`"
                + " FROM " + quote(model.tableName()) + where
                + " ORDER BY " + column + " ASC LIMIT :limit";
        return new OptionsSql(select, bind.params);
    }

    /** 一页记录里同一个关联字段的 id 收集好后，用一条 IN 查询取回 id 和 label。 */
    public String labelSelect(ModelDef model, FieldDef labelField) {
        return "SELECT " + quote(SystemFields.ID) + ", " + quote(labelField.name()) + " AS `label`"
                + " FROM " + quote(model.tableName())
                + " WHERE " + quote(SystemFields.ID) + " IN (:ids)";
    }

    /** 多对一写入前检查目标记录是否存在。 */
    public String existsById(ModelDef model) {
        return "SELECT COUNT(*) FROM " + quote(model.tableName())
                + " WHERE " + quote(SystemFields.ID) + " = :" + SystemFields.ID;
    }

    /** 删除记录前，检查某个子表里有多少条记录通过 field 指向它。 */
    public String countReferences(ModelDef child, FieldDef field) {
        return "SELECT COUNT(*) FROM " + quote(child.tableName())
                + " WHERE " + quote(field.name()) + " = :" + SystemFields.ID;
    }

    // ---------- 写入 ----------

    public String insert(ModelDef model) {
        List<String> columns = insertColumns(model);
        List<String> placeholders = new ArrayList<>();
        for (String column : columns) {
            placeholders.add(":" + column);
        }
        return "INSERT INTO " + quote(model.tableName())
                + " (" + quoteAll(columns) + ") VALUES (" + String.join(", ", placeholders) + ")";
    }

    public String update(ModelDef model) {
        List<String> sets = new ArrayList<>();
        for (String column : updateColumns(model)) {
            sets.add(quote(column) + " = :" + column);
        }
        return "UPDATE " + quote(model.tableName()) + " SET " + String.join(", ", sets)
                + " WHERE " + quote(SystemFields.ID) + " = :" + SystemFields.ID;
    }

    /** 脚本的 db.increment：只允许对整数业务字段做原子加 1。 */
    public String increment(ModelDef model, FieldDef field) {
        return "UPDATE " + quote(model.tableName())
                + " SET " + quote(field.name()) + " = " + quote(field.name()) + " + 1"
                + " WHERE " + quote(SystemFields.ID) + " = :" + SystemFields.ID;
    }

    public String deleteById(ModelDef model) {
        return "DELETE FROM " + quote(model.tableName())
                + " WHERE " + quote(SystemFields.ID) + " = :" + SystemFields.ID;
    }

    // ---------- 列 ----------

    /** 读记录时的列：id、每个占列的字段、创建时间、最后修改时间。 */
    public List<String> allColumns(ModelDef model) {
        List<String> columns = new ArrayList<>();
        columns.add(SystemFields.ID);
        columns.addAll(fieldColumns(model));
        columns.add(SystemFields.CREATED_AT);
        columns.add(SystemFields.UPDATED_AT);
        return columns;
    }

    /** 新建时的列：不带 id，主键由数据库生成。 */
    public List<String> insertColumns(ModelDef model) {
        List<String> columns = new ArrayList<>(fieldColumns(model));
        columns.add(SystemFields.CREATED_AT);
        columns.add(SystemFields.UPDATED_AT);
        return columns;
    }

    /** 修改时的列：创建时间不允许改。 */
    public List<String> updateColumns(ModelDef model) {
        List<String> columns = new ArrayList<>(fieldColumns(model));
        columns.add(SystemFields.UPDATED_AT);
        return columns;
    }

    /** 占数据库列的业务字段，顺序和定义里一致。一对多不在其中。 */
    public List<String> fieldColumns(ModelDef model) {
        List<String> columns = new ArrayList<>();
        for (FieldDef field : model.fields()) {
            if (field != null && FieldTypes.hasColumn(field.type())) {
                columns.add(field.name());
            }
        }
        return columns;
    }

    // ---------- 过滤和排序 ----------

    private String condition(ModelDef model, SearchRequest.Filter filter, Bind bind) {
        FieldDef field = filterField(model, filter.field());
        String column = quote(field.name());
        String op = filter.op() == null ? "" : filter.op();
        if (!OPS.contains(op)) {
            throw BizException.badRequest("不支持的比较方式：" + (filter.op() == null ? "（没填）" : filter.op()));
        }

        if ("in".equals(op)) {
            if (!(filter.value() instanceof List<?> items) || items.isEmpty()) {
                throw BizException.badRequest("字段 " + field.name() + " 的 in 条件要传一个非空数组");
            }
            List<String> placeholders = new ArrayList<>();
            for (Object item : items) {
                placeholders.add(bind.add(convert(field, item)));
            }
            return column + " IN (" + String.join(", ", placeholders) + ")";
        }

        if ("like".equals(op)) {
            Object value = convert(field, filter.value());
            if (!(value instanceof String text)) {
                throw BizException.badRequest("like 只用于文本字段");
            }
            // 契约里 like 的含义是“包含”，所以两边补上 %；值里的 % 和 _ 先转义成普通字符
            return column + " LIKE " + bind.add("%" + escapeLike(text) + "%");
        }

        // 没给值：等于、不等于可以理解成“是空值”“不是空值”，其它比较方式没有意义
        if (filter.value() == null) {
            return switch (op) {
                case "eq" -> column + " IS NULL";
                case "ne" -> column + " IS NOT NULL";
                default -> throw BizException.badRequest("比较方式 " + op + " 需要一个值");
            };
        }

        return column + " " + OP_SQL.get(op) + " " + bind.add(convert(field, filter.value()));
    }

    /**
     * 请求里的字段名，只在模型定义里查找。查不到、或者是个不占列的一对多字段，都返回 400。
     * 系统字段（id、created_at、updated_at）也可以用来过滤和排序。
     */
    private FieldDef filterField(ModelDef model, String name) {
        if (name == null || name.isBlank()) {
            throw BizException.badRequest("过滤条件和排序条件都要写字段名");
        }
        FieldDef system = FieldValues.systemField(name);
        if (system != null) {
            return system;
        }
        FieldDef field = model.findField(name);
        if (field == null) {
            throw BizException.badRequest("字段 " + name + " 不是模型 " + model.name() + " 的字段");
        }
        if (!FieldTypes.hasColumn(field.type())) {
            throw BizException.badRequest("字段 " + name + " 是子表，不能用来过滤或排序");
        }
        return field;
    }

    /** 没传排序时用模型视图里的默认排序。方向只认 asc、desc。 */
    private String orderBy(ModelDef model, SearchRequest request) {
        List<SortItem> items = sortOf(model, request);
        List<String> parts = new ArrayList<>();
        for (SortItem item : items) {
            if (item == null || item.field() == null) {
                continue;
            }
            FieldDef field = filterField(model, item.field());
            String direction = switch (item.order() == null ? "asc" : item.order().toLowerCase(Locale.ROOT)) {
                case "asc" -> "ASC";
                case "desc" -> "DESC";
                default -> throw BizException.badRequest("排序方向只能是 asc 或 desc");
            };
            parts.add(quote(field.name()) + " " + direction);
        }
        return parts.isEmpty() ? "" : " ORDER BY " + String.join(", ", parts);
    }

    /** 请求里没写排序就用模型的默认排序，两处都没有就不排。 */
    private List<SortItem> sortOf(ModelDef model, SearchRequest request) {
        List<SortItem> sort = request == null ? null : request.sort();
        if (sort != null && !sort.isEmpty()) {
            return sort;
        }
        if (model.views() != null && model.views().list() != null && model.views().list().defaultSort() != null) {
            return List.of(model.views().list().defaultSort());
        }
        return List.of();
    }

    private static List<SearchRequest.Filter> filtersOf(SearchRequest request) {
        List<SearchRequest.Filter> filters = request == null ? null : request.filters();
        return filters == null ? List.of() : filters;
    }

    private static int defaultPageSize(ModelDef model) {
        if (model.views() != null && model.views().list() != null && model.views().list().pageSize() != null) {
            return model.views().list().pageSize();
        }
        return DEFAULT_PAGE_SIZE;
    }

    // ---------- 值和名字 ----------

    private Object convert(FieldDef field, Object value) {
        try {
            return values.toColumn(field, value);
        } catch (BizException e) {
            throw BizException.badRequest("字段「" + field.name() + "」" + e.getMessage());
        }
    }

    /**
     * 转义 like 的值里的 % 和 _，让它们按普通字符匹配。
     * 反斜杠是 MySQL 和 H2 默认的转义符，所以反斜杠本身也要先转义。
     */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 给名字加反引号。名字不符合安全格式就返回 400，绝不让它进 SQL。
     * 正常情况下这里不会触发：元模型在保存时已经校验过标识规则，这里只是第二道。
     */
    public static String quote(String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw BizException.badRequest("表名或列名不合法：" + identifier);
        }
        return "`" + identifier + "`";
    }

    private static String quoteAll(List<String> columns) {
        List<String> quoted = new ArrayList<>();
        for (String column : columns) {
            quoted.add(quote(column));
        }
        return String.join(", ", quoted);
    }

    /** 给参数起 p0、p1…… 这样的名字，一次查询里有几个条件都不冲突。 */
    private static final class Bind {

        private final MapSqlParameterSource params = new MapSqlParameterSource();
        private int next;

        /** 记下一个参数，返回在 SQL 里引用的写法，例如 :p0。 */
        String add(Object value) {
            String name = "p" + next++;
            params.addValue(name, value);
            return ":" + name;
        }
    }
}
