package com.metaforge.meta;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;

import tools.jackson.databind.ObjectMapper;

/**
 * 元模型的存取：只负责读写 meta_module 和 meta_model 两张表，不做任何校验和业务判断。
 * 模型定义整个转成 JSON 存在 definition 列里，所以存一个模型只要一条 SQL。
 */
@Repository
public class MetaRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public MetaRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ---------- 模块 ----------

    public List<ModuleDef> findAllModules() {
        return jdbc.query(
                "SELECT name, display_name, icon, sort FROM meta_module",
                (rs, rowNum) -> new ModuleDef(
                        rs.getString("name"),
                        rs.getString("display_name"),
                        rs.getString("icon"),
                        rs.getInt("sort")));
    }

    /** 有就改，没有就新增。 */
    public void saveModule(ModuleDef module) {
        int updated = jdbc.update(
                "UPDATE meta_module SET display_name = ?, icon = ?, sort = ? WHERE name = ?",
                module.displayName(), module.icon(), module.sort(), module.name());
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO meta_module (name, display_name, icon, sort) VALUES (?, ?, ?, ?)",
                    module.name(), module.displayName(), module.icon(), module.sort());
        }
    }

    public void deleteModule(String name) {
        jdbc.update("DELETE FROM meta_module WHERE name = ?", name);
    }

    // ---------- 模型 ----------

    public List<ModelDef> findAllModels() {
        return jdbc.query(
                "SELECT definition FROM meta_model",
                (rs, rowNum) -> json.readValue(rs.getString("definition"), ModelDef.class));
    }

    public void insertModel(ModelDef model) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update(
                "INSERT INTO meta_model (name, module_name, definition, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                model.name(), model.module(), json.writeValueAsString(model), now, now);
    }

    public void updateModel(ModelDef model) {
        jdbc.update(
                "UPDATE meta_model SET module_name = ?, definition = ?, updated_at = ? WHERE name = ?",
                model.module(), json.writeValueAsString(model), LocalDateTime.now(), model.name());
    }

    public void deleteModel(String name) {
        jdbc.update("DELETE FROM meta_model WHERE name = ?", name);
    }
}
