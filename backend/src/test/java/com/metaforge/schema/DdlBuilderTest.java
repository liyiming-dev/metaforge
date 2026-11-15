package com.metaforge.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.metaforge.meta.MetaDefaults;
import com.metaforge.meta.SampleMeta;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.schema.DdlStep.Kind;

/** 只检查生成的 SQL 字符串，不连数据库。真正执行的效果在真实 MySQL 上验收。 */
class DdlBuilderTest {

    private static final String SUFFIX = "251114120000";

    private final FieldTypeRegistry types = new FieldTypeRegistry();
    private final DdlBuilder ddl = new DdlBuilder(types);

    private static FieldDef field(String type, Integer length, Integer precision, Integer scale) {
        return new FieldDef("f", "字段", type, null, null, null, null, length, precision, scale, null, null);
    }

    private String columnType(String type, Integer length, Integer precision, Integer scale) {
        return types.get(type).columnType(field(type, length, precision, scale));
    }

    private static ModelDef material() {
        return MetaDefaults.normalize(SampleMeta.model("material"));
    }

    private static ModelDef changedMaterial(java.util.function.Consumer<tools.jackson.databind.node.ObjectNode> change) {
        return MetaDefaults.normalize(SampleMeta.changed("material", change));
    }

    // ---------- 每种字段类型的列类型 ----------

    @Test
    void stringUsesVarcharWithLength() {
        assertThat(columnType("string", null, null, null)).isEqualTo("VARCHAR(255)");
        assertThat(columnType("string", 64, null, null)).isEqualTo("VARCHAR(64)");
    }

    @Test
    void textUsesText() {
        assertThat(columnType("text", null, null, null)).isEqualTo("TEXT");
    }

    @Test
    void integerUsesBigint() {
        assertThat(columnType("integer", null, null, null)).isEqualTo("BIGINT");
    }

    @Test
    void decimalUsesPrecisionAndScale() {
        assertThat(columnType("decimal", null, null, null)).isEqualTo("DECIMAL(18,4)");
        assertThat(columnType("decimal", null, 10, 2)).isEqualTo("DECIMAL(10,2)");
        // 精度比默认的小数位数还小时，小数位数不能超过精度
        assertThat(columnType("decimal", null, 2, null)).isEqualTo("DECIMAL(2,2)");
    }

    @Test
    void booleanUsesBoolean() {
        assertThat(columnType("boolean", null, null, null)).isEqualTo("BOOLEAN");
    }

    @Test
    void dateAndDatetime() {
        assertThat(columnType("date", null, null, null)).isEqualTo("DATE");
        assertThat(columnType("datetime", null, null, null)).isEqualTo("DATETIME");
    }

    @Test
    void enumStoresTheOptionValueAsVarchar() {
        assertThat(columnType("enum", null, null, null)).isEqualTo("VARCHAR(64)");
    }

    @Test
    void manyToOneStoresTheTargetId() {
        assertThat(columnType("manyToOne", null, null, null)).isEqualTo("BIGINT");
    }

    @Test
    void oneToManyHasNoColumn() {
        assertThat(columnType("oneToMany", null, null, null)).isNull();
    }

    // ---------- 建表 ----------

    @Test
    void createTableHasSystemColumnsFieldColumnsAndUniqueConstraints() {
        String sql = ddl.createTable(material());

        assertThat(sql).startsWith("CREATE TABLE `md_material` (");
        assertThat(sql).contains(
                "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY",
                "`code` VARCHAR(64) NULL",
                // name 是必填字段，但列仍然允许为空：必填由程序校验
                "`name` VARCHAR(100) NULL",
                "`category` VARCHAR(64) NULL",
                "`safety_stock` DECIMAL(18,2) NULL",
                "`enabled` BOOLEAN NULL",
                "`remark` TEXT NULL",
                "`created_at` DATETIME NOT NULL",
                "`updated_at` DATETIME NOT NULL",
                "CONSTRAINT `uk_material_code` UNIQUE (`code`)");
    }

    @Test
    void createTableSkipsOneToManyFields() {
        String sql = ddl.createTable(MetaDefaults.normalize(SampleMeta.model("product")));

        assertThat(sql).contains("`launch_date` DATE NULL").doesNotContain("bom_lines");
    }

    // ---------- 改表 ----------

    @Test
    void noStepsWhenNothingChanged() {
        assertThat(ddl.alterTable(material(), material(), SUFFIX)).isEmpty();
    }

