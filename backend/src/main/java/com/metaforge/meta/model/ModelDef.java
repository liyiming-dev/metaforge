package com.metaforge.meta.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 模型定义：一个业务对象的完整描述。键名和 docs/contract/meta-model.md 一致。
 * 请求里 showInMenu、sort、services、views 可以不传，tableName 传了也会被忽略；
 * 经过 MetaDefaults 补齐之后这些值才一定存在。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelDef(
        String name,
        String displayName,
        String description,
        String module,
        Boolean showInMenu,
        Integer sort,
        List<FieldDef> fields,
        List<ServiceDef> services,
        ViewsDef views,
        String tableName) {

    /** 按标识找字段，找不到返回 null。 */
    public FieldDef findField(String fieldName) {
        if (fields == null || fieldName == null) {
            return null;
        }
        for (FieldDef field : fields) {
            if (field != null && fieldName.equals(field.name())) {
                return field;
            }
        }
        return null;
    }
}
