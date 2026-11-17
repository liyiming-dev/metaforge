package com.metaforge.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.metaforge.meta.model.FieldTypes;
import com.metaforge.schema.types.BooleanType;
import com.metaforge.schema.types.DateTimeType;
import com.metaforge.schema.types.DateType;
import com.metaforge.schema.types.DecimalType;
import com.metaforge.schema.types.EnumType;
import com.metaforge.schema.types.IntegerType;
import com.metaforge.schema.types.ManyToOneType;
import com.metaforge.schema.types.OneToManyType;
import com.metaforge.schema.types.StringType;
import com.metaforge.schema.types.TextType;

/** 字段类型的注册表：按类型名找到对应的处理器。新增字段类型时在这里登记一行。 */
@Component
public class FieldTypeRegistry {

    private final Map<String, FieldTypeHandler> handlers = new HashMap<>();

    public FieldTypeRegistry() {
        List<FieldTypeHandler> builtIn = List.of(
                new StringType(),
                new TextType(),
                new IntegerType(),
                new DecimalType(),
                new BooleanType(),
                new DateType(),
                new DateTimeType(),
                new EnumType(),
                new ManyToOneType(),
                new OneToManyType());
        for (FieldTypeHandler handler : builtIn) {
            handlers.put(handler.type(), handler);
        }
        // 启动时就检查：契约里列出的每种字段类型都必须有处理器，漏了就直接启动失败
        for (String type : FieldTypes.ALL) {
            if (!handlers.containsKey(type)) {
                throw new IllegalStateException("字段类型 " + type + " 没有登记处理器");
            }
        }
    }

    public FieldTypeHandler get(String type) {
        FieldTypeHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalStateException("字段类型 " + type + " 没有登记处理器");
        }
        return handler;
    }
}
