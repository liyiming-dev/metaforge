# 接口

响应格式、错误码、命名规则见 [README.md](README.md)。下面每个接口只写 `data` 里的内容。

| 分组 | 前缀 | 用途 | 谁在用 |
|---|---|---|---|
| 元模型接口 | `/api/meta` | 定义和读取元模型 | 设计器页面；通用页面读视图定义 |
| 数据接口 | `/api/data/{model}` | 任意模型的增删改查 | 通用列表页和表单页 |
| 服务接口 | `/api/svc/{model}/{service}` | 调用脚本服务 | 服务按钮 |
| AI 接口 | `/api/ai` | 根据描述生成元模型（加分项） | AI 助手页面 |
| 健康检查 | `/api/health` | 确认后端和数据库正常 | 首页 |

数据接口和服务接口里的 `{model}` 不是写死的：新建一个模型后，它的这几个接口立刻可用，不用重启。这就是课题要求的“动态生成 api”。

## 健康检查

### `GET /api/health`

```json
{ "status": "UP", "database": "MySQL 8.0.36", "time": "2026-11-14 09:00:00" }
```

## 元模型接口

### `GET /api/meta/menus`

返回菜单树。一级是模块，二级是模块里 `showInMenu` 为 `true` 的模型，都已经排好序。没有可显示模型的模块不返回。

```json
[
  {
    "key": "mes", "displayName": "生产管理", "icon": "AppstoreOutlined",
    "children": [
      { "key": "material", "displayName": "物料", "model": "material", "path": "/app/material" },
      { "key": "product", "displayName": "产品", "model": "product", "path": "/app/product" }
    ]
  }
]
```

前端按叶子节点的 `path` 和 `model` 动态注册路由。`path` 固定是 `/app/` 加模型名。

### `GET /api/meta/modules`

返回 `ModuleDef[]`，按 `sort` 排序。

### `PUT /api/meta/modules/{name}`

新建或修改模块，请求体是 `ModuleDef`，返回保存后的 `ModuleDef`。路径里的 `name` 和请求体里的 `name` 不一致时返回 400。

### `DELETE /api/meta/modules/{name}`

删除模块。模块下还有模型时返回 409。

### `GET /api/meta/models`

返回全部模型的完整定义 `ModelDef[]`。

### `GET /api/meta/models/{name}`

返回一个模型的完整定义 `ModelDef`。不存在时返回 404。

### `POST /api/meta/models`

新建模型。请求体是 `ModelDef`，返回保存后的 `ModelDef`。成功后数据表已经建好，数据接口立刻可用。

- 校验不通过返回 400。例如标识不合规则、字段重名、`enum` 没有 `options`、关联指向不存在的模型、视图里用了不存在的字段。
- 同名模型已存在返回 409。

### `PUT /api/meta/models/{name}`

修改模型。请求体是完整的 `ModelDef`，返回保存后的 `ModelDef`。

- 新增的字段立刻可用，已有数据在新字段上的值是默认值或 `null`。
- 从 `fields` 里去掉的字段不再显示，也不能再读写。数据库里的列和数据是否保留，由后端在决策记录 005 里确定。
- 不允许修改已有字段的 `type`，返回 400。

### `DELETE /api/meta/models/{name}`

删除模型。模型被其它模型的关联字段引用时返回 409。

### `GET /api/meta/export`

把全部模块和模型导出成一个 `MetaBundle`。

### `POST /api/meta/import`

导入一个 `MetaBundle`。包里的模块和模型，不存在的新建，已存在的按修改处理。整个包要么全部成功，要么全部不生效。

```json
{ "modules": 2, "models": 4 }
```

返回的是这次处理了多少个模块和模型。

## 数据接口

`{model}` 是模型名。模型不存在时所有数据接口都返回 404。

