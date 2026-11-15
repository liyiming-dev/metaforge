package com.metaforge.meta;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;

import jakarta.annotation.PostConstruct;

/**
 * 内存里的元模型注册表：所有模块和模型定义在内存里放一份，读取都走这里，不用每次查数据库。
 *
 * 更新的办法是“整份替换”：数据库写成功之后调用 reload()，重新读出全部定义，
 * 组装成一份新的快照，再一次性换上去。读的人要么看到旧的一整份，要么看到新的一整份，
 * 不会看到改了一半的状态。定义总量很小，整份重读的开销可以忽略。
 */
@Component
public class MetaRegistry {

    /** 一份不可变的快照。两个 Map 都按显示顺序排好了。 */
    private record Snapshot(Map<String, ModuleDef> modules, Map<String, ModelDef> models) {
    }

    private static final Comparator<ModuleDef> MODULE_ORDER =
            Comparator.comparing(ModuleDef::sort).thenComparing(ModuleDef::name);

    private final MetaRepository repository;

    private volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of());

    public MetaRegistry(MetaRepository repository) {
        this.repository = repository;
    }

    /** 启动时执行一次；之后每次元模型有改动，由 MetaService 调用。 */
    @PostConstruct
    public void reload() {
        Map<String, ModuleDef> modules = new LinkedHashMap<>();
        repository.findAllModules().stream()
                .sorted(MODULE_ORDER)
                .forEach(module -> modules.put(module.name(), module));

        // 模型的顺序：先按所属模块的顺序，再按模型自己的 sort，最后按标识
        List<String> moduleOrder = List.copyOf(modules.keySet());
        Comparator<ModelDef> modelOrder = Comparator
                .comparingInt((ModelDef model) -> {
                    int index = moduleOrder.indexOf(model.module());
                    return index < 0 ? Integer.MAX_VALUE : index;
                })
                .thenComparing(ModelDef::sort)
                .thenComparing(ModelDef::name);
        Map<String, ModelDef> models = new LinkedHashMap<>();
        repository.findAllModels().stream()
                .sorted(modelOrder)
                .forEach(model -> models.put(model.name(), model));

        snapshot = new Snapshot(Collections.unmodifiableMap(modules), Collections.unmodifiableMap(models));
    }

    public List<ModuleDef> modules() {
        return List.copyOf(snapshot.modules().values());
    }

    public List<ModelDef> models() {
        return List.copyOf(snapshot.models().values());
    }

    /** 找不到返回 null。 */
    public ModuleDef findModule(String name) {
        return snapshot.modules().get(name);
    }

    /** 找不到返回 null。 */
    public ModelDef findModel(String name) {
        return snapshot.models().get(name);
    }

    /** 找不到就抛 404。数据接口和服务接口用它。 */
    public ModelDef requireModel(String name) {
        ModelDef model = findModel(name);
        if (model == null) {
            throw BizException.notFound("模型 " + name + " 不存在");
        }
        return model;
    }

    /** 当前所有定义，给校验器当上下文用。 */
    public MetaContext context() {
        Snapshot current = snapshot;
        return new MetaContext(current.modules().keySet(), current.models());
    }
}
