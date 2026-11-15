package com.metaforge.schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.metaforge.common.BizException;
import com.metaforge.meta.model.ModelDef;

/**
 * 表结构同步：让数据库里的表跟上模型定义。SQL 由 DdlBuilder 生成，这里负责执行，
 * 并把执行失败翻译成用户看得懂的提示。
 *
 * 要记住的一点：MySQL 的建表、改表语句一执行就生效，不能像普通的增删改那样回滚。
 * 所以 MetaService 里的顺序是“先改表，改成功了再保存定义”，改表失败时定义保持不变。
 *
 * 即使这样，表和定义还是有可能对不上（比如改表成功后存定义那一步失败了）。
 * 所以 create 和 drop 都先看一眼表在不在，对不上时自己把状态理顺，而不是直接报错卡死。
 */
@Component
public class SchemaSync {

    private static final Logger log = LoggerFactory.getLogger(SchemaSync.class);

    /**
     * 备份名的后缀，精确到毫秒。
     * 一开始只精确到秒，结果同一秒内把同一个模型删除两次时，两次的备份表重名，第二次删除失败。
     */
    private static final DateTimeFormatter BACKUP_SUFFIX = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");

    private final JdbcTemplate jdbc;
    private final DdlBuilder ddl;

    public SchemaSync(JdbcTemplate jdbc, DdlBuilder ddl) {
        this.jdbc = jdbc;
        this.ddl = ddl;
    }

    /** 新建模型：建表。 */
    public void create(ModelDef model) {
        if (tableExists(model)) {
            // 有同名的表，却没有对应的模型定义。不知道里面的数据还有没有用，不能覆盖，先改名留作备份
            log.warn("表 {} 已经存在但没有对应的模型定义，先把它改名留作备份", model.tableName());
            execute(ddl.backupTable(model, backupSuffix()));
        }
        execute(ddl.createTable(model));
    }

    /** 修改模型：按新旧定义的差别改表。 */
    public void alter(ModelDef oldModel, ModelDef newModel) {
        for (DdlStep step : ddl.alterTable(oldModel, newModel, backupSuffix())) {
            try {
                execute(step.sql());
            } catch (DataAccessException e) {
                switch (step.kind()) {
                    case ADD_UNIQUE -> throw BizException.conflict(
                            "字段 " + step.field() + " 已有重复的值，不能设为唯一");
                    case MODIFY_COLUMN -> throw BizException.badRequest(
                            "字段 " + step.field() + " 现有的数据放不进新的长度或精度");
                    default -> throw e;
                }
            }
        }
    }

    /** 删除模型：把表改名留作备份。 */
    public void drop(ModelDef model) {
        if (!tableExists(model)) {
            // 定义还在、表却没了。没有数据可备份，让删除定义的操作继续进行
            log.warn("表 {} 不存在，没有可以备份的数据", model.tableName());
            return;
        }
        execute(ddl.backupTable(model, backupSuffix()));
    }

    /** 善后用：表刚建好、定义却没存成时，把这张空表删掉。失败了只记日志，不再抛错。 */
    public void dropQuietly(ModelDef model) {
        try {
            execute(ddl.dropTableIfExists(model));
        } catch (RuntimeException e) {
            log.warn("清理表 {} 失败，需要手工处理", model.tableName(), e);
        }
    }

    private boolean tableExists(ModelDef model) {
        try {
            jdbc.execute(ddl.probeTable(model));
            return true;
        } catch (BadSqlGrammarException e) {
            return false;
        }
    }

    private void execute(String sql) {
        log.info("执行表结构变更：{}", sql.replace("\n", " "));
        jdbc.execute(sql);
    }

    private static String backupSuffix() {
        return LocalDateTime.now().format(BACKUP_SUFFIX);
    }
}
