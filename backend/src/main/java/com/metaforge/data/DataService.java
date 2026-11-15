package com.metaforge.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.metaforge.common.BizException;
import com.metaforge.common.ErrorCode;
import com.metaforge.meta.MetaRegistry;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.SystemFields;

/**
 * 数据接口的业务逻辑：/api/data/{model} 的查询、详情、新建、修改、删除，对任意模型都可用。
 * 模型定义每次从 MetaRegistry 里现取，所以新建一个模型之后不用重启，它的接口立刻能用。
 *
 * 新建、修改、删除都写成同一个固定流程（模板方法）：
 * 前置钩子 → 校验 → 写库 → 后置钩子 → 读回。
 * 钩子现在是空方法，任务 B5 会把脚本服务接进去；到时候只要往这几个方法里填执行脚本的代码，
 * 流程本身不用动，也不会出现“某个接口忘了调钩子”的情况。
 *
 * 写操作加事务：钩子、写库、读回要一起成功或一起失败。
 * 这里用 @Transactional 而不是 MetaService 里的 TransactionTemplate，因为那边只有中间一段在事务里
 * （改表语句不能回滚，必须排在事务外面），这边整个方法就是一个事务。
 */
@Service
public class DataService {

    private final MetaRegistry registry;
    private final SqlBuilder sql;
    private final FieldValues values;
    private final NamedParameterJdbcTemplate jdbc;

    public DataService(MetaRegistry registry, SqlBuilder sql, FieldValues values, NamedParameterJdbcTemplate jdbc) {
        this.registry = registry;
        this.sql = sql;
        this.values = values;
        this.jdbc = jdbc;
    }

    // ---------- 查询 ----------