记录的格式：键是字段名，值的格式见 [meta-model.md 的字段类型表](meta-model.md#字段类型)。响应里的记录总是带 `id`、`created_at`、`updated_at`。

```json
{
  "id": 7,
  "product": { "id": 1, "label": "电动螺丝刀" },
  "material": { "id": 3, "label": "螺丝 M3" },
  "quantity": 12,
  "created_at": "2026-11-14 10:20:30",
  "updated_at": "2026-11-14 10:20:30"
}
```

### `POST /api/data/{model}/search`

分页查询。请求体里的每一项都可以不传。

```json
{
  "page": 1,
  "size": 10,
  "filters": [
    { "field": "name", "op": "like", "value": "螺丝" },
    { "field": "category", "op": "eq", "value": "raw" }
  ],
  "sort": [ { "field": "created_at", "order": "desc" } ]
}
```

- `page` 从 1 开始，默认 1。`size` 默认 10，最大 200。
- 多个 `filters` 之间是“并且”的关系。
- 不传 `sort` 时用模型视图里的 `defaultSort`。
- `filters` 和 `sort` 里的 `field` 必须是这个模型的字段或系统字段，否则返回 400。

| `op` | 含义 | `value` |
|---|---|---|
| `eq` | 等于 | 单个值 |
| `ne` | 不等于 | 单个值 |
| `like` | 包含 | 字符串 |
| `gt`、`ge`、`lt`、`le` | 大于、大于等于、小于、小于等于 | 单个值 |
| `in` | 在列表中 | 数组 |

对 `manyToOne` 字段过滤时，`value` 传目标记录的 `id`。

响应：

```json
{ "records": [ /* 记录 */ ], "total": 37, "page": 1, "size": 10 }
```

### `GET /api/data/{model}/{id}`

返回一条记录。不存在时返回 404。

### `POST /api/data/{model}`

新建记录。请求体是记录，不用带系统字段，带了会被忽略。返回保存后的完整记录。

- 没传的字段取默认值，没有默认值的是 `null`。
- 必填字段没有值、值的类型不对、枚举值不在选项里：返回 400，带 `fieldErrors`。
- 违反唯一约束：返回 409，带 `fieldErrors`。
- 请求体里有模型没定义的字段：返回 400。

### `PUT /api/data/{model}/{id}`

修改记录。请求体只需要带要改的字段，没带的字段保持原值。返回修改后的完整记录。校验规则和新建相同。

### `DELETE /api/data/{model}/{id}`

删除记录。记录被其它记录通过 `manyToOne` 引用时返回 409，`message` 里说明被谁引用。

### `GET /api/data/{model}/options?keyword=螺丝&limit=20`

给 `manyToOne` 的下拉框用。`keyword` 可选，按显示字段模糊匹配；`limit` 默认 20，最大 100。

```json
[ { "value": 3, "label": "螺丝 M3" }, { "value": 4, "label": "螺丝 M4" } ]
```

`label` 取哪个字段：调用方用查询参数 `displayField` 指定，前端应该总是把关联字段定义里的 `relation.displayField` 传过来。没有指定时，后端用这个模型第一个 `string` 类型的字段。

## 服务接口

### `POST /api/svc/{model}/{service}`

调用一个 `trigger` 为 `manual` 的服务。

```json
{ "id": 1, "params": {} }
```

- `id`：`scope` 为 `record` 的服务必填，是要操作的记录；`scope` 为 `model` 的服务不用传。
- `params`：传给脚本的参数，可以不传。

`data` 是脚本的返回值，可以是任何 JSON 值。例如编码规则的 `generate` 服务返回 `"MAT-20261114-0001"`。

- 模型或服务不存在、服务不是 `manual`：返回 404。
- 脚本执行出错：返回 400，`message` 是出错原因。

## AI 接口（加分项）

### `POST /api/ai/generate`

```json
{ "prompt": "我要管理供应商，记录名称、联系人、电话和信用等级" }
```

返回一个 `MetaBundle`。**这个接口只生成，不保存。** 前端把结果展示给用户，用户确认或修改后，再调用 `POST /api/meta/import` 让它生效。

没有配置大模型的 API key 时返回 400，`message` 说明没有配置。
