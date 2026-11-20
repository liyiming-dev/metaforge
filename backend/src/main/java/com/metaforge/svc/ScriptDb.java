package com.metaforge.svc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.metaforge.common.BizException;
import com.metaforge.data.DataService;

/**
 * 暴露给脚本的受控数据访问对象。
 *
 * 脚本只能通过这里读写，不直接拿 JdbcTemplate/SQL；每个方法内部仍然走 B3 的 DataService，
 * 所以字段白名单、必填、关联校验和普通接口一致。db.update/db.increment 不触发目标模型的钩子，
 * 避免脚本里的写操作递归触发同一个钩子（契约里有说明）。
 *
 * 这个类不是 Spring bean，由 ServiceRunner 每次执行时创建，因此可以安全地绑定当前调用链。
 */
public class ScriptDb {

    private final DataService data;
    private final ServiceRunner caller;

    ScriptDb(DataService data, ServiceRunner caller) {
        this.data = data;
        this.caller = caller;
    }

    public Map<String, Object> get(String model, Object id) {
        return data.getForScript(model, toId(id));
    }

    public Map<String, Object> find(String model, String field, Object value) {
        return data.findForScript(model, field, value);
    }

    public Map<String, Object> update(String model, Object id, Object values) {
        if (!(values instanceof Map<?, ?> raw)) {
            throw BizException.badRequest("db.update 的 values 必须是字段名到值的 map");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            body.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return data.updateForScript(model, toId(id), body);
    }

    public long increment(String model, Object id, String field) {
        return data.incrementForScript(model, toId(id), field);
    }

    public String today(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw BizException.badRequest("db.today 的日期格式不能为空");
        }
        try {
            return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw BizException.badRequest("db.today 的日期格式不合法：" + pattern);
        }
    }

    public Object call(String model, String service, Object id, Object params) {
        Map<String, Object> actualParams = Map.of();
        if (params instanceof Map<?, ?> raw) {
            actualParams = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                actualParams.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } else if (params != null) {
            throw BizException.badRequest("db.call 的 params 必须是 map");
        }
        return caller.runManualInternal(model, service, id == null ? null : toId(id), actualParams);
    }

    private static Long toId(Object id) {
        if (id == null) {
            throw BizException.badRequest("db 操作的记录 id 不能为空");
        }
        if (id instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(id).trim());
        } catch (NumberFormatException e) {
            throw BizException.badRequest("db 操作的记录 id 必须是数字");
        }
    }
}
