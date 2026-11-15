package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/**
 * 多对一：存对方记录的 id，用 BIGINT。
 * 没有建数据库外键，对方记录是否存在、能不能删，由任务 B4 在程序里检查。
 */
public final class ManyToOneType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.MANY_TO_ONE;
    }

    @Override
    public String columnType(FieldDef field) {
        return "BIGINT";
    }
}
