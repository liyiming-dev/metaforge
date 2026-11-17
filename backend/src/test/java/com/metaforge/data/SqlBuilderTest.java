package com.metaforge.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.metaforge.common.BizException;
import com.metaforge.meta.MetaDefaults;
import com.metaforge.meta.SampleMeta;
import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.SortItem;
import com.metaforge.schema.FieldTypeRegistry;

/**
 * 只检查拼出来的 SQL 字符串和参数表，不连数据库。
 * 这里重点测决策 004 的白名单规则：字段名、比较方式、排序方向都是“只认定义里有的”，
 * 其余一律 400，不会被拼进 SQL。
 */
class SqlBuilderTest {

    private final SqlBuilder sql = new SqlBuilder(new FieldValues(new FieldTypeRegistry()));

    private static ModelDef material() {
        return MetaDefaults.normalize(SampleMeta.model("material"));
    }

    private static SearchRequest request(int page, int size, List<SearchRequest.Filter> filters, List<SortItem> sort) {
        return new SearchRequest(page, size, filters, sort);
    }

    private static SearchRequest.Filter filter(String field, String op, Object value) {
        return new SearchRequest.Filter(field, op, value);
    }

    // ---------- 白名单：不合法的字段名、比较方式、排序方向 ----------

    @Test
    void rejectsFilterFieldThatIsNotInTheModel() {
        // 注入测试一：字段名里带一句 DROP TABLE，它只会在定义里查一次，查不到就 400
        SearchRequest request = request(1, 10,
                List.of(filter("name; DROP TABLE md_material", "eq", "x")), null);

        assertThatThrownBy(() -> sql.search(material(), request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不是模型");
    }

    @Test
    void rejectsOperatorOutsideTheWhitelist() {
        SearchRequest request = request(1, 10, List.of(filter("name", "regexp", "x")), null);

        assertThatThrownBy(() -> sql.search(material(), request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持的比较方式");
    }

    @Test
    void rejectsSortDirectionThatIsNotAscOrDesc() {
        // 注入测试三：排序方向里带一句 drop table
        SearchRequest request = request(1, 10, null, List.of(new SortItem("name", "desc; drop table")));

        assertThatThrownBy(() -> sql.search(material(), request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("排序方向");
    }

    @Test
    void rejectsOneToManyFieldAsFilter() {
        ModelDef product = MetaDefaults.normalize(SampleMeta.model("product"));
        SearchRequest request = request(1, 10, List.of(filter("bom_lines", "eq", 1)), null);

        assertThatThrownBy(() -> sql.search(product, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("子表");
    }

    // ---------- 值的处理 ----------

    @Test
    void likeEscapesPercentAndUnderscore() {
        SearchRequest request = request(1, 10, List.of(filter("name", "like", "50%_off")), null);

        SqlBuilder.SearchSql query = sql.search(material(), request);

        assertThat(query.select()).contains("`name` LIKE :p0");
        // % 和 _ 前面加上反斜杠，查询时就按普通字符匹配，不会被当成通配符
        assertThat(query.params().getValue("p0")).isEqualTo("%50\\%\\_off%");
    }

    @Test
    void inExpandsToSeveralParameters() {
        SearchRequest request = request(1, 10, List.of(filter("category", "in", List.of("raw", "semi"))), null);

        SqlBuilder.SearchSql query = sql.search(material(), request);

        assertThat(query.select()).contains("`category` IN (:p0, :p1)");
        assertThat(query.params().getValue("p0")).isEqualTo("raw");
        assertThat(query.params().getValue("p1")).isEqualTo("semi");
    }

    @Test
    void valuesAreConvertedByFieldTypeBeforeBinding() {
        // 日期字段的过滤值也要先转成 LocalDate，再作为参数绑上去
        ModelDef product = MetaDefaults.normalize(SampleMeta.model("product"));
        SearchRequest request = request(1, 10, List.of(filter("launch_date", "ge", "2026-01-01")), null);

        SqlBuilder.SearchSql query = sql.search(product, request);

        assertThat(query.params().getValue("p0")).isEqualTo(java.time.LocalDate.of(2026, 1, 1));
    }

    @Test
    void emptyInListIsRejected() {
        SearchRequest request = request(1, 10, List.of(filter("category", "in", List.of())), null);

        assertThatThrownBy(() -> sql.search(material(), request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非空数组");
    }

    // ---------- 分页和排序 ----------

    @Test
    void pageAndSizeHaveBounds() {
        assertThatThrownBy(() -> sql.search(material(), request(0, 10, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("page");
        assertThatThrownBy(() -> sql.search(material(), request(1, 201, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("size");
    }

    @Test
    void defaultSortComesFromTheModelView() {
        SqlBuilder.SearchSql query = sql.search(material(), request(1, 10, null, null));

        assertThat(query.select()).contains("ORDER BY `created_at` DESC");
        assertThat(query.params().getValue("limit")).isEqualTo(10);
        assertThat(query.params().getValue("offset")).isEqualTo(0L);
    }

    @Test
    void givenSortWinsOverDefault() {
        SqlBuilder.SearchSql query = sql.search(material(),
                request(3, 20, null, List.of(new SortItem("name", "ASC"), new SortItem("id", "desc"))));

        assertThat(query.select()).contains("ORDER BY `name` ASC, `id` DESC");
        assertThat(query.params().getValue("offset")).isEqualTo(40L);
    }

    // ---------- 增删改的 SQL ----------

    @Test
    void insertCoversAllStoredColumnsButNotId() {
        String insert = sql.insert(material());

        assertThat(insert).startsWith("INSERT INTO `md_material`");
        assertThat(insert).doesNotContain("`id`");
        assertThat(insert).contains("`code`", "`name`", ":code", ":name", ":created_at", ":updated_at");
    }

    @Test
    void updateNeverWritesCreatedAt() {
        String update = sql.update(material());

        assertThat(update).startsWith("UPDATE `md_material` SET");
        assertThat(update).contains("`updated_at` = :updated_at");
        assertThat(update).doesNotContain("`created_at`");
        assertThat(update).endsWith("WHERE `id` = :id");
    }

    @Test
    void selectReadsOnlyDefinedColumns() {
        // 产品有一对多字段 bom_lines，它不占列，读记录时不应该出现
        String select = sql.selectById(MetaDefaults.normalize(SampleMeta.model("product")));

        assertThat(select).isEqualTo(
                "SELECT `id`, `code`, `name`, `spec`, `launch_date`, `created_at`, `updated_at`"
                        + " FROM `md_product` WHERE `id` = :id");
    }

    // ---------- 关联：options、label 批量查询、删除引用 ----------

    @Test
    void optionsUsesWhitelistedLabelAndEscapesKeyword() {
        ModelDef material = material();
        FieldDef label = sql.labelField(material, "name");

        SqlBuilder.OptionsSql query = sql.options(material, label, "50%_off", 20);

        assertThat(query.select()).contains("`name` AS `label`", "`name` LIKE :p0", "LIMIT :limit");
        assertThat(query.params().getValue("p0")).isEqualTo("%50\\%\\_off%");
        assertThat(query.params().getValue("limit")).isEqualTo(20);
    }

    @Test
    void labelFieldFallsBackToFirstStringFieldAndRejectsUnknown() {
        ModelDef material = material();

        // material 的第一个 string 字段是 code
        assertThat(sql.labelField(material, null).name()).isEqualTo("code");
        assertThat(sql.labelField(material, "name").name()).isEqualTo("name");
        assertThatThrownBy(() -> sql.labelField(material, "nope"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不是模型");
    }

    @Test
    void labelSelectUsesOneInQuery() {
        ModelDef material = material();
        String select = sql.labelSelect(material, sql.labelField(material, "name"));

        assertThat(select).contains("WHERE `id` IN (:ids)");
        assertThat(select).contains("`name` AS `label`");
    }

    @Test
    void relationExistenceAndReferenceSqlUseParametersOnly() {
        ModelDef material = material();
        ModelDef bom = MetaDefaults.normalize(SampleMeta.model("product_bom"));
        FieldDef materialField = bom.findField("material");

        assertThat(sql.existsById(material)).isEqualTo(
                "SELECT COUNT(*) FROM `md_material` WHERE `id` = :id");
        assertThat(sql.countReferences(bom, materialField)).isEqualTo(
                "SELECT COUNT(*) FROM `md_product_bom` WHERE `material` = :id");
    }

    // ---------- 第二道名字检查 ----------

    @Test
    void unsafeIdentifierIsRejectedBeforeReachingSql() {
        assertThatThrownBy(() -> SqlBuilder.quote("md_material; DROP TABLE x"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不合法");
        assertThat(SqlBuilder.quote("md_material")).isEqualTo("`md_material`");
    }
}
