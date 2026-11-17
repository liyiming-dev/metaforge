package com.metaforge.data;

import org.springframework.stereotype.Component;

import com.metaforge.meta.model.FieldDef;
import com.metaforge.meta.model.FieldTypes;
import com.metaforge.meta.model.SystemFields;
import com.metaforge.schema.FieldTypeRegistry;

/**
 * 记录里的值在“请求的 JSON”和“数据库的值”之间转换。具体怎么转交给每种字段类型的处理器，
 * 这里只负责按类型找到处理器，外加给系统字段拼一个假的字段定义。
 *
 * 有了它，DataService 里不需要出现“这个是日期就转成 LocalDate”这类判断，
 * 加一种新字段类型时也不用改这里。
 */
@Component
public class FieldValues {

    private final FieldTypeRegistry types;

    public FieldValues(FieldTypeRegistry types) {
        this.types = types;
    }

    /** 请求里的值 → 数据库里的值。值不合法时抛 BizException，消息是给用户看的。 */
    public Object toColumn(FieldDef field, Object value) {
        return types.get(field.type()).toColumn(field, value);
    }

    /** 数据库里的值 → 响应里的值。 */
    public Object toJson(FieldDef field, Object value) {
        return types.get(field.type()).toJson(field, value);
    }

    /**
     * 系统字段也能当过滤和排序的条件，所以要有一个能交给处理器用的字段定义。
     * id 是整数，created_at、updated_at 是日期时间。不是系统字段时返回 null。
     */
    public static FieldDef systemField(String name) {
        if (SystemFields.ID.equals(name)) {
            return field(name, FieldTypes.INTEGER);
        }
        if (SystemFields.CREATED_AT.equals(name) || SystemFields.UPDATED_AT.equals(name)) {
            return field(name, FieldTypes.DATETIME);
        }
        return null;
    }

    private static FieldDef field(String name, String type) {
        return new FieldDef(name, name, type, null, null, null, null, null, null, null, null, null);
    }
}
