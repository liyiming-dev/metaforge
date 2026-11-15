package com.metaforge.meta;

import static com.metaforge.meta.SampleMeta.changed;
import static com.metaforge.meta.SampleMeta.model;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.metaforge.meta.model.FormTabDef;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;
import com.metaforge.meta.model.ServiceDef;
import com.metaforge.meta.model.SortItem;

class MetaDefaultsTest {

    @Test
    void generatesDefaultViewsWhenNoneAreGiven() {
        ModelDef product = MetaDefaults.normalize(changed("product", n -> n.remove("views")));

        // 一对多字段 bom_lines 不出现在列表列和表单字段里，而是变成一个子表标签页
        assertThat(product.views().list().columns()).containsExactly("code", "name", "spec", "launch_date");
        assertThat(product.views().list().searchFields()).containsExactly("code", "name", "spec");
        assertThat(product.views().list().defaultSort()).isEqualTo(new SortItem("created_at", "desc"));
        assertThat(product.views().list().pageSize()).isEqualTo(10);

        assertThat(product.views().form().groups()).hasSize(1);
        assertThat(product.views().form().groups().get(0).title()).isEqualTo("基本信息");
        assertThat(product.views().form().groups().get(0).columns()).isEqualTo(2);
        assertThat(product.views().form().groups().get(0).fields())
                .containsExactly("code", "name", "spec", "launch_date");
        assertThat(product.views().form().tabs()).containsExactly(new FormTabDef("BOM 明细", "bom_lines"));
    }

    @Test
    void defaultViewsKeepTextFieldsOutOfTheListButInTheForm() {
        ModelDef material = MetaDefaults.normalize(changed("material", n -> n.remove("views")));

        assertThat(material.views().list().columns()).doesNotContain("remark");
        assertThat(material.views().form().groups().get(0).fields()).contains("remark");
        // 搜索字段只取前 3 个文本或枚举字段
        assertThat(material.views().list().searchFields()).containsExactly("code", "name", "spec");
    }

    @Test
    void keepsViewsThatAreGiven() {
        ModelDef material = MetaDefaults.normalize(model("material"));

        assertThat(material.views().list().columns())
                .containsExactlyElementsOf(model("material").views().list().columns());
        assertThat(material.views().form().groups()).hasSize(2);
    }

    @Test
    void fillsModelLevelDefaults() {
        ModelDef bare = MetaDefaults.normalize(changed("material", n -> {
            n.remove("showInMenu");
            n.remove("sort");
            n.remove("services");
        }));

        assertThat(bare.showInMenu()).isTrue();
        assertThat(bare.sort()).isEqualTo(100);
        assertThat(bare.services()).isEmpty();
        assertThat(bare.tableName()).isEqualTo("md_material");
    }

    @Test
    void fillsServiceDefaults() {
        ServiceDef manual = MetaDefaults.normalize(changed("code_rule",
                n -> ((tools.jackson.databind.node.ObjectNode) n.get("services").get(0)).remove("scope")))
                .services().get(0);
        // 手动服务没写范围时，默认针对一条记录
        assertThat(manual.scope()).isEqualTo("record");
        assertThat(manual.enabled()).isTrue();

        ServiceDef hook = MetaDefaults.normalize(model("material")).services().get(0);
        // 钩子服务没有“范围”这个概念
        assertThat(hook.scope()).isNull();
        assertThat(hook.enabled()).isTrue();
    }

    @Test
    void fillsModuleSort() {
        assertThat(MetaDefaults.normalize(new ModuleDef("sales", "销售管理", null, null)).sort()).isEqualTo(100);
        assertThat(MetaDefaults.normalize(new ModuleDef("mes", "生产管理", null, 10)).sort()).isEqualTo(10);
    }
}
