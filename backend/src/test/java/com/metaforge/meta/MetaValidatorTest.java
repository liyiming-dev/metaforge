package com.metaforge.meta;

import static com.metaforge.meta.SampleMeta.changed;
import static com.metaforge.meta.SampleMeta.context;
import static com.metaforge.meta.SampleMeta.field;
import static com.metaforge.meta.SampleMeta.indexOf;
import static com.metaforge.meta.SampleMeta.model;
import static com.metaforge.meta.SampleMeta.removeValue;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.metaforge.common.BizException;
import com.metaforge.common.ErrorCode;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ModuleDef;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class MetaValidatorTest {

    /** 断言：保存这个定义会被拒绝，并且提示里包含给定的文字。 */
    private static void assertRejected(ModelDef model, ModelDef old, String messagePart) {
        assertThatThrownBy(() -> MetaValidator.validateModel(model, old, context()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
                .hasMessageContaining(messagePart);
    }

    @Test
    void acceptsEveryModelInTheContractSample() {
        for (ModelDef sample : SampleMeta.bundle().models()) {
            assertThatCode(() -> MetaValidator.validateModel(sample, null, context()))
                    .as("示例里的模型 %s 应该能通过校验", sample.name())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsModelNameThatBreaksTheNamingRule() {
        assertRejected(changed("material", n -> n.put("name", "Bad Name")), null, "不合规则");
    }

    @Test
    void rejectsModuleThatDoesNotExist() {
        assertRejected(changed("material", n -> n.put("module", "nope")), null, "所属的模块 nope 不存在");
    }

    @Test
    void rejectsDuplicateFieldNames() {
        ModelDef model = changed("material", n -> {
            ArrayNode fields = (ArrayNode) n.get("fields");
            fields.add(fields.get(0).deepCopy());
        });
        assertRejected(model, null, "字段标识 code 重复");
    }

    @Test
    void rejectsFieldNamedLikeASystemField() {
        assertRejected(changed("material", n -> field(n, "code").put("name", "id")), null, "是系统字段");
    }

    @Test
    void rejectsUnknownFieldType() {
        assertRejected(changed("material", n -> field(n, "spec").put("type", "image")), null, "类型 image 不支持");
    }

    @Test
    void rejectsEnumWithoutOptions() {
        assertRejected(changed("material", n -> field(n, "category").remove("options")), null, "至少要有一个选项");
    }

    @Test
    void rejectsUniqueOnTextField() {
        assertRejected(changed("material", n -> field(n, "remark").put("unique", true)), null, "不能设为唯一");
    }

    @Test
    void rejectsRelationToMissingModel() {
        ModelDef model = changed("product_bom",
                n -> ((ObjectNode) field(n, "material").get("relation")).put("model", "nope"));
        assertRejected(model, null, "关联的模型 nope 不存在");
    }

    @Test
    void rejectsOneToManyWhoseMappedByDoesNotPointBack() {
        // product_bom.material 指向的是物料，不是产品，所以不能当产品 BOM 明细的回指字段
        ModelDef model = changed("product",
                n -> ((ObjectNode) field(n, "bom_lines").get("relation")).put("mappedBy", "material"));
        assertRejected(model, null, "mappedBy 必须是");
    }

    @Test
    void rejectsViewColumnThatIsNotAField() {
        ModelDef model = changed("material",
                n -> ((ArrayNode) n.get("views").get("list").get("columns")).add("nope"));
        assertRejected(model, null, "nope 不是这个模型的字段");
    }

    @Test
    void rejectsFormTabThatIsNotOneToMany() {
        ModelDef model = changed("product",
                n -> ((ObjectNode) n.get("views").get("form").get("tabs").get(0)).put("field", "name"));
        assertRejected(model, null, "必须对应一个一对多字段");
    }

    @Test
    void rejectsServiceWithUnknownTrigger() {
        ModelDef model = changed("code_rule",
                n -> ((ObjectNode) n.get("services").get(0)).put("trigger", "onClick"));
        assertRejected(model, null, "触发时机 onClick 不支持");
    }

    @Test
    void rejectsChangingTheTypeOfAnExistingField() {
        ModelDef changed = changed("material", n -> field(n, "spec").put("type", "text"));
        assertRejected(changed, model("material"), "不能修改字段 spec 的类型");
    }

    @Test
    void rejectsRemovingAFieldThatAnotherModelStillUses() {
        // 产品 BOM 的“物料”字段用物料的 name 当显示文字，所以物料的 name 不能删
        ModelDef withoutName = changed("material", n -> {
            ArrayNode fields = (ArrayNode) n.get("fields");
            fields.remove(indexOf(fields, "name", "name"));
            removeValue((ArrayNode) n.get("views").get("list").get("columns"), "name");
            removeValue((ArrayNode) n.get("views").get("list").get("searchFields"), "name");
            removeValue((ArrayNode) n.get("views").get("form").get("groups").get(0).get("fields"), "name");
        });
        assertRejected(withoutName, model("material"), "模型 product_bom 的字段 material 还在引用它");
    }

    @Test
    void allowsAddingAFieldOnUpdate() {
        ModelDef withBrand = changed("material", n -> {
            ObjectNode brand = ((ArrayNode) n.get("fields")).addObject();
            brand.put("name", "brand").put("displayName", "品牌").put("type", "string");
        });
        assertThatCode(() -> MetaValidator.validateModel(withBrand, model("material"), context()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsModuleNameThatBreaksTheNamingRule() {
        assertThatThrownBy(() -> MetaValidator.validateModule(new ModuleDef("Bad", "坏名字", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不合规则");
    }
}
