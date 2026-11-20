# 元模型格式

元模型是“描述业务模型的数据”。开发者在设计器里定义它，平台读它来建表、提供接口、渲染页面。课题要求的“定义模型、属性、服务、视图”，对应下面的 `ModelDef`、`FieldDef`、`ServiceDef`、`ViewsDef`。

和课堂上的 IIDP 对照：

| 这里的元模型 | IIDP 里的写法 |
|---|---|
| `ModelDef` 的 `name`、`displayName`、`description` | `@Model(name = ..., displayName = ..., description = ...)` |
| `FieldDef` | `@Property(displayName = ...)` |
| `type: "manyToOne"` | `@ManyToOne` 加 `@JoinColumn` |
| `ServiceDef` | 模型类里写的服务方法 |
| `ViewsDef` | 视图 JSON |

区别在于：IIDP 把这些信息写在 Java 注解里，编译后才生效；这里把它们存成数据，运行时就能改。

完整例子见 [samples/mes.json](samples/mes.json)。

## 模块 ModuleDef

模块用来给模型分组，对应左侧菜单的一级目录。

| 键 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 标识 |
| `displayName` | string | 是 | 显示名 |
| `icon` | string | 否 | Ant Design 图标的组件名，例如 `AppstoreOutlined` |
| `sort` | number | 否 | 菜单排序，小的在前，默认 100 |

## 模型 ModelDef

| 键 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 标识 |
| `displayName` | string | 是 | 显示名 |
| `description` | string | 否 | 说明 |
| `module` | string | 是 | 所属模块的 `name` |
| `showInMenu` | boolean | 否 | 是否出现在菜单里，默认 `true`。只作为子表使用的模型设为 `false` |
| `sort` | number | 否 | 在模块内的排序，默认 100 |
| `fields` | FieldDef[] | 是 | 至少一个字段 |
| `services` | ServiceDef[] | 否 | 默认空数组 |
| `views` | ViewsDef | 否 | 请求里可以不传，后端会生成默认视图。**后端返回的模型定义里 `views` 一定是完整的** |
| `tableName` | string | 只读 | 后端生成的表名，请求里传了也会被忽略 |

后端返回模型定义时，会把 `showInMenu`、`sort`、`services`、`views` 这些有默认值的键都补齐，前端不用自己处理缺省。

每个模型自动带三个系统字段，不用也不能在 `fields` 里定义：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `id` | integer | 主键，自增 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 最后修改时间 |

## 字段 FieldDef

| 键 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 标识，不能和系统字段重名 |
| `displayName` | string | 是 | 显示名 |
| `type` | string | 是 | 字段类型，见下表 |
| `required` | boolean | 否 | 必填，默认 `false` |
| `unique` | boolean | 否 | 值不能重复，默认 `false` |
| `readonly` | boolean | 否 | 表单里只读，默认 `false`。用于由服务自动填写的字段，例如自动生成的编码 |
| `defaultValue` | 与类型对应 | 否 | 新建记录时的默认值 |
| `length` | number | 否 | 只用于 `string`，默认 255 |
| `precision`、`scale` | number | 否 | 只用于 `decimal`，默认 18 和 4 |
| `options` | `{ value, label }[]` | `enum` 必填 | 枚举的选项 |
| `relation` | RelationDef | 关联类型必填 | 见下面的“关联” |

### 字段类型

| `type` | 含义 | 记录里的 JSON 值 | 默认的表单组件 |
|---|---|---|---|
| `string` | 单行文本 | 字符串 | 输入框 |
| `text` | 多行文本 | 字符串 | 多行输入框 |
| `integer` | 整数 | 数字 | 数字输入框 |
| `decimal` | 小数 | 数字 | 数字输入框 |
| `boolean` | 是或否 | `true` / `false` | 开关 |
| `date` | 日期 | `"2026-11-14"` | 日期选择 |
| `datetime` | 日期时间 | `"2026-11-14 09:30:00"` | 日期时间选择 |
| `enum` | 单选 | 选项的 `value`（字符串） | 下拉框 |
| `manyToOne` | 多对一 | 响应里是 `{ "id": 3, "label": "螺丝 M3" }`，请求里传 `3` | 可搜索的下拉框 |
| `oneToMany` | 一对多 | 不出现在记录里 | 表单里的子表标签页 |

