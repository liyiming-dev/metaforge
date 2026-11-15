package com.metaforge.meta;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.metaforge.meta.model.MetaBundle;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 测试用的帮手：读入契约里的 MES 示例（docs/contract/samples/mes.json），
 * 并提供“复制一个模型、改动其中一处”的方法，用来构造各种不合规则的定义。
 * 这样测试用的数据和契约示例是同一份，示例写错了测试会先发现。
 */
final class SampleMeta {

    static final JsonMapper JSON = JsonMapper.builder().build();

    private static final MetaBundle BUNDLE = JSON.readValue(
            Path.of("..", "docs", "contract", "samples", "mes.json").toFile(), MetaBundle.class);

    private SampleMeta() {
    }

    static MetaBundle bundle() {
        return BUNDLE;
    }

    static ModelDef model(String name) {
        return BUNDLE.models().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    /** 示例里的全部模块和模型，作为校验时的上下文。 */
    static MetaContext context() {
        Set<String> modules = BUNDLE.modules().stream().map(ModuleDef::name).collect(Collectors.toSet());
        Map<String, ModelDef> models = new LinkedHashMap<>();
        BUNDLE.models().forEach(m -> models.put(m.name(), m));
        return new MetaContext(modules, models);
    }

    /** 复制示例里的一个模型，对它的 JSON 做一处改动，返回改动后的定义。 */
    static ModelDef changed(String name, Consumer<ObjectNode> change) {
        ObjectNode node = JSON.valueToTree(model(name));
        change.accept(node);
        return JSON.treeToValue(node, ModelDef.class);
    }

    /** 在模型的 JSON 里找到某个字段的节点。 */
    static ObjectNode field(ObjectNode modelNode, String fieldName) {
        ArrayNode fields = (ArrayNode) modelNode.get("fields");
        return (ObjectNode) fields.get(indexOf(fields, "name", fieldName));
    }

    /** 从字符串数组里删掉一个值，没有就不动。 */
    static void removeValue(ArrayNode array, String value) {
        for (int i = array.size() - 1; i >= 0; i--) {
            if (value.equals(array.get(i).asString())) {
                array.remove(i);
            }
        }
    }

    /** 在对象数组里找某个键等于给定值的那一项的下标。 */
    static int indexOf(ArrayNode array, String key, String value) {
        for (int i = 0; i < array.size(); i++) {
            if (value.equals(array.get(i).get(key).asString())) {
                return i;
            }
        }
        throw new IllegalArgumentException("找不到 " + key + "=" + value);
    }
}
