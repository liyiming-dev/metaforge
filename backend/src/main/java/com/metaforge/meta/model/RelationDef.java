package com.metaforge.meta.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 关联的配置。
 * manyToOne 用 model 和 displayField：指向哪个模型，用它的哪个字段当显示文字。
 * oneToMany 用 model 和 mappedBy：子表是哪个模型，子表里哪个 manyToOne 字段指回本模型。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelationDef(String model, String displayField, String mappedBy) {
}
