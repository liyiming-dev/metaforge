package com.metaforge.meta.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 开发者定义的脚本服务。增删改查是内置的，不在这里定义。
 * trigger 的可选值见 ServiceTriggers；scope 只对 manual 有意义，取 record 或 model。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceDef(
        String name,
        String displayName,
        String trigger,
        String scope,
        String script,
        Boolean enabled) {
}
