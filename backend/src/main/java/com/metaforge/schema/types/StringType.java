package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 单行文本：VARCHAR，长度取字段的 length，没写就是 255。 */
public final class StringType implements FieldTypeHandler {

    public static final int DEFAULT_LENGTH = 255;

    @Override
    public String type() {
        return FieldTypes.STRING;
    }

    @Override
    public String columnType(FieldDef field) {
        int length = field.length() == null ? DEFAULT_LENGTH : field.length();
        return "VARCHAR(" + length + ")";
    }
}
