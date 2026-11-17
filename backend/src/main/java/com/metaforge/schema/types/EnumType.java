package com.metaforge.schema.types;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.EnumOption;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/**
 * 枚举：存选项的 value，用 VARCHAR(64)。
 * 没有用数据库自带的 ENUM 类型，因为那样每次增减选项都要改表结构；
 * 值是否在选项里，在写入之前由这里检查。
 */
public final class EnumType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.ENUM;
    }

    @Override
    public String columnType(FieldDef field) {
        return "VARCHAR(64)";
    }

    @Override
    public Object toColumn(FieldDef field, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw BizException.badRequest("应为选项里的一个值");
        }
        if (field.options() == null || field.options().isEmpty()) {
            throw BizException.badRequest("这个枚举字段没有定义选项");
        }
        for (EnumOption option : field.options()) {
            if (option != null && text.equals(option.value())) {
                return text;
            }
        }
        throw BizException.badRequest("取值不在选项里");
    }
}
