package com.metaforge.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.SystemFields;
import com.metaforge.schema.DdlStep.Kind;

/**
 * 根据模型定义生成建表、改表的 SQL。这个类只生成字符串，不执行，所以可以直接写单元测试。
 * 执行由 SchemaSync 负责。
 *
 * 几条固定的规则（理由见决策记录 002 和 005）：
 * - 每个模型一张表，表名是 md_ 加模型标识；每个占列的字段一列，列名就是字段标识。
 * - 业务列一律允许为空。“必填”由程序在写入前校验，不用数据库的 NOT NULL。
 * - “唯一”交给数据库的唯一约束，约束名是 uk_模型标识_字段标识。
 * - 字段被删除时，列不删，改名为 zz_ 开头的备份列。
 * - 模型被删除时，表不删，改名为 zz_ 开头的备份表。
 */
@Component
public class DdlBuilder {

    /** 备份列、备份表的名字前缀 */
    public static final String BACKUP_PREFIX = "zz_";

    /**
     * 能拼进 SQL 的名字只能是这个样子。表名、列名都来自校验过的元模型，本来就符合；
     * 这里再查一遍，是为了即使别处漏了校验，也不会有奇怪的字符被拼进 SQL。
     */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final FieldTypeRegistry types;

    public DdlBuilder(FieldTypeRegistry types) {
        this.types = types;
    }

    /** 建表语句。 */
    public String createTable(ModelDef model) {
        List<String> lines = new ArrayList<>();
        List<String> uniques = new ArrayList<>();
        lines.add(quote(SystemFields.ID) + " BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY");
        for (FieldDef field : model.fields()) {
            String columnType = columnType(field);
            if (columnType == null) {
                continue;
            }
            lines.add(quote(field.name()) + " " + columnType + " NULL");
            if (isUnique(field)) {
                uniques.add("CONSTRAINT " + quote(uniqueName(model, field)) + " UNIQUE (" + quote(field.name()) + ")");
            }
        }
        lines.add(quote(SystemFields.CREATED_AT) + " DATETIME NOT NULL");
        lines.add(quote(SystemFields.UPDATED_AT) + " DATETIME NOT NULL");
        lines.addAll(uniques);
        return "CREATE TABLE " + quote(model.tableName()) + " (\n  " + String.join(",\n  ", lines) + "\n)";
    }

    /**
     * 比较新旧定义，生成改表要执行的步骤。没有变化时返回空列表。
     *
     * 步骤的顺序是有意安排的：可能因为已有数据而失败的步骤（改长度、给已有列加唯一约束）排在最前面。
     * MySQL 的改表语句执行了就不能撤销，把容易失败的放前面，失败时表还没被动过。
     *
     * @param backupSuffix 备份列名的后缀，一般是时间，保证每次的备份列名不同
     */
    public List<DdlStep> alterTable(ModelDef oldModel, ModelDef newModel, String backupSuffix) {
        String table = quote(newModel.tableName());
        List<DdlStep> risky = new ArrayList<>();
        List<DdlStep> safe = new ArrayList<>();

        for (FieldDef field : newModel.fields()) {
            String columnType = columnType(field);
            if (columnType == null) {
                continue;
            }
            String column = quote(field.name());
            String addUnique = "ALTER TABLE " + table + " ADD CONSTRAINT " + quote(uniqueName(newModel, field))
                    + " UNIQUE (" + column + ")";
            FieldDef before = oldModel.findField(field.name());

            if (before == null) {
                // 新增的字段：加列。新列里全是空值，加唯一约束不会失败
                safe.add(new DdlStep(Kind.ADD_COLUMN, field.name(),
                        "ALTER TABLE " + table + " ADD COLUMN " + column + " " + columnType + " NULL"));
                if (isUnique(field)) {
                    safe.add(new DdlStep(Kind.ADD_UNIQUE, field.name(), addUnique));
                }
                continue;
            }

            // 已有的字段：类型不允许改（校验器保证），但长度、精度、是否唯一可以变
            if (!columnType.equals(columnType(before))) {
                risky.add(new DdlStep(Kind.MODIFY_COLUMN, field.name(),
                        "ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + columnType + " NULL"));
            }
            if (isUnique(field) && !isUnique(before)) {
                risky.add(new DdlStep(Kind.ADD_UNIQUE, field.name(), addUnique));
            }
            if (!isUnique(field) && isUnique(before)) {
                safe.add(new DdlStep(Kind.DROP_UNIQUE, field.name(), dropUnique(newModel, before)));
            }
        }

        // 被删除的字段：先去掉唯一约束，再把列改名留作备份
        for (FieldDef before : oldModel.fields()) {
            if (columnType(before) == null || newModel.findField(before.name()) != null) {
                continue;
            }
            if (isUnique(before)) {
                safe.add(new DdlStep(Kind.DROP_UNIQUE, before.name(), dropUnique(newModel, before)));
            }
            String backup = BACKUP_PREFIX + before.name() + "_" + backupSuffix;
            safe.add(new DdlStep(Kind.BACKUP_COLUMN, before.name(),
                    "ALTER TABLE " + table + " RENAME COLUMN " + quote(before.name()) + " TO " + quote(backup)));
        }

        List<DdlStep> steps = new ArrayList<>(risky);
        steps.addAll(safe);
        return steps;
    }

    /** 模型被删除时：把表改名留作备份。 */
    public String backupTable(ModelDef model, String backupSuffix) {
        String backup = BACKUP_PREFIX + model.name() + "_" + backupSuffix;
        return "ALTER TABLE " + quote(model.tableName()) + " RENAME TO " + quote(backup);
    }

    /** 一条不返回任何数据的查询，只用来试探这张表在不在：表不存在时执行它会报错。 */
    public String probeTable(ModelDef model) {
        return "SELECT 1 FROM " + quote(model.tableName()) + " WHERE 1 = 0";
    }

    /** 真正删表。只用于“表刚建好、定义却没存成”时的善后。 */
    public String dropTableIfExists(ModelDef model) {
        return "DROP TABLE IF EXISTS " + quote(model.tableName());
    }

    /** 唯一约束的名字。任务 B3 会从“违反了哪个约束”反查出是哪个字段重复了。 */
    public static String uniqueName(ModelDef model, FieldDef field) {
        return "uk_" + model.name() + "_" + field.name();
    }

    private String dropUnique(ModelDef model, FieldDef field) {
        return "ALTER TABLE " + quote(model.tableName()) + " DROP INDEX " + quote(uniqueName(model, field));
    }

    private String columnType(FieldDef field) {
        return types.get(field.type()).columnType(field);
    }

    private static boolean isUnique(FieldDef field) {
        return Boolean.TRUE.equals(field.unique());
    }

    /** 给名字加上反引号。名字不符合安全格式就直接报错，不拼进 SQL。 */
    private static String quote(String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("不安全的表名或列名：" + identifier);
        }
        return "`" + identifier + "`";
    }
}
