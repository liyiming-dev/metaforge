package com.metaforge.schema.types;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 日期时间：DATETIME。契约规定记录里的 JSON 值是 "2026-11-14 09:30:00"。 */
public final class DateTimeType implements FieldTypeHandler {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String type() {
        return FieldTypes.DATETIME;
    }

    @Override
    public String columnType(FieldDef field) {
        return "DATETIME";
    }

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            try {
                return LocalDateTime.parse(trimmed, FORMAT);
            } catch (DateTimeParseException ignored) {
                // 也接受 "2026-11-14T09:30:00" 这种标准写法
                try {
                    return LocalDateTime.parse(trimmed);
                } catch (DateTimeParseException e) {
                    throw BizException.badRequest("时间格式应为 2026-11-14 09:30:00");
                }
            }
        }
        throw BizException.badRequest("时间格式应为 2026-11-14 09:30:00");
    }

    @Override
    public Object toJson(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(FORMAT);
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(FORMAT);
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(FORMAT);
        }
        return value.toString();
    }
}
