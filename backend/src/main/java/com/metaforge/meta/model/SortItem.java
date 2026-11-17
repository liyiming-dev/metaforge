package com.metaforge.meta.model;

/** 排序：field 是字段标识，order 是 asc 或 desc。 */
public record SortItem(String field, String order) {
}
