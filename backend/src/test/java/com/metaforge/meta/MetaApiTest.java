package com.metaforge.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.metaforge.TestTables;

/** 从接口这一层测试 /api/meta：请求进来，经过校验、存库、刷新内存，再读出来。用内存数据库。 */
@SpringBootTest
@ActiveProfiles("test")
class MetaApiTest {

    private static final String SUPPLIER = """
            {
              "name": "supplier",
              "displayName": "供应商",
              "module": "base",
              "fields": [
                { "name": "name", "displayName": "名称", "type": "string", "required": true },
                { "name": "phone", "displayName": "电话", "type": "string" },
                { "name": "level", "displayName": "信用等级", "type": "enum",
                  "options": [ { "value": "A", "label": "A" }, { "value": "B", "label": "B" } ] }
              ]
            }
            """;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MetaRegistry registry;

    private MockMvc mvc;
    private String sampleJson;

    @BeforeEach
    void startFromEmpty() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        sampleJson = Files.readString(Path.of("..", "docs", "contract", "samples", "mes.json"));
        // 每个测试都从空的元模型和空的数据库开始，互不影响
        jdbc.update("DELETE FROM meta_model");
        jdbc.update("DELETE FROM meta_module");
        TestTables.dropBusinessTables(jdbc);
        registry.reload();
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions importSample() throws Exception {
        return send(post("/api/meta/import"), sampleJson);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void importsTheContractSample() throws Exception {
        importSample()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modules").value(2))
                .andExpect(jsonPath("$.data.models").value(4));

        mvc.perform(get("/api/meta/models/material"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fields", hasSize(8)))
                .andExpect(jsonPath("$.data.tableName").value("md_material"))
                .andExpect(jsonPath("$.data.views.form.groups", hasSize(2)));

        // 每个模型的表都真的建出来了
        assertThat(TestTables.businessTables(jdbc))
                .containsExactly("md_code_rule", "md_material", "md_product", "md_product_bom");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `md_material`", Integer.class)).isZero();
    }

    @Test
    void addingAFieldAddsAColumnWithoutRestart() throws Exception {
        importSample();
        jdbc.update("INSERT INTO `md_material` (`name`, `created_at`, `updated_at`) VALUES ('螺丝 M3', NOW(), NOW())");
        String withBrand = SampleMeta.JSON.writeValueAsString(
                SampleMeta.changed("material", n -> SampleMeta.addField(n, "brand", "品牌", "string")));

        send(put("/api/meta/models/material"), withBrand).andExpect(status().isOk());

        // 新列出现了，原来的那条数据还在，它在新列上是空值
        assertThat(jdbc.queryForList("SELECT `name`, `brand` FROM `md_material`"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.get("name")).isEqualTo("螺丝 M3");
                    assertThat(row.get("brand")).isNull();
                });
    }

    @Test
    void deletingAModelKeepsItsTableAsBackup() throws Exception {
        importSample();
        send(post("/api/meta/models"), SUPPLIER).andExpect(status().isOk());
        assertThat(TestTables.businessTables(jdbc)).contains("md_supplier");

        mvc.perform(delete("/api/meta/models/supplier")).andExpect(status().isOk());

        assertThat(TestTables.businessTables(jdbc))
                .doesNotContain("md_supplier")
                .anyMatch(name -> name.startsWith("zz_supplier_"));
    }

    @Test
    void sameModelCanBeDeletedAndRecreatedRepeatedly() throws Exception {
        importSample();

        // 连着“新建、删除”三次。备份表的名字如果只精确到秒，第二次删除就会因为重名而失败
        for (int i = 0; i < 3; i++) {
            send(post("/api/meta/models"), SUPPLIER).andExpect(status().isOk());
            mvc.perform(delete("/api/meta/models/supplier")).andExpect(status().isOk());
        }

        assertThat(TestTables.businessTables(jdbc).stream().filter(name -> name.startsWith("zz_supplier_")))
                .hasSize(3);
    }

    @Test
    void modelWhoseTableIsMissingCanStillBeDeleted() throws Exception {
        importSample();
        send(post("/api/meta/models"), SUPPLIER).andExpect(status().isOk());
        // 人为制造“定义还在、表没了”的状态
        jdbc.execute("DROP TABLE `md_supplier`");

        mvc.perform(delete("/api/meta/models/supplier")).andExpect(status().isOk());

        mvc.perform(get("/api/meta/models/supplier")).andExpect(status().isNotFound());
    }

    @Test
    void leftoverTableIsBackedUpBeforeCreatingTheModel() throws Exception {
        importSample();
        // 人为制造“有同名的表、却没有定义”的状态，里面有一条数据
        jdbc.execute("CREATE TABLE `md_supplier` (`old_data` VARCHAR(20))");
        jdbc.update("INSERT INTO `md_supplier` (`old_data`) VALUES ('keep me')");

        send(post("/api/meta/models"), SUPPLIER).andExpect(status().isOk());

        // 新表是按定义建的；原来那张表被改名留作备份，数据没有丢
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `md_supplier`", Integer.class)).isZero();
        String backup = TestTables.businessTables(jdbc).stream()
                .filter(name -> name.startsWith("zz_supplier_")).findFirst().orElseThrow();
        assertThat(jdbc.queryForObject("SELECT `old_data` FROM `" + backup + "`", String.class)).isEqualTo("keep me");
    }

    @Test
    void menusGroupVisibleModelsByModule() throws Exception {
        importSample();

        mvc.perform(get("/api/meta/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].key").value("mes"))
                .andExpect(jsonPath("$.data[0].displayName").value("生产管理"))
                // product_bom 的 showInMenu 是 false，不出现在菜单里
                .andExpect(jsonPath("$.data[0].children[*].model", contains("material", "product")))
                .andExpect(jsonPath("$.data[0].children[0].path").value("/app/material"))
                .andExpect(jsonPath("$.data[1].key").value("base"))
                .andExpect(jsonPath("$.data[1].children[*].model", contains("code_rule")));
    }

    @Test
    void createsModelWithDefaultViewsAndShowsItInMenu() throws Exception {
        importSample();

        send(post("/api/meta/models"), SUPPLIER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.views.list.columns", contains("name", "phone", "level")))
                .andExpect(jsonPath("$.data.views.form.groups[0].title").value("基本信息"))
                .andExpect(jsonPath("$.data.showInMenu").value(true));

        mvc.perform(get("/api/meta/menus"))
                .andExpect(jsonPath("$.data[1].children[*].model", contains("code_rule", "supplier")));
    }

    @Test
    void rejectsBadNameAndDuplicate() throws Exception {
        importSample();

        send(post("/api/meta/models"), SUPPLIER.replace("\"supplier\"", "\"Bad Name\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("不合规则")));

        send(post("/api/meta/models"), SUPPLIER).andExpect(status().isOk());
        send(post("/api/meta/models"), SUPPLIER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void returnsNotFoundForUnknownModel() throws Exception {
        mvc.perform(get("/api/meta/models/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void rejectsTypeChangeOnUpdate() throws Exception {
        importSample();
        String changed = SampleMeta.JSON.writeValueAsString(
                SampleMeta.changed("material", n -> SampleMeta.field(n, "spec").put("type", "text")));

        send(put("/api/meta/models/material"), changed)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("不能修改字段 spec 的类型")));
    }

    @Test
    void refusesToDeleteWhatIsStillReferenced() throws Exception {
        importSample();

        // 物料被产品 BOM 的关联字段引用着
        mvc.perform(delete("/api/meta/models/material"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("产品 BOM")));
        // 模块下还有模型
        mvc.perform(delete("/api/meta/modules/mes")).andExpect(status().isConflict());

        // 产品和产品 BOM 互相引用：产品有一对多指向 BOM，BOM 有多对一指回产品。所以两个都不能直接删
        mvc.perform(delete("/api/meta/models/product"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("产品 BOM")));
        mvc.perform(delete("/api/meta/models/product_bom"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("产品")));

        // 解开的办法：先修改产品，去掉“BOM 明细”这个一对多字段和对应的标签页
        String productWithoutBom = SampleMeta.JSON.writeValueAsString(SampleMeta.changed("product", n -> {
            tools.jackson.databind.node.ArrayNode fields = (tools.jackson.databind.node.ArrayNode) n.get("fields");
            fields.remove(SampleMeta.indexOf(fields, "name", "bom_lines"));
            ((tools.jackson.databind.node.ArrayNode) n.get("views").get("form").get("tabs")).removeAll();
        }));
        send(put("/api/meta/models/product"), productWithoutBom).andExpect(status().isOk());

        // 之后按“先删引用别人的，再删被引用的”顺序就能删了
        mvc.perform(delete("/api/meta/models/product_bom")).andExpect(status().isOk());
        mvc.perform(delete("/api/meta/models/product")).andExpect(status().isOk());
        mvc.perform(delete("/api/meta/models/material")).andExpect(status().isOk());
        mvc.perform(delete("/api/meta/modules/mes")).andExpect(status().isOk());
    }

    @Test
    void importIsAllOrNothing() throws Exception {
        String bundle = """
                {
                  "modules": [ { "name": "crm", "displayName": "客户管理" } ],
                  "models": [
                    { "name": "customer", "displayName": "客户", "module": "crm",
                      "fields": [ { "name": "name", "displayName": "名称", "type": "string" } ] },
                    { "name": "Bad Name", "displayName": "坏的", "module": "crm",
                      "fields": [ { "name": "name", "displayName": "名称", "type": "string" } ] }
                  ]
                }
                """;

        send(post("/api/meta/import"), bundle).andExpect(status().isBadRequest());

        // 第二个模型不合规则，所以第一个模型和模块也都没有被创建
        mvc.perform(get("/api/meta/models")).andExpect(jsonPath("$.data", hasSize(0)));
        mvc.perform(get("/api/meta/modules")).andExpect(jsonPath("$.data", hasSize(0)));
        // 校验在建表之前，所以也没有任何表被建出来
        assertThat(TestTables.businessTables(jdbc)).isEmpty();
    }

    @Test
    void exportedBundleCanBeImportedBackUnchanged() throws Exception {
        importSample();
        String first = SampleMeta.JSON.readTree(body(mvc.perform(get("/api/meta/export")))).get("data").toString();

        send(post("/api/meta/import"), first).andExpect(status().isOk());
        String second = SampleMeta.JSON.readTree(body(mvc.perform(get("/api/meta/export")))).get("data").toString();

        assertThat(second).isEqualTo(first);
    }
}
