package com.metaforge.meta.model;

import java.util.List;

/** 元模型包：导入、导出和 AI 助手都用这个格式，一次带上若干模块和模型。 */
public record MetaBundle(List<ModuleDef> modules, List<ModelDef> models) {
}
