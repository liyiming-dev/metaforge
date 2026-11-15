package com.metaforge.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.metaforge.meta.model.SortItem;

/**
 * 查询请求，对应 docs/contract/api.md 里 POST /api/data/{model}/search 的请求体。
 * 每一项都可以不传。
 *
 * op 的取值和含义见契约里的表：eq、ne、like、gt、ge、lt、le、in。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchRequest(
        Integer page,
        Integer size,
        List<Filter> filters,
        List<SortItem> sort) {

    /**
     * 一个过滤条件。field 是字段标识，op 是比较方式，value 是比较的值；
     * op 为 in 时 value 是数组。对多对一字段过滤时，value 传目标记录的 id。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Filter(String field, String op, Object value) {
    }
}
