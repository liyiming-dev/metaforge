package com.metaforge.meta.model;

import java.util.List;

/** 表单里的一个分组：标题、每行放几个字段（1 到 4）、包含哪些字段。 */
public record FormGroupDef(String title, Integer columns, List<String> fields) {
}
