package com.metaforge.schema.types;

import java.math.BigDecimal;
import java.util.Map;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/**
 * 多对一：存对方记录的 id，用 BIGINT。
 * 没有建数据库外键，对方记录是否存在、能不能删，由任务 B4 在程序里检查。
 */
public final class ManyToOneType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.MANY_TO_ONE;
    }

    @Override
    public String columnType(FieldDef field) {
        return "BIGINT";
    }

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        // 契约允许两种写法：直接传 id，或者把读到的 { "id": 3, "label": "..." } 原样传回来
        if (value instanceof Map<?, ?> map) {
            return toColumn(field, map.get("id"));
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                throw BizException.badRequest("应为关联记录的 id");
            }
        }
        throw BizException.badRequest("应为关联记录的 id");
    }

    @Override
    public Object toJson(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.longValue();
        }
        // 这里只返回 id；DataService 会把一页里的 id 收集起来批量取 label，再换成 {id,label}
        return value;
    }
}
