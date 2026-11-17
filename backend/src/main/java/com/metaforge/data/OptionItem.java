package com.metaforge.data;

/**
 * 多对一下拉框的一项：value 是目标记录的 id，label 是显示文字。
 * 对应 docs/contract/api.md 里 GET /api/data/{model}/options 的响应。
 */
public record OptionItem(Long value, String label) {
}
