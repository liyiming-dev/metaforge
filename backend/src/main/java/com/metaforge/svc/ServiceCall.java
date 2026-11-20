package com.metaforge.svc;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** POST /api/svc/{model}/{service} 的请求体。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceCall(Long id, Map<String, Object> params) {
}
