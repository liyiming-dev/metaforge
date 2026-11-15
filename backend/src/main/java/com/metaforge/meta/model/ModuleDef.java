package com.metaforge.meta.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 模块：给模型分组，对应左侧菜单的一级目录。键名和 docs/contract/meta-model.md 一致。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModuleDef(String name, String displayName, String icon, Integer sort) {
}
