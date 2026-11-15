package com.metaforge.schema.types;

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
}
