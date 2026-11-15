package com.metaforge.meta.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 菜单树上的一个节点。一级节点是模块，有 children；
 * 二级节点是模型，有 model 和 path，前端按它们动态注册路由。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuNode(
        String key,
        String displayName,
        String icon,
        String model,
        String path,
        List<MenuNode> children) {
}