没有值的字段在响应里是 `null`。

### 关联 RelationDef

多对一，例如“产品 BOM”里的“物料”字段：

```json
{ "name": "material", "displayName": "物料", "type": "manyToOne", "required": true,
  "relation": { "model": "material", "displayField": "name" } }
```

- `model`：指向哪个模型。
- `displayField`：用目标模型的哪个字段当显示文字，也就是响应里的 `label`。
- 请求里传目标记录的 `id`。为了让前端能把读到的记录改完直接传回来，后端也接受 `{ "id": 3, "label": "..." }` 这种形式，只取其中的 `id`。

一对多，例如“产品”里的“BOM 明细”字段：

```json
{ "name": "bom_lines", "displayName": "BOM 明细", "type": "oneToMany",
  "relation": { "model": "product_bom", "mappedBy": "product" } }
```

- `model`：子表模型。
- `mappedBy`：子表模型里指回本模型的那个 `manyToOne` 字段名。
- 一对多字段不占数据库列，也不出现在记录里。前端要显示子表时，对子表模型调用查询接口，条件是 `mappedBy` 字段等于当前记录的 `id`。子表记录的增删改直接调子表模型的数据接口。

## 服务 ServiceDef

增删改查是每个模型自带的内置服务，不用定义。这里定义的是开发者自己写的脚本服务。

| 键 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 标识 |
| `displayName` | string | 是 | 显示名，手动服务用它当按钮文字 |
| `trigger` | string | 是 | 什么时候执行，见下表 |
| `scope` | string | 否 | 只用于 `manual`：`record` 针对一条记录，`model` 不针对具体记录。默认 `record` |
| `script` | string | 是 | 脚本内容 |
| `enabled` | boolean | 否 | 默认 `true` |

| `trigger` | 执行时机 |
|---|---|
| `manual` | 用户点按钮，或者调用服务接口 `/api/svc/{model}/{service}` |
| `beforeCreate` | 新建记录、写入数据库之前。脚本可以修改将要保存的记录 |
| `afterCreate` | 新建记录、写入数据库之后 |
| `beforeUpdate` | 修改记录、写入数据库之前 |
| `afterUpdate` | 修改记录、写入数据库之后 |
| `beforeDelete` | 删除记录之前。脚本可以阻止删除 |

前端的显示规则：`manual` 且 `scope` 为 `record` 的服务，显示成列表每一行操作列里的按钮；`scope` 为 `model` 的显示成列表上方工具栏的按钮。其它触发方式的服务不显示按钮。

脚本语言是 **AviatorScript 5.4.4**，由后端开启 sandbox 后执行。脚本不能直接访问 `JdbcTemplate`、JDBC、文件系统或任意 Java 类，只能使用下面列出的变量和 `db` 函数；表名、列名仍然由后端从模型定义取，值一律参数绑定。

### 脚本变量

| 变量 | 类型 | 说明 |
|---|---|---|
| `record` | map | 当前记录。`manual` 且 `scope` 为 `record` 时是选中的那条记录；`scope` 为 `model` 时是空 map；`beforeCreate`/`beforeUpdate` 时可以修改，修改后的值会写进数据库；`afterCreate`/`afterUpdate`/`beforeDelete` 时是已保存/待删除记录的只读视图 |
| `params` | map | 服务接口传入的 `params`；生命周期钩子没有参数，是空 map |

`record` 里是字段名到值的映射；多对一字段的值是目标记录的 `id`，一对多字段不在 `record` 里。系统字段（`id`、`created_at`、`updated_at`）可以读，但不能通过脚本修改。

