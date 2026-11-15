package com.metaforge.schema.types;

import java.math.BigDecimal;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 整数：BIGINT。 */
public final class IntegerType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.INTEGER;
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
        if (value instanceof Long || value instanceof Integer || value instanceof Short) {
            return ((Number) value).longValue();
        }
        // JSON 里的数字可能被解析成 BigDecimal 或 Double，要看它是不是整数
        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).longValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                throw BizException.badRequest("应为整数");
            }
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                throw BizException.badRequest("应为整数");
            }
        }
        throw BizException.badRequest("应为整数");
    }
}
