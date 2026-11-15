package com.metaforge.schema.types;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 日期：DATE。契约规定记录里的 JSON 值是 "2026-11-14" 这样的字符串。 */
public final class DateType implements FieldTypeHandler {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String type() {
        return FieldTypes.DATE;
    }

    @Override
    public String columnType(FieldDef field) {
        return "DATE";
    }

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        // 存成 LocalDate：MySQL 和 H2 的驱动都认这种类型，不用自己拼字符串
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof String text) {
            try {
                return LocalDate.parse(text.trim(), FORMAT);
            } catch (DateTimeParseException e) {
                throw BizException.badRequest("日期格式应为 2026-11-14");
            }
        }
        throw BizException.badRequest("日期格式应为 2026-11-14");
    }

    @Override
    public Object toJson(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date.format(FORMAT);
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().format(FORMAT);
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(FORMAT);
        }
        return value.toString();
    }
}