### `db` 函数

| 函数 | 返回 | 说明 |
|---|---|---|
| `db.get(model, id)` | map 或 `nil` | 按 id 读一条记录 |
| `db.find(model, field, value)` | map 或 `nil` | 按一个字段的精确值找第一条记录；`field` 必须是该模型已定义的字段 |
| `db.increment(model, id, field)` | number | 把整数字段原子加 1，返回加完后的值。并发生成编码必须用它，不能先查再写 |
| `db.today(pattern)` | string | 按 Java 日期格式返回服务器当天日期，例如 `db.today("yyyyMMdd")` |
| `db.call(model, service, id, params)` | 任意 JSON 值 | 调用另一个 `manual` 服务的脚本，返回它的返回值；`id` 和 `params` 可以传 `nil`，只传三个参数时等价于 `params` 为 `{}` |

`db.call` 只能调用 `manual` 服务，调用链不能形成环；后端检测到递归会报错。服务调用和当前操作在同一个事务里，被调服务出错时整个操作回滚。

### 错误、超时和死循环

- 脚本语法错误、运行时错误、`throw` 出来的错误：服务接口返回 400，`message` 说明是哪条服务、什么原因；生命周期钩子出错时，这次新增/修改/删除整体回滚。
- 后端给脚本设置执行时间上限和循环次数上限；超时或超限按脚本错误处理，不会让请求一直挂住。
- `beforeDelete` 里 `throw` 可以阻止删除；其它钩子的返回值被忽略，要修改记录请改 `record`。

### 示例：编码规则的 `generate`

```javascript
let serial = db.increment("code_rule", record.id, "current_serial");
let digits = "" + serial;
while (string.length(digits) < record.serial_digits) {
  digits = "0" + digits;
}
return record.prefix + db.today(record.date_format) + digits;
```

物料和产品的新建钩子见 `samples/mes.json`：用 `db.find` 找到对应编码规则，再用 `db.call` 调它的 `generate`，把返回值填进 `record.code`。

## 视图 ViewsDef

```json
{
  "list": {
    "columns": ["code", "name", "category"],
    "searchFields": ["code", "name"],
    "defaultSort": { "field": "created_at", "order": "desc" },
    "pageSize": 10
  },
  "form": {
    "groups": [
      { "title": "基本信息", "columns": 2, "fields": ["code", "name", "category"] }
    ],
    "tabs": [
      { "title": "BOM 明细", "field": "bom_lines" }
    ]
  }
}
```

| 键 | 说明 |
|---|---|
| `list.columns` | 列表显示哪些字段，按数组顺序排列 |
| `list.searchFields` | 搜索区有哪些字段 |
| `list.defaultSort` | 默认排序，`order` 是 `asc` 或 `desc` |
| `list.pageSize` | 每页条数，默认 10 |
| `form.groups` | 表单分组。`columns` 是每行放几个字段（1 到 4），`fields` 是这一组里的字段 |
| `form.tabs` | 表单下方的子表标签页，`field` 必须是一个 `oneToMany` 字段 |

视图里出现的字段名都必须是这个模型已有的字段，系统字段也可以用。

### 默认视图

模型定义里没有传 `views` 时，后端按下面的规则生成：

- `list.columns`：除 `text` 和 `oneToMany` 以外的所有字段，按定义顺序。
- `list.searchFields`：前 3 个 `string` 或 `enum` 字段。
- `list.defaultSort`：按 `created_at` 倒序。
- `form.groups`：一个标题为“基本信息”的分组，每行 2 个，包含除 `oneToMany` 以外的所有字段。
- `form.tabs`：每个 `oneToMany` 字段一个标签页，标题用字段的显示名。

## 元模型包 MetaBundle

导入、导出和 AI 助手都用这个格式，一次带上若干模块和模型：

```json
{ "modules": [ /* ModuleDef */ ], "models": [ /* ModelDef */ ] }
```
