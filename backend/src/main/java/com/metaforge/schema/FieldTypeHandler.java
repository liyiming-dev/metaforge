package com.metaforge.schema;

import com.metaforge.meta.model.FieldDef;

/**
 * 一种字段类型的处理方式。每种类型一个实现类，放在 types 包里，由 FieldTypeRegistry 按类型名查找。
 *
 * 这是策略模式：别的代码只管“拿到这个字段的处理器，问它列类型是什么、这个值能不能存”，
 * 不用写 if (type 是 string) ... else if (type 是 decimal) ... 这样的判断。
 * 以后新增一种字段类型，只要加一个实现类，再到注册表里登记一行。
 *
 * 每个处理器管三件事：建表时的列类型（B2）、请求里的值怎么变成数据库里的值、
 * 数据库里的值怎么变回响应里的 JSON（后两件是 B3 加的）。
 */
public interface FieldTypeHandler {

    /** 这个处理器负责的字段类型名，取值见 FieldTypes。 */
    String type();

    /**
     * 这种字段在建表语句里的列类型，例如 VARCHAR(100)。
     * 不占数据库列的类型（一对多）返回 null。
     */
    String columnType(FieldDef field);

    /**
     * 请求里的 JSON 值 → 写进数据库的值。也用于过滤条件的值。
     *
     * 每种类型自己决定接受什么形状：日期接受 "2026-11-14" 这样的字符串，
     * 多对一接受 id 或者 { "id": 3, "label": "..." }。值不合法时抛 BizException.badRequest，
     * 提示会由 DataService 收集到 fieldErrors 里，显示在对应输入框下面。
     */
    default Object toColumn(FieldDef field, Object value) {
        return value;
    }

    /** 数据库里的值 → 响应里的 JSON 值。默认原样返回，日期、小数等在各自的实现里覆盖。 */
    default Object toJson(FieldDef field, Object value) {
        return value;
    }
}
