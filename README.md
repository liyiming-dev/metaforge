# MetaForge 元铸

一个模型驱动的低代码平台：开发者在页面上定义模块、模型、字段、视图和服务，平台据此在运行时动态建表/改表、提供通用增删改查与脚本服务接口、注册前端路由并渲染列表和表单，修改定义无需重启后端或改代码。

本项目是华南师范大学《中级软件设计实作》课题 4 的三人小组作品，平台上使用设计器搭建了一套小型 MES（物料、产品、产品 BOM、编码规则）作为示例系统。

## 已实现功能

- **元模型管理与热加载（B1 / B6 / F5）**：模块与模型定义以整份 JSON 持久化到 `meta_module`、`meta_model`；后端 `MetaRegistry` 整份快照热刷新，保存或导入导出后无需重启即时生效。
- **动态表结构同步（B2）**：按字段类型策略动态生成 MySQL DDL，业务表统一 `md_` 前缀，删字段/删模型改名为 `zz_` 前缀保留备份，避免 DDL 不可回滚导致误删数据。
- **通用数据接口与 SQL 防护（B3）**：单控制器 `/api/data/{model}` 服务全部模型，支持分页、过滤、排序与字段级校验（`fieldErrors`）；表名列名严格白名单校验、值全部命名参数绑定。
- **关联关系（B4 / F4）**：多对一字段支持可搜索下拉（`/options`）、批量 `IN` 查询拼装 `{ id, label }`、写入存在校验与删除引用保护（409）；一对多以抽屉内子表标签页维护。
- **脚本服务引擎（B5 / F6）**：集成 AviatorScript 5.4.4 沙箱执行器与受控 `db` 函数（`get`/`find`/`update`/`increment`/`today`/`call`），支持 `beforeCreate`/`afterCreate` 等生命周期钩子与手动服务按钮（示例：物料自动编码、并发安全流水号生成）。
- **模型驱动前端（F1–F3 / F6）**：按 `/api/meta/menus` 逐个 `addRoute` 注册动态路由并支持刷新恢复；`ModelTable`、`ModelForm` 与字段组件注册表渲染任意业务模型。
- **示例系统（MES）**：内置契约示例 [docs/contract/samples/mes.json](docs/contract/samples/mes.json)（2 个模块、4 个模型、21 个字段）；AI 助手（`FR-11`）作为加分项经评估后本期未实现，设计思路记录在决策 011 中。

## 技术栈

| 部分 | 技术选型 |
|---|---|
| 后端 | Java 21、Spring Boot 4.0.5、MySQL 8+/9+、`NamedParameterJdbcTemplate`、Jackson 3、AviatorScript 5.4.4、JUnit 5 + H2 |
| 前端 | Vue 3（`<script setup lang="ts">`）、TypeScript、Ant Design Vue 4、Pinia、Vue Router、Vite |

## 目录结构

| 目录 | 内容 |
|---|---|
| `backend/` | 后端源码与单元测试（119 个测试，H2 + MockMvc） |
| `frontend/` | 前端源码（含开发期内存假后端 `frontend/src/mock`） |
| `docs/contract/` | 前后端接口契约（`meta-model.md`、`api.md`）与 MES 示例元模型 `samples/mes.json` |
| `docs/dev-doc/` | 需求分析、可行性分析、项目设计（含 PlantUML 源码与图）、项目实现、黑盒测试（38/38 通过） |
| `docs/decisions/` | 15 个设计问题的决策记录与问题清单 |
| `docs/process/` | 联调纪要、讨论与验收截图、三人学习日志 |
| `docs/tasks/` | 任务拆解、时间表、环境准备与验收标准 |

## 快速启动

**环境要求**：JDK 21+、Node.js `20.19+`（或 `22.12+`）、MySQL 8+（无需单独安装 Maven，项目自带 `mvnw`；国内镜像已内置在 `backend/pom.xml` 和 `frontend/.npmrc` 中）。更多排查见 [docs/tasks/00-setup.md](docs/tasks/00-setup.md)。

### 1. 配置数据库并启动后端（端口 8200）

