package com.metaforge.meta.model;

import java.util.List;

/** 脚本服务的触发时机。名字和 docs/contract/meta-model.md 的“服务”一节一致。 */
public final class ServiceTriggers {

    public static final String MANUAL = "manual";
    public static final String BEFORE_CREATE = "beforeCreate";
    public static final String AFTER_CREATE = "afterCreate";
    public static final String BEFORE_UPDATE = "beforeUpdate";
    public static final String AFTER_UPDATE = "afterUpdate";
    public static final String BEFORE_DELETE = "beforeDelete";

    public static final List<String> ALL = List.of(
            MANUAL, BEFORE_CREATE, AFTER_CREATE, BEFORE_UPDATE, AFTER_UPDATE, BEFORE_DELETE);

    /** manual 服务的作用范围 */
    public static final String SCOPE_RECORD = "record";
    public static final String SCOPE_MODEL = "model";

    private ServiceTriggers() {
    }
}
