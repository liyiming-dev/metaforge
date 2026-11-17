package com.metaforge.meta.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 列表视图：显示哪些列、搜索区有哪些字段、默认排序、每页条数。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListViewDef(
        List<String> columns,
        List<String> searchFields,
        SortItem defaultSort,
        Integer pageSize) {
}
