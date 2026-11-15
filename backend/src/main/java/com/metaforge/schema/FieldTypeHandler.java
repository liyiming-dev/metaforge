package com.metaforge.schema;

import com.metaforge.meta.model.FieldDef;

/**
 * 一种字段类型的处理方式。每种类型一个实现类，放在 types 包里，由 FieldTypeRegistry 按类型名查找。
 *
 * 这是策略模式：别的代码只管“拿到这个字段的处理器，问它列类型是什么”，
 * 不用写 if (type 是 string) ... else if (type 是 decimal) ... 这样的判断。
 * 以后新增一种字段类型，只要加一个实现类，再到注册表里登记一行。
 *
 * 目前只负责建表时的列类型。任务 B3 会在这里加上“请求里的值怎么转成数据库值、数据库值怎么转回去”。
 */
public interface FieldTypeHandler {

    /** 这个处理器负责的字段类型名，取值见 FieldTypes。 */
    String type();

    /**
     * 这种字段在建表语句里的列类型，例如 VARCHAR(100)。
     * 不占数据库列的类型（一对多）返回 null。
     */
    String columnType(FieldDef field);
}
