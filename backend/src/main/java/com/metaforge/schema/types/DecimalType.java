package com.metaforge.schema.types;

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
        int precision = field.precision() == null ? MetaDefaults.DEFAULT_PRECISION : field.precision();
        int scale = field.scale() == null ? Math.min(DEFAULT_SCALE, precision) : field.scale();
        return "DECIMAL(" + precision + "," + scale + ")";
    }
}
