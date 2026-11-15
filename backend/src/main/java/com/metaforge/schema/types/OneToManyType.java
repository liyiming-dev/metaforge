package com.metaforge.schema.types;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.FieldTypeHandler;

/** 一对多：不占列。数据在子表里，靠子表的多对一字段指回来。 */
public final class OneToManyType implements FieldTypeHandler {

    @Override
    public String type() {
        return FieldTypes.ONE_TO_MANY;
    }

    @Override
    public String columnType(FieldDef field) {
        return null;
    }
}