    @Test
    void addedFieldBecomesAddColumn() {
        ModelDef changed = changedMaterial(n -> SampleMeta.addField(n, "brand", "品牌", "string"));

        assertThat(ddl.alterTable(material(), changed, SUFFIX)).containsExactly(
                new DdlStep(Kind.ADD_COLUMN, "brand", "ALTER TABLE `md_material` ADD COLUMN `brand` VARCHAR(255) NULL"));
    }

    @Test
    void removedFieldIsRenamedAsBackupNotDropped() {
        ModelDef changed = changedMaterial(n -> SampleMeta.removeField(n, "spec"));

        assertThat(ddl.alterTable(material(), changed, SUFFIX)).containsExactly(
                new DdlStep(Kind.BACKUP_COLUMN, "spec",
                        "ALTER TABLE `md_material` RENAME COLUMN `spec` TO `zz_spec_251114120000`"));
    }

    @Test
    void removedUniqueFieldLosesItsConstraintFirst() {
        ModelDef changed = changedMaterial(n -> SampleMeta.removeField(n, "code"));

        List<DdlStep> steps = ddl.alterTable(material(), changed, SUFFIX);

        assertThat(steps).extracting(DdlStep::kind).containsExactly(Kind.DROP_UNIQUE, Kind.BACKUP_COLUMN);
        assertThat(steps.get(0).sql()).isEqualTo("ALTER TABLE `md_material` DROP INDEX `uk_material_code`");
    }

    @Test
    void changedLengthBecomesModifyColumn() {
        ModelDef changed = changedMaterial(n -> SampleMeta.field(n, "name").put("length", 200));

        assertThat(ddl.alterTable(material(), changed, SUFFIX)).containsExactly(
                new DdlStep(Kind.MODIFY_COLUMN, "name", "ALTER TABLE `md_material` MODIFY COLUMN `name` VARCHAR(200) NULL"));
    }

    @Test
    void uniqueTurnedOffBecomesDropIndex() {
        ModelDef changed = changedMaterial(n -> SampleMeta.field(n, "code").put("unique", false));

        assertThat(ddl.alterTable(material(), changed, SUFFIX)).containsExactly(
                new DdlStep(Kind.DROP_UNIQUE, "code", "ALTER TABLE `md_material` DROP INDEX `uk_material_code`"));
    }

    @Test
    void stepsThatCanFailOnExistingDataComeFirst() {
        // 给已有的 name 列加唯一约束可能因为已有重复数据而失败，所以要排在“加新列”前面
        ModelDef changed = changedMaterial(n -> {
            SampleMeta.addField(n, "brand", "品牌", "string");
            SampleMeta.field(n, "name").put("unique", true);
        });

        List<DdlStep> steps = ddl.alterTable(material(), changed, SUFFIX);

        assertThat(steps).extracting(DdlStep::kind).containsExactly(Kind.ADD_UNIQUE, Kind.ADD_COLUMN);
        assertThat(steps.get(0).sql())
                .isEqualTo("ALTER TABLE `md_material` ADD CONSTRAINT `uk_material_name` UNIQUE (`name`)");
    }

    @Test
    void removingOneToManyFieldNeedsNoSchemaChange() {
        ModelDef product = MetaDefaults.normalize(SampleMeta.model("product"));
        ModelDef withoutBom = MetaDefaults.normalize(SampleMeta.changed("product", n -> {
            SampleMeta.removeField(n, "bom_lines");
            ((tools.jackson.databind.node.ArrayNode) n.get("views").get("form").get("tabs")).removeAll();
        }));

        assertThat(ddl.alterTable(product, withoutBom, SUFFIX)).isEmpty();
    }

    // ---------- 删模型 ----------

    @Test
    void deletedModelTableIsRenamedAsBackup() {
        assertThat(ddl.backupTable(material(), SUFFIX))
                .isEqualTo("ALTER TABLE `md_material` RENAME TO `zz_material_251114120000`");
    }

    // ---------- 安全 ----------

    @Test
    void refusesToPutUnsafeNamesIntoSql() {
        ModelDef evil = new ModelDef("x", "坏的", null, "base", true, 100,
                List.of(field("string", null, null, null)), List.of(), null, "md_x`; DROP TABLE meta_model; --");

        assertThatThrownBy(() -> ddl.createTable(evil))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不安全的表名或列名");
    }
}
