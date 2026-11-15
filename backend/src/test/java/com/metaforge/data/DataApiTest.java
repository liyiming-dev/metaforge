package com.metaforge.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.metaforge.TestTables;
import com.metaforge.meta.MetaRegistry;
import com.metaforge.meta.SampleMeta;

/**
 * 从接口这一层测 /api/data/{model}：请求进来，经过校验、拼 SQL、写库，再读回来。
 * 用内存数据库。和 MySQL 有关的差异（比如约束名的格式）在真实 MySQL 上再验一遍。
 */
@SpringBootTest
@ActiveProfiles("test")
class DataApiTest {

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
    }

    private long create(String model, String body) throws Exception {
        String response = send(post("/api/data/" + model), body)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return SampleMeta.JSON.readTree(response).get("data").get("id").asLong();
    }

    private ResultActions search(String model, String body) throws Exception {
        return send(post("/api/data/" + model + "/search"), body);
    }

    // ---------- 新建 ----------

    @Test
    void createsRecordWithDefaultsAndSystemFields() throws Exception {
        importSample();

        send(post("/api/data/material"), "{\"name\":\"螺丝 M3\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("螺丝 M3"))
                // 没传的字段取定义里的默认值
                .andExpect(jsonPath("$.data.category").value("raw"))
                .andExpect(jsonPath("$.data.unit").value("个"))
                .andExpect(jsonPath("$.data.safety_stock").value(0))
                .andExpect(jsonPath("$.data.enabled").value(true))
                // 没有默认值的字段是 null
                .andExpect(jsonPath("$.data.code").value(nullValue()))
                // 系统字段由服务端写
                .andExpect(jsonPath("$.data.created_at").isNotEmpty())
                .andExpect(jsonPath("$.data.updated_at").isNotEmpty());
    }

    @Test
    void systemFieldsInRequestAreIgnored() throws Exception {
        importSample();

        send(post("/api/data/material"), "{\"name\":\"螺丝 M3\",\"id\":999,\"created_at\":\"2020-01-01\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(not(999)))
                .andExpect(jsonPath("$.data.created_at").value(not("2020-01-01 00:00:00")));
    }

    @Test
    void missingRequiredFieldIsReportedOnThatField() throws Exception {
        importSample();

        send(post("/api/data/material"), "{\"spec\":\"M3x10\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data.fieldErrors.name").value(containsString("请填写物料名称")));
    }

    @Test
    void unknownFieldInBodyIsRejected() throws Exception {
        importSample();

        send(post("/api/data/material"), "{\"name\":\"螺丝 M3\",\"nope\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("nope")));
    }

    @Test
    void enumValueOutsideOptionsIsRejected() throws Exception {
        importSample();

        send(post("/api/data/material"), "{\"name\":\"螺丝 M3\",\"category\":\"other\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.fieldErrors.category").value(containsString("不在选项里")));
    }

    @Test
    void wrongValueTypeIsRejected() throws Exception {
        importSample();

        send(post("/api/data/material"), "{\"name\":\"螺丝 M3\",\"safety_stock\":\"很多\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.fieldErrors.safety_stock").value(containsString("应为数字")));
    }

    @Test
    void duplicateUniqueValueIsReportedOnThatField() throws Exception {
        importSample();
        create("material", "{\"name\":\"螺丝 M3\",\"code\":\"MAT-0001\"}");

        send(post("/api/data/material"), "{\"name\":\"螺丝 M4\",\"code\":\"MAT-0001\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.data.fieldErrors.code").value(containsString("已经存在")));
    }

    // ---------- 修改和删除 ----------

    @Test
    void updateOnlyChangesGivenFieldsAndTouchesUpdatedAt() throws Exception {
        importSample();
        long id = create("material", "{\"code\":\"MAT-0001\",\"name\":\"螺丝 M3\",\"spec\":\"旧规格\"}");
        // 把修改时间改成很久以前，这样能确定地看出修改之后它变了
        jdbc.update("UPDATE `md_material` SET `updated_at` = '2020-01-01 00:00:00' WHERE `id` = ?", id);

        send(put("/api/data/material/" + id), "{\"spec\":\"新规格\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("MAT-0001"))
                .andExpect(jsonPath("$.data.name").value("螺丝 M3"))
                .andExpect(jsonPath("$.data.spec").value("新规格"))
                .andExpect(jsonPath("$.data.updated_at").value(not("2020-01-01 00:00:00")));
    }

    @Test
    void requiredFieldCannotBeClearedByUpdate() throws Exception {
        importSample();
        long id = create("material", "{\"name\":\"螺丝 M3\"}");

        send(put("/api/data/material/" + id), "{\"name\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.fieldErrors.name").exists());
    }

    @Test
    void deleteThenGetReturnsNotFound() throws Exception {
        importSample();
        long id = create("material", "{\"name\":\"螺丝 M3\"}");

        mvc.perform(get("/api/data/material/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
        mvc.perform(delete("/api/data/material/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/data/material/" + id)).andExpect(status().isNotFound());
    }

    // ---------- 查询 ----------

    @Test
    void searchFiltersSortsAndPaginates() throws Exception {
        importSample();
        for (int i = 1; i <= 12; i++) {
            create("material", "{\"name\":\"螺丝 " + i + "\",\"category\":\"raw\"}");
        }
        create("material", "{\"name\":\"扳手\",\"category\":\"aux\"}");

        search("material", """
                { "page": 2, "size": 5,
                  "filters": [ { "field": "name", "op": "like", "value": "螺丝" } ],
                  "sort": [ { "field": "name", "order": "asc" } ] }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.records", hasSize(5)));

        // in：只取两个类别里的
        search("material", """
                { "filters": [ { "field": "category", "op": "in", "value": ["raw", "aux"] } ] }
                """)
                .andExpect(jsonPath("$.data.total").value(13));
    }

    @Test
    void defaultSortIsUsedWhenNoneGiven() throws Exception {
        importSample();
        create("material", "{\"name\":\"第一条\"}");
        // 把第一条的创建时间改早，不然两条是同一秒建的，默认排序分不出先后
        jdbc.update("UPDATE `md_material` SET `created_at` = '2020-01-01 00:00:00' WHERE `name` = '第一条'");
        create("material", "{\"name\":\"第二条\"}");

        // 示例里的默认排序是 created_at 倒序，所以后建的应该在前
        search("material", "{}")
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].name").value("第二条"));
    }

    // ---------- 注入 ----------

    @Test
    void injectionInFilterFieldIsRejectedAndTableSurvives() throws Exception {
        importSample();
        create("material", "{\"name\":\"螺丝 M3\"}");

        search("material", """
                { "filters": [ { "field": "name; DROP TABLE md_material", "op": "eq", "value": "x" } ] }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        // 表还在，数据也还在
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `md_material`", Integer.class)).isEqualTo(1);
    }

    @Test
    void injectionInFilterValueIsTreatedAsPlainText() throws Exception {
        importSample();
        create("material", "{\"name\":\"螺丝 M3\"}");

        search("material", """
                { "filters": [ { "field": "name", "op": "eq", "value": "' OR '1'='1" } ] }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void injectionInSortOrderIsRejected() throws Exception {
        importSample();

        search("material", """
                { "sort": [ { "field": "name", "order": "desc; drop table md_material" } ] }
                """)
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `md_material`", Integer.class)).isZero();
    }

    // ---------- 模型不存在、新建模型立刻可用 ----------

    @Test
    void unknownModelReturnsNotFound() throws Exception {
        mvc.perform(get("/api/data/nope/1")).andExpect(status().isNotFound());
        search("nope", "{}").andExpect(status().isNotFound());
        send(post("/api/data/nope"), "{\"name\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    void newModelIsUsableWithoutRestart() throws Exception {
        importSample();

        send(post("/api/meta/models"), """
                { "name": "supplier", "displayName": "供应商", "module": "base",
                  "fields": [
                    { "name": "name", "displayName": "名称", "type": "string", "required": true },
                    { "name": "level", "displayName": "等级", "type": "enum",
                      "options": [ { "value": "A", "label": "A" } ] } ] }
                """).andExpect(status().isOk());

        // 后端没重启，这个模型的接口立刻能用
        send(post("/api/data/supplier"), "{\"name\":\"华东五金\",\"level\":\"A\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("华东五金"));

        search("supplier", "{}").andExpect(jsonPath("$.data.total").value(1));
    }

    // ---------- 各种类型的值 ----------

    @Test
    void dateAndDecimalValuesComeBackInContractFormat() throws Exception {
        importSample();
        long id = create("product", "{\"name\":\"电动螺丝刀\",\"launch_date\":\"2026-01-15\"}");

        mvc.perform(get("/api/data/product/" + id))
                .andExpect(jsonPath("$.data.launch_date").value("2026-01-15"));

        long materialId = create("material", "{\"name\":\"螺丝 M3\",\"safety_stock\":12.5,\"enabled\":false}");
        mvc.perform(get("/api/data/material/" + materialId))
                .andExpect(jsonPath("$.data.safety_stock").value(12.5))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }
}
