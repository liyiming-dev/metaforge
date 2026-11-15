package com.metaforge.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.ImportResult;
import com.metaforge.meta.model.MenuNode;
import com.metaforge.meta.model.MetaBundle;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;

/**
 * 元模型的业务逻辑。每个写操作都是同一个顺序：
 * 校验（MetaValidator）→ 补默认值（MetaDefaults）→ 写数据库（MetaRepository）→ 刷新内存（MetaRegistry）。
 *
 * 写操作都加了 synchronized：校验用的是内存里的定义，如果两个保存同时进行，
 * 可能各自校验通过、合起来却不合规则。一次只让一个写操作进行，就没有这个问题。
 * 这只在后端只有一个实例时成立，见决策 010。
 */
@Service
public class MetaService {

    private final MetaRepository repository;
    private final MetaRegistry registry;
    private final TransactionTemplate transaction;

    public MetaService(MetaRepository repository, MetaRegistry registry, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.registry = registry;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    // ---------- 模块 ----------

    public List<ModuleDef> listModules() {
        return registry.modules();
    }

    public synchronized ModuleDef saveModule(String name, ModuleDef input) {
        MetaValidator.validateModule(input);
        if (!name.equals(input.name())) {
            throw BizException.badRequest("地址里的模块标识和请求体里的不一致");
        }
        ModuleDef module = MetaDefaults.normalize(input);
        repository.saveModule(module);
        registry.reload();
        return module;
    }

    public synchronized void deleteModule(String name) {
        if (registry.findModule(name) == null) {
            throw BizException.notFound("模块 " + name + " 不存在");
        }
        boolean hasModels = registry.models().stream().anyMatch(model -> name.equals(model.module()));
        if (hasModels) {
            throw BizException.conflict("模块下还有模型，不能删除");
        }
        repository.deleteModule(name);
        registry.reload();
    }

    // ---------- 模型 ----------

    public List<ModelDef> listModels() {
        return registry.models();
    }

    public ModelDef getModel(String name) {
        return registry.requireModel(name);
    }

    public synchronized ModelDef createModel(ModelDef input) {
        if (input != null && input.name() != null && registry.findModel(input.name()) != null) {
            throw BizException.conflict("模型 " + input.name() + " 已存在");
        }
        MetaValidator.validateModel(input, null, registry.context());
        ModelDef model = MetaDefaults.normalize(input);
        repository.insertModel(model);
        registry.reload();
        return model;
    }

    public synchronized ModelDef updateModel(String name, ModelDef input) {
        ModelDef old = registry.requireModel(name);
        if (input != null && !name.equals(input.name())) {
            throw BizException.badRequest("地址里的模型标识和请求体里的不一致");
        }
        MetaValidator.validateModel(input, old, registry.context());
        ModelDef model = MetaDefaults.normalize(input);
        repository.updateModel(model);
        registry.reload();
        return model;
    }

    public synchronized void deleteModel(String name) {
        registry.requireModel(name);
        for (ModelDef other : registry.models()) {
            if (other.name().equals(name)) {
                continue;
            }
            for (FieldDef field : other.fields()) {
                if (field.relation() != null && name.equals(field.relation().model())) {
                    throw BizException.conflict(
                            "模型 " + name + " 被“" + other.displayName() + "”的关联字段引用，不能删除");
                }
            }
        }
        repository.deleteModel(name);
        registry.reload();
    }

    // ---------- 导入导出 ----------

    public MetaBundle exportAll() {
        return new MetaBundle(registry.modules(), registry.models());
    }

    /**
     * 导入一个包：不存在的新建，已存在的按修改处理。
     * 先把整个包校验完，全部通过才开始写库，并且写库放在一个事务里，
     * 所以要么全部生效，要么全部不生效。
     */
    public synchronized ImportResult importBundle(MetaBundle bundle) {
        if (bundle == null) {
            throw BizException.badRequest("导入的内容不能为空");
        }
        List<ModuleDef> modules = bundle.modules() == null ? List.of() : bundle.modules();
        List<ModelDef> models = bundle.models() == null ? List.of() : bundle.models();

        // 校验时的上下文 = 已有的定义 + 这个包里的定义。包里的模型可以互相引用。
        MetaContext existing = registry.context();
        Set<String> moduleNames = new HashSet<>(existing.moduleNames());
        for (ModuleDef module : modules) {
            MetaValidator.validateModule(module);
            moduleNames.add(module.name());
        }
        Map<String, ModelDef> merged = new LinkedHashMap<>(existing.models());
        Set<String> namesInBundle = new HashSet<>();
        for (ModelDef model : models) {
            if (model == null || model.name() == null) {
                throw BizException.badRequest("包里有没写标识的模型");
            }
            if (!namesInBundle.add(model.name())) {
                throw BizException.badRequest("包里模型 " + model.name() + " 出现了两次");
            }
            merged.put(model.name(), model);
        }
        MetaContext context = new MetaContext(moduleNames, merged);
        for (ModelDef model : models) {
            MetaValidator.validateModel(model, existing.models().get(model.name()), context);
        }

        transaction.executeWithoutResult(status -> {
            for (ModuleDef module : modules) {
                repository.saveModule(MetaDefaults.normalize(module));
            }
            for (ModelDef model : models) {
                ModelDef normalized = MetaDefaults.normalize(model);
                if (existing.models().containsKey(model.name())) {
                    repository.updateModel(normalized);
                } else {
                    repository.insertModel(normalized);
                }
            }
        });
        registry.reload();
        return new ImportResult(modules.size(), models.size());
    }

    // ---------- 菜单 ----------

    /** 菜单树：一级是模块，二级是模块里显示在菜单中的模型。没有可显示模型的模块不返回。 */
    public List<MenuNode> menus() {
        List<MenuNode> menus = new ArrayList<>();
        for (ModuleDef module : registry.modules()) {
            List<MenuNode> children = new ArrayList<>();
            for (ModelDef model : registry.models()) {
                if (module.name().equals(model.module()) && Boolean.TRUE.equals(model.showInMenu())) {
                    children.add(new MenuNode(
                            model.name(), model.displayName(), null, model.name(), "/app/" + model.name(), null));
                }
            }
            if (!children.isEmpty()) {
                menus.add(new MenuNode(module.name(), module.displayName(), module.icon(), null, null, children));
            }
        }
        return menus;
    }
}
