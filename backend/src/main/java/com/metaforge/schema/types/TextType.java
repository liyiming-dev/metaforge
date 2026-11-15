package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 多行文本：TEXT。 */
public final class TextType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.TEXT;
    }

    @Override
    public String columnType(FieldDef field) {
        return "TEXT";
    }
}
