package com.metaforge.schema.types;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 是或否：BOOLEAN。在 MySQL 里它就是 TINYINT(1)，读出来可能是布尔值也可能是 0/1。 */
public final class BooleanType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.BOOLEAN;
    }

    @Override
    public String columnType(FieldDef field) {
        return "BOOLEAN";
    }

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            int numberValue = number.intValue();
            if (numberValue == 0 || numberValue == 1) {
                return numberValue == 1;
            }
        }
        throw BizException.badRequest("应为是或否");
    }

    @Override
    public Object toJson(FieldDef field, Object value) {
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value;
    }
}
