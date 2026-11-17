package com.metaforge.schema.types;

import java.math.BigDecimal;

import com.metaforge.common.BizException;
import com.metaforge.meta.MetaDefaults;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 小数：DECIMAL(精度, 小数位数)。精度没写是 18；小数位数没写是 4，但不会超过精度。 */
public final class DecimalType implements FieldTypeHandler {

    public static final int DEFAULT_SCALE = 4;

    @Override
    public String type() {
        return FieldTypes.DECIMAL;
    }

    @Override
    public String columnType(FieldDef field) {
        int precision = precision(field);
        return "DECIMAL(" + precision + "," + scale(field, precision) + ")";
    }

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        BigDecimal number;
        if (value instanceof BigDecimal decimal) {
            number = decimal;
        } else if (value instanceof Number other) {
            // 不用 double 的二进制值，用它的十进制写法，避免 0.1 变成 0.1000000000000000055
            number = new BigDecimal(other.toString());
        } else if (value instanceof String text) {
            try {
                number = new BigDecimal(text.trim());
            } catch (NumberFormatException e) {
                throw BizException.badRequest("应为数字");
            }
        } else {
            throw BizException.badRequest("应为数字");
        }
        int scale = scale(field, precision(field));
        if (number.stripTrailingZeros().scale() > scale) {
            throw BizException.badRequest("最多 " + scale + " 位小数");
        }
        return number;
    }

    private static int precision(FieldDef field) {
        return field.precision() == null ? MetaDefaults.DEFAULT_PRECISION : field.precision();
    }

    private static int scale(FieldDef field, int precision) {
        return field.scale() == null ? Math.min(DEFAULT_SCALE, precision) : field.scale();
    }
}
