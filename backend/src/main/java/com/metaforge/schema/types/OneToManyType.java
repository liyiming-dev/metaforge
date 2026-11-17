package com.metaforge.schema.types;

import com.metaforge.common.BizException;
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

    @Override
    public Object toColumn(FieldDef field, Object value) {
        // 一对多的数据存在子表里，不能直接写在父表的记录上
        throw BizException.badRequest("一对多字段不能直接写值，请对子表调用接口");
    }
}
