package com.metaforge.meta.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 字段定义。type 用字符串而不是枚举：这样遇到不认识的类型时，
 * 校验器能给出“类型 xxx 不支持”的明确提示，而不是一个 JSON 解析错误。
 * 可选的类型见 FieldTypes。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldDef(
        String name,
        String displayName,
        String type,
        Boolean required,
        Boolean unique,
        Boolean readonly,
        Object defaultValue,
        /** string 专用 */
        Integer length,
        /** decimal 专用 */
        Integer precision,
        /** decimal 专用 */
        Integer scale,
        /** enum 必填 */
        List<EnumOption> options,
        /** manyToOne、oneToMany 必填 */
        RelationDef relation) {
}
