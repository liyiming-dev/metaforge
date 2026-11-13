# 接口契约

这个目录是前端和后端之间唯一的约定。前端按它写页面和 mock，后端按它写接口，两边不用等对方。

| 文件 | 内容 |
|---|---|
| [meta-model.md](meta-model.md) | 元模型 JSON 的格式：模块、模型、字段、服务、视图 |
| [api.md](api.md) | 全部接口的请求和响应 |
| [samples/mes.json](samples/mes.json) | MES 示例的完整元模型。前端 mock、后端测试、文档同学配置示例都以它为准 |

## 怎么改契约

契约可以改，但不能悄悄改。

1. 先在群里说要改什么、为什么。把讨论截图存到 `docs/process/screenshots/`，这算一次讨论记录。
2. 提一个只改 `docs/contract/` 的 PR，前端和后端都同意后合并。
3. 合并后两边各自改代码。前端要同步改 `frontend/src/types/` 和 `frontend/src/mock/mes-sample.ts`。

## 通用约定

- 所有接口以 `/api` 开头，请求体和响应体都是 UTF-8 的 JSON。
- 本地地址：后端 `http://localhost:8200`，前端 `http://localhost:5173`。开发时前端把 `/api` 代理到后端，所以前端代码里只写 `/api/...`。

### 响应格式

成功时 HTTP 状态码是 200：

```json
{ "code": 0, "message": "ok", "data": {} }
```

失败时 HTTP 状态码表示错误大类，响应体格式不变：

```json
{ "code": 40900, "message": "物料编码已存在", "data": { "fieldErrors": { "code": "物料编码已存在" } } }
```

| HTTP 状态码 | code | 含义 |
|---|---|---|
| 400 | 40000 | 请求参数或数据校验不通过 |
| 404 | 40400 | 模型、记录或服务不存在 |
| 409 | 40900 | 冲突：名称重复、违反唯一约束、数据被别处引用 |
| 500 | 50000 | 系统错误 |

`data.fieldErrors` 只在错误能定位到具体字段时出现，键是字段名，值是给用户看的提示。前端把它显示在对应的表单项下面；没有 `fieldErrors` 时，用 `message` 弹一个全局提示。

### 命名

- 元模型 JSON 的键用 camelCase，例如 `displayName`、`searchFields`。
- 模块名、模型名、字段名、服务名统称“标识”，必须匹配 `^[a-z][a-z0-9_]{0,29}$`（小写字母开头，只含小写字母、数字、下划线，最长 30 位）。标识创建后不能改。
- 业务数据记录的键就是字段名，例如 `{ "code": "M-0001", "safety_stock": 10 }`。
