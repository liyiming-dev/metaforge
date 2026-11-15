package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/**
 * 枚举：存选项的 value，用 VARCHAR(64)。
 * 没有用数据库自带的 ENUM 类型，因为那样每次增减选项都要改表结构；
 * 值是否在选项里，由任务 B3 在写入前校验。
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
}
