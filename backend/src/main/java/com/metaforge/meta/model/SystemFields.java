package com.metaforge.meta.model;

import java.util.List;

/** 每个模型自动带的三个系统字段，不用也不能在 fields 里定义。 */
public final class SystemFields {

    public static final String ID = "id";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";

    public static final List<String> NAMES = List.of(ID, CREATED_AT, UPDATED_AT);

    private SystemFields() {
    }
}
