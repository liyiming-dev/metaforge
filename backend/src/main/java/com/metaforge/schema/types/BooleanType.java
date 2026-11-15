package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 是或否：BOOLEAN。在 MySQL 里它就是 TINYINT(1)。 */
public final class BooleanType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.BOOLEAN;
    }

    @Override
    public String columnType(FieldDef field) {
        return "BOOLEAN";
    }
}
