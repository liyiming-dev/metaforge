package com.metaforge.meta.model;

import java.util.List;

/** 平台支持的字段类型。名字和 docs/contract/meta-model.md 的字段类型表一致。 */
public final class FieldTypes {

    public static final String STRING = "string";
    public static final String TEXT = "text";
    public static final String INTEGER = "integer";
    public static final String DECIMAL = "decimal";
    public static final String BOOLEAN = "boolean";
    public static final String DATE = "date";
    public static final String DATETIME = "datetime";
    public static final String ENUM = "enum";
    public static final String MANY_TO_ONE = "manyToOne";
    public static final String ONE_TO_MANY = "oneToMany";

    public static final List<String> ALL = List.of(
            STRING, TEXT, INTEGER, DECIMAL, BOOLEAN, DATE, DATETIME, ENUM, MANY_TO_ONE, ONE_TO_MANY);

    private FieldTypes() {
    }

    public static boolean isRelation(String type) {
        return MANY_TO_ONE.equals(type) || ONE_TO_MANY.equals(type);
    }

    /** 这种类型的字段在数据库里占不占一列。一对多不占列，数据在子表里。 */
    public static boolean hasColumn(String type) {
        return !ONE_TO_MANY.equals(type);
    }
}
