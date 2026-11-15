package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 日期时间：DATETIME。 */
public final class DateTimeType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.DATETIME;
    }

    @Override
    public String columnType(FieldDef field) {
        return "DATETIME";
    }
}
