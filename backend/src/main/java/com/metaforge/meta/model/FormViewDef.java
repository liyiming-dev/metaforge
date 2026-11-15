package com.metaforge.meta.model;

import java.util.List;

/** 表单视图：若干分组，加上若干子表标签页。 */
public record FormViewDef(List<FormGroupDef> groups, List<FormTabDef> tabs) {
}
