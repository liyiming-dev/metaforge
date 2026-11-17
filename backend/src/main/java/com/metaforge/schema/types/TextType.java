package com.metaforge.schema.types;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 多行文本：TEXT。数据库里最多 65535 字节，程序里不另外限长。 */
public final class TextType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.TEXT;
    }

    @Override
    public String columnType(FieldDef field) {
        return "TEXT";
    }

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw BizException.badRequest("应为文本");
        }
        return text;
    }
}
