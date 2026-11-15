package com.metaforge.meta;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metaforge.common.Result;
import com.metaforge.meta.model.ImportResult;
import com.metaforge.meta.model.MenuNode;
import com.metaforge.meta.model.MetaBundle;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;

/** 元模型接口，和 docs/contract/api.md 的“元模型接口”一节一一对应。这里只做转发，逻辑在 MetaService。 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final MetaService service;

    public MetaController(MetaService service) {
        this.service = service;
    }

    @GetMapping("/menus")
    public Result<List<MenuNode>> menus() {
        return Result.ok(service.menus());
    }

    @GetMapping("/modules")
    public Result<List<ModuleDef>> modules() {
        return Result.ok(service.listModules());
    }

    @PutMapping("/modules/{name}")
    public Result<ModuleDef> saveModule(@PathVariable String name, @RequestBody ModuleDef module) {
        return Result.ok(service.saveModule(name, module));
    }

    @DeleteMapping("/modules/{name}")
    public Result<Void> deleteModule(@PathVariable String name) {
        service.deleteModule(name);
        return Result.ok();
    }

    @GetMapping("/models")
    public Result<List<ModelDef>> models() {
        return Result.ok(service.listModels());
    }

    @GetMapping("/models/{name}")
    public Result<ModelDef> model(@PathVariable String name) {
        return Result.ok(service.getModel(name));
    }

    @PostMapping("/models")
    public Result<ModelDef> createModel(@RequestBody ModelDef model) {
        return Result.ok(service.createModel(model));
    }

    @PutMapping("/models/{name}")
    public Result<ModelDef> updateModel(@PathVariable String name, @RequestBody ModelDef model) {
        return Result.ok(service.updateModel(name, model));
    }

    @DeleteMapping("/models/{name}")
    public Result<Void> deleteModel(@PathVariable String name) {
        service.deleteModel(name);
        return Result.ok();
    }

    @GetMapping("/export")
    public Result<MetaBundle> exportAll() {
        return Result.ok(service.exportAll());
    }

    @PostMapping("/import")
    public Result<ImportResult> importBundle(@RequestBody MetaBundle bundle) {
        return Result.ok(service.importBundle(bundle));
    }
}
