package com.metaforge.svc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import tools.jackson.databind.JsonNode;
import com.metaforge.TestTables;
import com.metaforge.meta.MetaRegistry;
import com.metaforge.meta.SampleMeta;

/** B5 的服务引擎、钩子、编码规则、错误回滚和 20 并发。 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceApiTest {

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

    private JsonNode callService(String model, String service, Long id) throws Exception {
        String body = id == null ? "{}" : "{\"id\":" + id + "}";
        String response = mvc.perform(post("/api/svc/" + model + "/" + service)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return SampleMeta.JSON.readTree(response).get("data");
    }

    private long total(String model) throws Exception {
        String response = send(post("/api/data/" + model + "/search"), "{}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return SampleMeta.JSON.readTree(response).get("data").get("total").asLong();
    }

    @Test
    void generateIncrementsCurrentSerialAndReturnsCode() throws Exception {
        importSample();
        long rule = create("code_rule",
                "{\"name\":\"manual\",\"prefix\":\"T-\",\"date_format\":\"yyyyMMdd\",\"serial_digits\":4}");

        String first = callService("code_rule", "generate", rule).asText();
        String second = callService("code_rule", "generate", rule).asText();

        assertThat(first).startsWith("T-").endsWith("0001");
        assertThat(second).startsWith("T-").endsWith("0002");
        assertThat(first).isNotEqualTo(second);

        String ruleJson = mvc.perform(get("/api/data/code_rule/" + rule))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(SampleMeta.JSON.readTree(ruleJson).get("data").get("current_serial").asLong()).isEqualTo(2);
    }

    @Test
    void materialCreateFillsCodeButExplicitCodeWins() throws Exception {
        importSample();

        String generated = mvc.perform(post("/api/data/material")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"自动编码物料\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(SampleMeta.JSON.readTree(generated).get("data").get("code").asText()).startsWith("MAT-");

        send(post("/api/data/material"), "{\"name\":\"手工编码物料\",\"code\":\"CUSTOM-1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CUSTOM-1"));
    }

    @Test
    void nonManualServiceReturnsNotFound() throws Exception {
        importSample();
        long material = create("material", "{\"name\":\"螺丝\"}");

        mvc.perform(post("/api/svc/material/fill_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + material + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void scriptErrorRollsBackCreate() throws Exception {
        importSample();
        send(post("/api/meta/models"), """
                {
                  "name": "bad_script",
                  "displayName": "坏脚本模型",
                  "module": "mes",
                  "fields": [{"name":"name","displayName":"名称","type":"string","required":true}],
                  "services": [{"name":"boom","displayName":"爆炸","trigger":"beforeCreate",
                    "script":"throw \\"脚本故意失败\\";"}]
                }
                """).andExpect(status().isOk());

        send(post("/api/data/bad_script"), "{\"name\":\"不会写进去\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("脚本故意失败")));

        assertThat(total("bad_script")).isZero();
    }

    @Test
    void changingScriptTakesEffectWithoutRestart() throws Exception {
        importSample();
        send(post("/api/meta/models"), """
                {
                  "name": "echo_service",
                  "displayName": "回显服务",
                  "module": "mes",
                  "fields": [{"name":"name","displayName":"名称","type":"string"}],
                  "services": [{"name":"echo","displayName":"回显","trigger":"manual","scope":"model",
                    "script":"return \\"one\\";"}]
                }
                """).andExpect(status().isOk());
        assertThat(callService("echo_service", "echo", null).asText()).isEqualTo("one");

        send(put("/api/meta/models/echo_service"), """
                {
                  "name": "echo_service",
                  "displayName": "回显服务",
                  "module": "mes",
                  "fields": [{"name":"name","displayName":"名称","type":"string"}],
                  "services": [{"name":"echo","displayName":"回显","trigger":"manual","scope":"model",
                    "script":"return \\"two\\";"}]
                }
                """).andExpect(status().isOk());

        assertThat(callService("echo_service", "echo", null).asText()).isEqualTo("two");
    }

    @Test
    void scriptCanReadById() throws Exception {
        importSample();
        long rule = create("code_rule",
                "{\"name\":\"lookup\",\"prefix\":\"L-\",\"date_format\":\"none\",\"serial_digits\":4}");
        send(post("/api/meta/models"), """
                {
                  "name": "lookup_script",
                  "displayName": "读取脚本",
                  "module": "mes",
                  "fields": [{"name":"name","displayName":"名称","type":"string"}],
                  "services": [{"name":"lookup","displayName":"读取","trigger":"manual","scope":"model",
                    "script":"let row = db.get(\\"code_rule\\", params.id); if (row == nil) { return \\"nil\\"; } return row.name;"}]
                }
                """).andExpect(status().isOk());

        String response = mvc.perform(post("/api/svc/lookup_script/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"params\":{\"id\":" + rule + "}}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(SampleMeta.JSON.readTree(response).get("data").asText()).isEqualTo("lookup");
    }

    @Test
    void scriptCanUpdateThroughControlledDb() throws Exception {
        importSample();
        send(post("/api/meta/models"), """
                {
                  "name": "update_script",
                  "displayName": "更新脚本",
                  "module": "mes",
                  "fields": [{"name":"name","displayName":"名称","type":"string","required":true}],
                  "services": [{"name":"rename","displayName":"改名","trigger":"manual","scope":"model",
                    "script":"return db.update(\\"update_script\\", params.id, params.values);"}]
                }
                """).andExpect(status().isOk());
        long id = create("update_script", "{\"name\":\"before\"}");

        String response = mvc.perform(post("/api/svc/update_script/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"params\":{\"id\":" + id + ",\"values\":{\"name\":\"after\"}}}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(SampleMeta.JSON.readTree(response).get("data").get("name").asText()).isEqualTo("after");
        assertThat(SampleMeta.JSON.readTree(response).get("data").get("id").asLong()).isEqualTo(id);
    }

    @Test
    void beforeDeleteCanBlockDelete() throws Exception {
        importSample();
        send(post("/api/meta/models"), """
                {
                  "name": "delete_guard",
                  "displayName": "删除保护",
                  "module": "mes",
                  "fields": [{"name":"name","displayName":"名称","type":"string","required":true}],
                  "services": [{"name":"guard","displayName":"禁止删除","trigger":"beforeDelete",
                    "script":"throw \\"这条记录不能删\\";"}]
                }
                """).andExpect(status().isOk());
        long id = create("delete_guard", "{\"name\":\"保留\"}");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/data/delete_guard/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("这条记录不能删")));
        assertThat(total("delete_guard")).isEqualTo(1);
    }

    @Test
    void twentyConcurrentCreatesGetUniqueCodes() throws Exception {
        importSample();
        int count = 20;
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        Set<String> codes = Collections.synchronizedSet(new HashSet<>());
        List<String> failures = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int i = 0; i < count; i++) {
                int index = i;
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await(10, TimeUnit.SECONDS);
                        String response = mvc.perform(post("/api/data/material")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"并发物料" + index + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
                        String code = SampleMeta.JSON.readTree(response).get("data").get("code").asText();
                        codes.add(code);
                    } catch (Exception e) {
                        failures.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(failures).isEmpty();
        assertThat(codes).hasSize(count);
        assertThat(total("material")).isEqualTo(count);
    }
}
