package com.metaforge.data;

import java.util.List;
import java.util.Map;

/**
 * 查询结果，对应 docs/contract/api.md 里 search 接口的响应：
 * records 是这一页的记录，total 是符合条件的总数（不是这一页的条数）。
 * page、size 回显实际用的值，前端可以据此画分页器。
 */
public record SearchResult(List<Map<String, Object>> records, long total, int page, int size) {
}
