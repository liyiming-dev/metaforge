package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 日期：DATE。 */
public final class DateType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.DATE;
    }

    @Override
    public String columnType(FieldDef field) {
        return "DATE";
    }
}