1. 将 `backend/src/main/resources/application-local.example.yml` 复制为 `application-local.yml`（已 gitignore），填入本机 MySQL 的账号和密码。首次启动会自动创建 `metaforge` 数据库及两张元模型表（`meta_module`、`meta_model`），无需手工建库。
2. 在 `backend/` 目录启动后端（macOS / Linux 将 `.\mvnw.cmd` 换为 `./mvnw`）：

   ```bash
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

   访问 <http://localhost:8200/api/health> 返回 `"status": "UP"` 即启动成功。

### 2. 启动前端（端口 5173）

另开一个终端，在 `frontend/` 目录安装依赖并启动（默认 `VITE_USE_MOCK=false`，通过 Vite 代理直连 `http://localhost:8200`）：

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 <http://localhost:5173>，首页「环境检查」显示后端连接正常即可开始使用。

> 如需在**不启动后端和 MySQL** 的情况下快速预览界面，可将 [frontend/.env.development](frontend/.env.development) 中的 `VITE_USE_MOCK` 临时改为 `true` 后重启前端。

### 3. 导入 MES 示例元模型

任选一种方式导入内置的 MES 元模型（2 个模块、4 个模型、21 个字段，自动建出 4 张 `md_*` 业务表）：

- **方式一（推荐，页面导入）**：进入左侧「平台设计 → 模型管理」→ 点击「导入」→ 选择 `docs/contract/samples/mes.json` → 点击「确认导入」。导入后左侧菜单会立刻出现「生产管理」和「基础设置」。
- **方式二（命令行导入，在项目根目录 `metaforge/` 执行）**：

  Windows PowerShell：

  ```powershell
  Invoke-RestMethod -Method Post -Uri http://localhost:8200/api/meta/import `
    -ContentType 'application/json; charset=utf-8' `
    -InFile docs/contract/samples/mes.json
  ```

  macOS / Linux / Git Bash：

  ```bash
  curl -X POST http://localhost:8200/api/meta/import \
    -H "Content-Type: application/json; charset=utf-8" \
    --data-binary @docs/contract/samples/mes.json
  ```

### 4. 初始化 2 条编码规则（启用自动编码）

`mes.json` 只包含元模型定义、不含业务数据；而「物料」和「产品」的新增前脚本会读取名为 `material` 和 `product` 的编码规则，因此在新增物料/产品前需先添加这两条规则（任选一种方式）：

- **方式一（页面添加）**：进入左侧「基础设置 → 编码规则」，分别点击「新增」保存以下两条记录（日期格式、流水号位数、当前流水号保持默认值即可）：
  - 规则名称：`material`，前缀：`MAT-`
  - 规则名称：`product`，前缀：`PRD-`
- **方式二（PowerShell 一键写入）**：

  ```powershell
  Invoke-RestMethod -Method Post -Uri http://localhost:8200/api/data/code_rule -ContentType 'application/json; charset=utf-8' -Body '{"name":"material","prefix":"MAT-"}'
  Invoke-RestMethod -Method Post -Uri http://localhost:8200/api/data/code_rule -ContentType 'application/json; charset=utf-8' -Body '{"name":"product","prefix":"PRD-"}'
  ```

完成后即可正常体验：
- 「生产管理 → 物料 → 新增」：留空只读的「物料编码」，保存后自动生成 `MAT-YYYYMMDD-0001`；
- 「生产管理 → 产品 → 新增」：自动生成产品编码，保存后编辑进入「BOM 明细」标签页关联物料；
- 「基础设置 → 编码规则」：点击操作列「生成编码」手动触发脚本服务；
- 「平台设计 → 模型管理 → 新建模型」：保存后无需重启，左侧菜单立即出现并可增删改查、刷新不 404。

## 验证命令

- 后端单元测试（使用内存 H2，无需连接 MySQL）：

  ```bash
  cd backend
  .\mvnw.cmd test
  ```

- 前端类型检查与生产构建：

  ```bash
  cd frontend
  npm run build
  ```

## 相关文档

- 接口契约与元模型规范：[docs/contract/README.md](docs/contract/README.md)
- 15 项设计决策清单：[docs/decisions/README.md](docs/decisions/README.md)
- 协作规范与提交约定：[CONTRIBUTING.md](CONTRIBUTING.md)
- 过程记录说明：[docs/process/README.md](docs/process/README.md)