    public SearchResult search(String modelName, SearchRequest request) {
        ModelDef model = registry.requireModel(modelName);
        SqlBuilder.SearchSql query = sql.search(model, request);
        Long total = jdbc.queryForObject(query.count(), query.params(), Long.class);
        List<Map<String, Object>> rows = jdbc.queryForList(query.select(), query.params());
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            records.add(toRecord(model, row));
        }
        return new SearchResult(records, total == null ? 0 : total, query.page(), query.size());
    }

    public Map<String, Object> get(String modelName, String id) {
        ModelDef model = registry.requireModel(modelName);
        return toRecord(model, readRow(model, parseId(id)));
    }

    // ---------- 新建 ----------

    @Transactional
    public Map<String, Object> create(String modelName, Map<String, Object> body) {
        ModelDef model = registry.requireModel(modelName);

        Map<String, Object> record = fromBody(model, body, null);
        beforeCreate(model, record);
        requireRequired(model, record);

        LocalDateTime now = LocalDateTime.now();
        MapSqlParameterSource params = toParams(model, record);
        params.addValue(SystemFields.CREATED_AT, now);
        params.addValue(SystemFields.UPDATED_AT, now);

        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(sql.insert(model), params, keys, new String[] { SystemFields.ID });
        } catch (DuplicateKeyException e) {
            throw duplicateKey(model, e);
        }
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("新建记录后没有拿到主键，模型 " + model.name());
        }
        long id = key.longValue();

        afterCreate(model, record, id);
        return readBack(model, id);
    }

    // ---------- 修改 ----------

    @Transactional
    public Map<String, Object> update(String modelName, String id, Map<String, Object> body) {
        ModelDef model = registry.requireModel(modelName);
        long recordId = parseId(id);
        Map<String, Object> existing = readRow(model, recordId);

        Map<String, Object> record = fromBody(model, body, existing);
        beforeUpdate(model, record);
        requireRequired(model, record);

        MapSqlParameterSource params = toParams(model, record);
        params.addValue(SystemFields.ID, recordId);
        params.addValue(SystemFields.UPDATED_AT, LocalDateTime.now());
        try {
            jdbc.update(sql.update(model), params);
        } catch (DuplicateKeyException e) {
            throw duplicateKey(model, e);
        }

        afterUpdate(model, record, recordId);
        return readBack(model, recordId);
    }

    // ---------- 删除 ----------

    @Transactional
    public void delete(String modelName, String id) {
        ModelDef model = registry.requireModel(modelName);
        long recordId = parseId(id);
        Map<String, Object> record = readRow(model, recordId);

        beforeDelete(model, record);
        int deleted = jdbc.update(sql.deleteById(model), new MapSqlParameterSource(SystemFields.ID, recordId));
        if (deleted == 0) {
            throw BizException.notFound("记录不存在");
        }
    }

    // ---------- 校验 ----------

    /**
     * 把请求体变成“字段名 → 数据库里的值”的表。
     *
     * 规则：没定义的字段直接拒绝（这是契约规定的 400）；系统字段由服务端写，传了就忽略；
     * 新建时没传的字段取定义里的默认值；修改时没传的字段保持原值（existing 传旧记录）。
     * 值的类型、长度、枚举选项由各字段类型的处理器检查，错误收集成 fieldErrors 一起返回。
     */
    private Map<String, Object> fromBody(ModelDef model, Map<String, Object> body, Map<String, Object> existing) {
        Map<String, Object> input = body == null ? Map.of() : body;
        Map<String, Object> record = new LinkedHashMap<>();
        for (FieldDef field : model.fields()) {
            if (hasColumn(field)) {
                record.put(field.name(), existing == null ? null : existing.get(field.name()));
            }
        }

        for (String key : input.keySet()) {
            if (SystemFields.NAMES.contains(key)) {
                continue;
            }
            FieldDef field = model.findField(key);
            if (field == null) {
                throw BizException.badRequest("字段 " + key + " 不是模型 " + model.name() + " 的字段");
            }
            if (!hasColumn(field)) {
                throw BizException.badRequest("字段 " + key + " 是子表，不能直接写在记录里");
            }
        }

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldDef field : model.fields()) {
            if (!hasColumn(field)) {
                continue;
            }
            if (input.containsKey(field.name())) {
                try {
                    record.put(field.name(), values.toColumn(field, input.get(field.name())));
                } catch (BizException e) {
                    errors.put(field.name(), e.getMessage());
                } catch (RuntimeException e) {
                    errors.put(field.name(), "值不合法");
                }
            } else if (existing == null) {
                record.put(field.name(), defaultValue(field));
            }
        }
        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "有字段填写不正确", errors);
        }
        return record;
    }

    private Object defaultValue(FieldDef field) {
        if (field.defaultValue() == null) {
            return null;
        }
        try {
            return values.toColumn(field, field.defaultValue());
        } catch (RuntimeException e) {
            // 定义里的默认值本身不合法：这是元模型的问题，不是用户填错了，所以不用 fieldErrors
            throw BizException.badRequest("字段 " + field.name() + " 的默认值不合法：" + e.getMessage());
        }
    }

    /** 必填检查放在钩子之后：钩子可能正好会填上必填字段（例如自动生成编码）。 */
    private void requireRequired(ModelDef model, Map<String, Object> record) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldDef field : model.fields()) {
            if (hasColumn(field) && Boolean.TRUE.equals(field.required()) && record.get(field.name()) == null) {
                errors.put(field.name(), "请填写" + (field.displayName() == null ? field.name() : field.displayName()));
            }
        }
        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "有必填字段没有填写", errors);
        }
    }

    // ---------- 数据库 ----------

    private MapSqlParameterSource toParams(ModelDef model, Map<String, Object> record) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (String column : sql.fieldColumns(model)) {
            params.addValue(column, record.get(column));
        }
        return params;
    }

    /** 读一条记录，读不到返回 404。返回的是数据库里的原始值，给修改时合并旧值用。 */
    private Map<String, Object> readRow(ModelDef model, long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                sql.selectById(model), new MapSqlParameterSource(SystemFields.ID, id));
        if (rows.isEmpty()) {
            throw BizException.notFound("记录不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> readBack(ModelDef model, long id) {
        return toRecord(model, readRow(model, id));
    }

    /** 数据库的一行 → 响应里的记录：按定义顺序排好，值换成契约规定的 JSON 形式。 */
    private Map<String, Object> toRecord(ModelDef model, Map<String, Object> row) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put(SystemFields.ID, row.get(SystemFields.ID));
        for (FieldDef field : model.fields()) {
            if (hasColumn(field)) {
                record.put(field.name(), values.toJson(field, row.get(field.name())));
            }
        }
        record.put(SystemFields.CREATED_AT,
                values.toJson(FieldValues.systemField(SystemFields.CREATED_AT), row.get(SystemFields.CREATED_AT)));
        record.put(SystemFields.UPDATED_AT,
                values.toJson(FieldValues.systemField(SystemFields.UPDATED_AT), row.get(SystemFields.UPDATED_AT)));
        return record;
    }

    /**
     * 违反唯一约束时，从数据库报的约束名反查出是哪个字段重复了，把提示挂到那个字段下面。
     * 约束名是 B2 建表时定的 uk_模型标识_字段标识，所以能反查。
     */
    private BizException duplicateKey(ModelDef model, DuplicateKeyException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
        for (FieldDef field : model.fields()) {
            if (!Boolean.TRUE.equals(field.unique())) {
                continue;
            }
            String constraint = "uk_" + model.name() + "_" + field.name();
            if (message.contains(constraint.toLowerCase(Locale.ROOT))) {
                return new BizException(ErrorCode.CONFLICT,
                        "「" + (field.displayName() == null ? field.name() : field.displayName()) + "」的值已经存在",
                        Map.of(field.name(), "这个值已经存在"));
            }
        }
        // 认不出是哪个约束（比如不是我们建的索引），也给 409，只是没有 fieldErrors
        return BizException.conflict("数据违反了唯一约束");
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw BizException.badRequest("记录 id 必须是数字");
        }
    }

    private static boolean hasColumn(FieldDef field) {
        return field != null && FieldTypes.hasColumn(field.type());
    }

    // ---------- 钩子（任务 B5 接脚本） ----------

    /** 新建之前。脚本可以修改将要保存的 record。 */
    private void beforeCreate(ModelDef model, Map<String, Object> record) {
    }

    /** 新建之后。记录已经在数据库里，参数是它的 id。 */
    private void afterCreate(ModelDef model, Map<String, Object> record, long id) {
    }

    /** 修改之前。 */
    private void beforeUpdate(ModelDef model, Map<String, Object> record) {
    }

    /** 修改之后。 */
    private void afterUpdate(ModelDef model, Map<String, Object> record, long id) {
    }

    /** 删除之前。脚本抛错就能阻止删除。 */
    private void beforeDelete(ModelDef model, Map<String, Object> record) {
    }
}
