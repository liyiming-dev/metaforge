package com.metaforge.data;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.metaforge.TestTables;
import com.metaforge.meta.MetaRegistry;
import com.metaforge.meta.SampleMeta;

/**
 * B4 关联关系的接口测试：多对一写入校验、读出 {id,label}、options、删除引用保护。
 * 用 H2；唯一约束名、真实 SQL 的条数等再在真实 MySQL 上验一遍。
 */
@SpringBootTest
@ActiveProfiles("test")
class RelationApiTest {

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
        jdbc.update("DELETE FROM meta_model");
        jdbc.update("DELETE FROM meta_module");
        TestTables.dropBusinessTables(jdbc);
        registry.reload();
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void importSample() throws Exception {
        send(post("/api/meta/import"), sampleJson).andExpect(status().isOk());
        // B5：物料/产品的 beforeCreate 脚本会找对应编码规则；测试里先把两条规则建好
        seedCodeRule("material", "MAT-");
        seedCodeRule("product", "PROD-");
    }

    private void seedCodeRule(String name, String prefix) throws Exception {
        send(post("/api/data/code_rule"),
                "{\"name\":\"" + name + "\",\"prefix\":\"" + prefix
                        + "\",\"date_format\":\"yyyyMMdd\",\"serial_digits\":4}")
                .andExpect(status().isOk());
    }

    private long create(String model, String body) throws Exception {
        String response = send(post("/api/data/" + model), body)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return SampleMeta.JSON.readTree(response).get("data").get("id").asLong();
    }

    // ---------- 多对一读出 ----------

    @Test
    void manyToOneValuesComeBackWithLabels() throws Exception {
        importSample();
        long material = create("material", "{\"code\":\"MAT-1\",\"name\":\"螺丝 M3\"}");
        long product = create("product", "{\"name\":\"电动螺丝刀\"}");
        long bom = create("product_bom",
                "{\"product\":" + product + ",\"material\":" + material + ",\"quantity\":12}");

        mvc.perform(get("/api/data/product_bom/" + bom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.id").value(product))
                .andExpect(jsonPath("$.data.product.label").value("电动螺丝刀"))
                .andExpect(jsonPath("$.data.material.id").value(material))
                .andExpect(jsonPath("$.data.material.label").value("螺丝 M3"));

        send(post("/api/data/product_bom/search"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].product.label").value("电动螺丝刀"))
                .andExpect(jsonPath("$.data.records[0].material.label").value("螺丝 M3"));
    }

    @Test
    void manyToOneAcceptsObjectFormWhenWriting() throws Exception {
        importSample();
        long material = create("material", "{\"name\":\"螺丝 M3\"}");
        long product = create("product", "{\"name\":\"电动螺丝刀\"}");

        send(post("/api/data/product_bom"),
                "{\"product\":{\"id\":" + product + ",\"label\":\"电动螺丝刀\"},"
                        + "\"material\":{\"id\":" + material + ",\"label\":\"螺丝 M3\"},\"quantity\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.material.label").value("螺丝 M3"));
    }

    // ---------- 写入校验 ----------

    @Test
    void unknownMaterialIdIsReportedOnThatField() throws Exception {
        importSample();
        long product = create("product", "{\"name\":\"电动螺丝刀\"}");

        send(post("/api/data/product_bom"),
                "{\"product\":" + product + ",\"material\":999999,\"quantity\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data.fieldErrors.material").value(containsString("不存在")));
    }

    @Test
    void unknownProductIdIsReportedOnThatField() throws Exception {
        importSample();
        long material = create("material", "{\"name\":\"螺丝 M3\"}");

        send(post("/api/data/product_bom"),
                "{\"product\":999999,\"material\":" + material + ",\"quantity\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.fieldErrors.product").value(containsString("不存在")));
    }

    // ---------- 查询和一对多 ----------

    @Test
    void filterByManyToOneReturnsOnlyThatProductLines() throws Exception {
        importSample();
        long material = create("material", "{\"name\":\"螺丝 M3\"}");
        long p1 = create("product", "{\"name\":\"产品一\"}");
        long p2 = create("product", "{\"name\":\"产品二\"}");
        long b1 = create("product_bom", "{\"product\":" + p1 + ",\"material\":" + material + "}");
        create("product_bom", "{\"product\":" + p2 + ",\"material\":" + material + "}");

        // 这正是一对多标签页用的查询：按 mappedBy 字段等于当前产品 id
        send(post("/api/data/product_bom/search"),
                "{\"filters\":[{\"field\":\"product\",\"op\":\"eq\",\"value\":" + p1 + "}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(b1))
                .andExpect(jsonPath("$.data.records[0].material.label").value("螺丝 M3"));
    }

    // ---------- options ----------

    @Test
    void optionsSearchesByDisplayField() throws Exception {
        importSample();
        create("material", "{\"code\":\"MAT-1\",\"name\":\"螺丝 M3\"}");
        create("material", "{\"code\":\"MAT-2\",\"name\":\"螺丝 M4\"}");
        create("material", "{\"code\":\"MAT-3\",\"name\":\"扳手\"}");

        mvc.perform(get("/api/data/material/options")
                        .param("keyword", "螺丝")
                        .param("displayField", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].value").isNumber())
                .andExpect(jsonPath("$.data[0].label").value("螺丝 M3"));
    }

    @Test
    void optionsUsesFirstStringFieldWhenDisplayFieldIsMissing() throws Exception {
        importSample();
        create("material", "{\"code\":\"MAT-1\",\"name\":\"螺丝 M3\"}");

        // material 的第一个 string 字段是 code
        mvc.perform(get("/api/data/material/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("MAT-1"));
    }

    @Test
    void optionsRejectsUnknownDisplayField() throws Exception {
        importSample();

        mvc.perform(get("/api/data/material/options").param("displayField", "nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("显示字段")));
    }

    @Test
    void optionsLimitHasBounds() throws Exception {
        importSample();

        mvc.perform(get("/api/data/material/options").param("limit", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/data/material/options").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void optionsOnUnknownModelReturnsNotFound() throws Exception {
        mvc.perform(get("/api/data/nope/options")).andExpect(status().isNotFound());
    }

    // ---------- 删除引用保护 ----------

    @Test
    void deletingReferencedMaterialReturnsConflictWithReferrer() throws Exception {
        importSample();
        long material = create("material", "{\"name\":\"螺丝 M3\"}");
        long product = create("product", "{\"name\":\"电动螺丝刀\"}");
        long bom = create("product_bom", "{\"product\":" + product + ",\"material\":" + material + "}");

        mvc.perform(delete("/api/data/material/" + material))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value(containsString("产品 BOM")));

        // 删掉引用后可以删
        mvc.perform(delete("/api/data/product_bom/" + bom)).andExpect(status().isOk());
        mvc.perform(delete("/api/data/material/" + material)).andExpect(status().isOk());
    }

    @Test
    void deletingUnreferencedRecordIsAllowed() throws Exception {
        importSample();
        long material = create("material", "{\"name\":\"螺丝 M3\"}");

        mvc.perform(delete("/api/data/material/" + material)).andExpect(status().isOk());
        mvc.perform(get("/api/data/material/" + material)).andExpect(status().isNotFound());
    }
}
