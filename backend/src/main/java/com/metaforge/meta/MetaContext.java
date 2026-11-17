package com.metaforge.meta;

import java.util.Map;
import java.util.Set;

import com.metaforge.meta.model.ModelDef;

/**
 * 校验一个模型时能“看到”的其它定义：有哪些模块，有哪些模型。
 * 校验关联字段时要查对方模型是否存在、对方有没有某个字段，所以需要它。
 * 导入一个包时，包里的模型也要放进来，因为同一个包里的模型可以互相引用。
 *
 * @param moduleNames 所有模块的标识
 * @param models      所有模型，键是模型标识
 */
public record MetaContext(Set<String> moduleNames, Map<String, ModelDef> models) {
}
