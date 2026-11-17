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
import com.metaforge.schema.SchemaSync;

/**
 * 元模型的业务逻辑。保存一个模型的顺序固定是：
 * 校验（MetaValidator）→ 补默认值（MetaDefaults）→ 改表（SchemaSync）→ 存定义（MetaRepository）→ 刷新内存（MetaRegistry）。
 *
 * 为什么“改表”排在“存定义”前面：改表语句执行了就不能回滚。先改表，失败了定义还是旧的，
 * 表和定义仍然对得上；反过来的话，可能出现定义说有某个字段、表里却没有这一列。
 *
 * 写操作都加了 synchronized：校验用的是内存里的定义，如果两个保存同时进行，
 * 可能各自校验通过、合起来却不合规则。一次只让一个写操作进行，就没有这个问题。
 * 这只在后端只有一个实例时成立，见决策 010。
 */
@Service
public class MetaService {

    private final MetaRepository repository;
    private final MetaRegistry registry;
    private final SchemaSync schemaSync;
    private final TransactionTemplate transaction;

    public MetaService(MetaRepository repository, MetaRegistry registry, SchemaSync schemaSync,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.registry = registry;
        this.schemaSync = schemaSync;
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

        schemaSync.create(model);
        try {
            repository.insertModel(model);
        } catch (RuntimeException e) {
            // 表建好了但定义没存成：把刚建的空表删掉，不留下没人认领的表
            schemaSync.dropQuietly(model);
            throw e;
        }
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

        schemaSync.alter(old, model);
        repository.updateModel(model);
        registry.reload();
        return model;
    }

    public synchronized void deleteModel(String name) {
        ModelDef model = registry.requireModel(name);
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
        schemaSync.drop(model);
        repository.deleteModel(name);
        registry.reload();
    }

    // ---------- 导入导出 ----------

    public MetaBundle exportAll() {
        return new MetaBundle(registry.modules(), registry.models());
    }

    /**
     * 导入一个包：不存在的新建，已存在的按修改处理。分三步：
     * 1. 把整个包校验完。有一处不合规则就整个拒绝，这时什么都还没动。
     * 2. 建表、改表。这一步不能回滚；如果中途失败，把这次新建的表删掉。
     * 3. 在一个事务里保存所有定义，要么全部存上，要么全部不存。
     */
    public synchronized ImportResult importBundle(MetaBundle bundle) {
        if (bundle == null) {
            throw BizException.badRequest("导入的内容不能为空");
        }
        List<ModuleDef> modules = bundle.modules() == null ? List.of() : bundle.modules();
        List<ModelDef> models = bundle.models() == null ? List.of() : bundle.models();

        // 第 1 步：校验。上下文 = 已有的定义 + 这个包里的定义，因为包里的模型可以互相引用
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
        List<ModelDef> normalized = models.stream().map(MetaDefaults::normalize).toList();

        // 第 2 步：建表、改表
        List<ModelDef> created = new ArrayList<>();
        try {
            for (ModelDef model : normalized) {
                ModelDef old = existing.models().get(model.name());
                if (old == null) {
                    schemaSync.create(model);
                    created.add(model);
                } else {
                    schemaSync.alter(old, model);
                }
            }

            // 第 3 步：保存定义
            transaction.executeWithoutResult(status -> {
                for (ModuleDef module : modules) {
                    repository.saveModule(MetaDefaults.normalize(module));
                }
                for (ModelDef model : normalized) {
                    if (existing.models().containsKey(model.name())) {
                        repository.updateModel(model);
                    } else {
                        repository.insertModel(model);
                    }
                }
            });
        } catch (RuntimeException e) {
            created.forEach(schemaSync::dropQuietly);
            throw e;
        }
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
