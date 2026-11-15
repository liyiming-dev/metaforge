package com.metaforge.schema.types;

import com.metaforge.common.BizException;
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

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw BizException.badRequest("应为文本");
        }
        // 长度在进数据库之前查，不然 MySQL 会直接报一条看不懂的 SQL 错误
        int length = field.length() == null ? DEFAULT_LENGTH : field.length();
        if (text.length() > length) {
            throw BizException.badRequest("最多 " + length + " 个字");
        }
        return text;
    }
}
