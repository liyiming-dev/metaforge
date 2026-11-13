<!-- output: 前端任务包.md -->
# 前端任务包

这份文件发给负责前端的同学，只需要这一个文件。它分四部分：

- 第一部分给你看，一页，讲你要做的几步。
- 第二、三、四部分给 AI 看。你只需要读第三部分每个任务里的“你来验收”。

---

# 第一部分：你要做的事

你负责平台的前端页面。代码交给 AI 写，你负责三件事：**把任务发给 AI，照着清单验收，提交**。

## 1. 准备（只做一次，约 15 分钟）

1. 安装 Git 和 Node.js。Node.js 要 20.19 以上或 22.12 以上，用 `node -v` 确认。
2. 配置 Git 身份。邮箱必须是你注册 GitHub 用的那个，否则你的提交在 GitHub 上不算你的。

   ```bash
   git config --global user.name "你的名字"
   ```

   ```bash
   git config --global user.email "你注册 GitHub 用的邮箱"
   ```

3. 下载代码并启动。仓库地址找组长要。

   ```bash
   git clone 组长给你的仓库地址
   ```

   ```bash
   cd metaforge/frontend
   ```

   ```bash
   npm install
   ```

   ```bash
   npm run dev
   ```

4. 浏览器打开 <http://localhost:5173>。看到“环境检查”，其中“页面数据来源”写着“前端内存 mock（示例里有 4 个模型）”，就成功了。

“后端连接”那一行显示红色是正常的。这个周末你不需要启动后端，页面用的是内置的假数据。假数据在浏览器内存里，**刷新页面就会恢复成初始状态**，这也是正常的。

## 2. 每个任务都重复这四步

一次只做一个任务，按 F1、F2、F3 的顺序来。下面用 F1 举例，做别的任务时把 `f1` 换成对应的编号。

**第一步：开分支。** 在 `metaforge` 目录下执行：

```bash
git checkout main
```

```bash
git pull
```

```bash
git checkout -b feat/f1
```

**第二步：把任务发给 AI。** 两种方式选一种：

- 用 AI 编程工具（Cursor、Trae、Claude Code、通义灵码等，推荐）：用它打开 `metaforge` 文件夹，对它说：

  > 读 docs/handoff/前端任务包.md，完成任务 F1。

- 用网页聊天的 AI：把这个文件整个上传或粘贴给它，然后说：

  > 完成任务 F1。每个要新建或修改的文件，都给出文件路径和完整内容。

  再把它给的每个文件保存到对应的位置。后面的任务在同一个对话里接着说“完成任务 F2”。

**第三步：验收。** 保持 `npm run dev` 开着，照着这个任务的“你来验收”一条一条操作。哪条不对，就告诉 AI“第几条不对，我看到的是什么”，让它改，直到全部通过。最后在 `frontend` 目录下执行下面的命令，不能有报错：

```bash
npm run build
```

**第四步：提交。** 在 `metaforge` 目录下执行：

```bash
git add -A
```

```bash
git commit -m "feat: F1 菜单和动态路由"
```

```bash
git push -u origin feat/f1
```

然后打开 GitHub 上的仓库页面，点页面上方的 “Compare & pull request”，再点 “Create pull request”，把链接发到群里。组长会看，有意见会写在这个页面上，你把意见原样发给 AI 让它改，改完再执行一遍第四步的三条命令。

## 3. 每个任务做完后花 5 分钟

老师评分看的是每个人的思考和收获，不是代码量。所以每个任务做完后：

1. 对 AI 说：“用大白话给我讲讲这个任务的代码是怎么工作的，重点讲任务卡里‘要弄懂的问题’。”
2. 把 `docs/process/journal/TEMPLATE.md` 复制成 `docs/process/journal/你的名字拼音.md`（第一次做），用自己的话写三五句：今天做了什么、弄懂了什么、AI 哪里做错了你是怎么发现的。
3. 任务卡里标了“发到群里”的，用自己的话在群里说一两句，开头带上编号，例如“【007】我这边动态路由用的是……”。有不明白的也在群里问。

## 4. 任务顺序

| 任务 | 内容 | 建议时间 |
|---|---|---|
| F1 | 菜单和动态路由 | 周六上午 |
| F2 | 通用列表页 | 周六下午 |
| F3 | 通用表单 | 周六下午 |
| 联调 1 | 和组长一起，把页面接到真实后端上 | 周六晚 |
| F4 | 关联字段 | 周日上午 |
| F5 | 模型设计器 | 周日下午 |
| F6 | 服务按钮和界面美化 | 周日晚 |
| 联调 2 | 和组长一起 | 周日晚 |
| F7 | AI 助手页（加分项） | 组长通知后再做 |

联调在组长的电脑上做：你把代码推上去，组长拉下来和后端一起运行，你们一起看。你不用装后端的环境。

## 5. 遇到问题

- AI 改了三次还是不对：把页面截图和 AI 的回复发到群里。
- AI 说要修改 `src/api`、`src/types`、`src/mock` 或 `docs/contract` 里的文件：先不要让它改，发到群里问组长。
- `npm install` 很慢或失败：确认 Node.js 版本符合要求，删掉 `frontend/node_modules` 文件夹再试一次。

---

# 第二部分：给 AI 的总说明

从这里开始是写给 AI 的。

你在帮一位大学生完成课程作品的前端。用户会说“完成任务 F1”这样的话，你按第三部分对应的任务卡实现。用户不一定熟悉前端，所以你要一次把事情做对，并且把做了什么讲清楚。

## 项目是什么

MetaForge 是一个模型驱动的低代码平台。开发者在页面上定义“模型”（例如物料、产品），每个模型有字段、视图和服务；平台后端按定义建表并提供通用的增删改查接口；前端按定义渲染菜单、列表页和表单页。改了定义不用改代码、不用重启。

前端要做的核心是三件事：

1. 根据接口返回的菜单，运行时注册路由（动态路由）。
2. 用同一套通用的列表页和表单页，显示任意模型的数据。
3. 一个“模型设计器”，让开发者在页面上定义模型。

## 技术栈

Vue 3（组合式 API，`<script setup lang="ts">`）、TypeScript（严格模式）、Ant Design Vue 4、Pinia、Vue Router 5（用法和 4 相同）、Vite、axios、dayjs。具体版本见第四部分的 `package.json`。

- Ant Design Vue 已经全局注册，模板里直接用 `a-button`、`a-table` 这些组件，不用 import。
- 图标从 `@ant-design/icons-vue` 按需 import。
- TypeScript 开了严格检查，用下标访问数组或对象时结果可能是 `undefined`，要判空。

## 硬性规则

违反任何一条都算没完成。

1. **一次只做用户指定的那一个任务。** 不要提前实现后面任务的内容。
2. **不能写死任何业务模型。** 除了 `src/mock` 目录，代码里不能出现 `material`、`product`、`product_bom`、`code_rule` 这类具体的模型名或它们的字段名。列表有哪些列、表单有哪些字段，全部来自接口返回的模型定义。
3. **不要修改这些文件和目录：** `src/api/`、`src/types/`、`src/mock/`、`docs/contract/`、`vite.config.ts`、各个 `tsconfig` 文件。也不要新增依赖。如果你认为它们有错，或者任务确实需要改它们，停下来告诉用户，让用户去问组长。
4. **调接口只用 `src/api/` 里已有的函数**（`metaApi`、`dataApi`、`callService`、`generateByAi`），类型只用 `src/types/` 里已有的定义。不要自己创建 axios 实例，不要自己拼接口地址。
5. **错误提示不要重复弹。** `src/api/request.ts` 在请求失败时已经弹出全局提示（有字段级错误时除外）。页面里捕获 `ApiError`，只是为了显示 `fieldErrors` 或者恢复界面状态。
6. 界面文字和代码注释都用中文。
7. 文件放在约定的位置：页面放 `src/pages/`，可复用的组件放 `src/components/`，字段组件放 `src/components/fields/`，Pinia 的 store 放 `src/stores/`。任务卡里写了文件名的，必须用那个文件名，因为后面的任务会引用它。
8. 如果你能读取仓库里的文件，以仓库里的实际文件为准。第四部分是它们的副本，可能比仓库旧。

## 完成一个任务后怎么回复

如果你能执行命令，先在 `frontend` 目录下运行 `npm run build`，确认没有类型错误和构建错误。然后按下面的格式回复：

1. **改动的文件**：新建了哪些、修改了哪些。如果你不能直接写文件，给出每个文件的路径和完整内容。
2. **自查**：对照任务卡“你来验收”的每一条，说明代码是怎么满足它的。有做不到的要明说。
3. **构建结果**：`npm run build` 是否通过。没法执行就说明，让用户自己运行。
4. **讲解**：用三五句大白话说明实现思路，并回答任务卡里“要弄懂的问题”。用户要靠这段话理解代码、写学习日志。

## 数据从哪来

开发时 `.env.development` 里 `VITE_USE_MOCK=true`，所有接口请求由 `src/mock` 在浏览器内存里处理，行为和真实后端一致（规则见第四部分的接口契约）。刷新页面后数据恢复初始状态。你写的代码不需要关心是 mock 还是真实后端，只管调用 `src/api/` 里的函数。

mock 里的初始数据如下，“你来验收”里的操作都基于它：

物料（模型名 `material`），4 条：

| id | 物料编码 | 物料名称 | 规格 | 类别 | 单位 | 安全库存 | 启用 |
|---|---|---|---|---|---|---|---|
| 1 | MAT-20261114-0001 | 螺丝 M3 | M3×8 | 原材料 | 个 | 500 | 是 |
| 2 | MAT-20261114-0002 | 直流电机 | 12V 380 | 半成品 | 个 | 50 | 是 |
| 3 | MAT-20261114-0003 | 润滑脂 | 100g | 辅料 | 支 | 10 | 是 |
| 4 | MAT-20261114-0004 | ABS 外壳 | 黑色 | 半成品 | 套 | 30 | 否 |

产品（`product`），2 条：电动螺丝刀（id 1，规格 ES-12V，上市日期 2026-09-01）、手持吸尘器（id 2，规格 VC-01，没有上市日期）。

产品 BOM（`product_bom`，不在菜单里），4 条：电动螺丝刀用螺丝 M3 共 12 个、直流电机 1 个、ABS 外壳 1 个；手持吸尘器用直流电机 1 个。

编码规则（`code_rule`），2 条：`material`（前缀 MAT-）和 `product`（前缀 PRD-）。

---

# 第三部分：任务卡

## F1 菜单和动态路由

### 要实现的（AI 读）

新建 `src/stores/menu.ts`、`src/pages/ModelListPage.vue`；修改 `src/router/index.ts`、`src/layouts/BasicLayout.vue`。

1. `src/stores/menu.ts` 导出 `useMenuStore`：
   - 状态：`menus`（`MenuNode[]`）、`loaded`（布尔）。
   - 方法 `load()`：调用 `metaApi.menus()`，保存到 `menus`。然后对每个叶子节点（同时有 `model` 和 `path` 的节点）调用：

     ```ts
     router.addRoute('layout', {
       path: node.path,
       name: `app-${node.model}`,
       component: () => import('@/pages/ModelListPage.vue'),
       props: { model: node.model },
       meta: { title: node.displayName },
     })
     ```

     把注册过的路由名记在一个数组里。最后把 `loaded` 设为 `true`。
   - 请求失败时也要把 `loaded` 设为 `true`、`menus` 设为空数组，否则路由守卫会死循环。
   - 方法 `reload()`：对记下的每个路由名调用 `router.removeRoute`，清空这个数组，然后重新执行加载逻辑。任务 F5 会用到。
   - 这个文件要 import `@/router`，而 `src/router/index.ts` 也要 import 这个 store。两个文件互相 import 没有问题，因为双方都只在函数内部使用对方。
2. 在 `src/router/index.ts` 里加全局前置守卫：

   ```ts
   router.beforeEach(async (to) => {
     const menuStore = useMenuStore()
     if (!menuStore.loaded) {
       await menuStore.load()
       // 动态路由刚注册好，让这次跳转重新匹配一遍
       return to.fullPath
     }
   })
   ```

   为此要把文件末尾的 `export default createRouter(...)` 改成先赋值给变量 `router`，加完守卫再 `export default router`。
3. 修改 `src/layouts/BasicLayout.vue` 的菜单：
   - 第一项“首页”保留。
   - 之后，`menus` 里的每个节点渲染成一个 `a-sub-menu`：`key` 用节点的 `key`，标题用 `displayName`，图标按节点的 `icon` 名字从 `@ant-design/icons-vue` 里取，取不到就用 `AppstoreOutlined`。
   - 每个子节点渲染成一个 `a-menu-item`，`key` 用子节点的 `path`。
   - 选中项跟随当前路由：`selectedKeys` 是 `[route.path]`。
   - 所有分组默认展开，用户可以手动收起。
4. `src/pages/ModelListPage.vue`：用 `defineProps<{ model: string }>()` 接收模型名，暂时只显示一个 `a-empty`，描述文字是“模型 xxx 的列表页将在任务 F2 实现”，其中 xxx 是模型名。
5. 任务完成后，另外生成 `docs/decisions/007-前端动态路由怎么做.md`：按第四部分的决策记录模板，写清两个候选方案（一条带参数的固定路由 `/app/:model`；按菜单用 `addRoute` 逐个注册）各自的做法、好处、代价，以及本项目选后者的理由。“讨论记录”和“代价和反思”两节留空，由用户自己填。

### 你来验收（人做）

1. 左侧菜单从上到下是：首页、生产管理（下面有物料、产品）、基础设置（下面有编码规则）。两个分组默认是展开的，分组名前面有图标。
2. 点“物料”：地址栏变成 `/app/material`，顶部标题显示“物料”，菜单里“物料”高亮，内容区显示“模型 material 的列表页将在任务 F2 实现”。
3. 点“产品”，然后按 F5 刷新页面：还停在产品页，没有变成“页面不存在”。
4. 在地址栏把地址改成 `/app/abc` 回车：显示“页面不存在”。
5. 点“首页”能回到首页。
6. 按 F12 打开控制台，没有红色报错。只有一条 `502 (Bad Gateway)` 可以忽略，那是因为没开后端。

### 要弄懂的问题

- 为什么用 `addRoute` 一个一个注册，而不是写一条 `/app/:model`？
- 刷新页面时，守卫里的 `return to.fullPath` 起了什么作用？去掉它会怎样？

发到群里：用自己的话说说第一个问题的答案，开头写【007】。

---

## F2 通用列表页

### 要实现的（AI 读）

新建 `src/stores/model.ts`、`src/components/fields/display.ts`、`src/components/ModelTable.vue`；修改 `src/pages/ModelListPage.vue`。

1. `src/stores/model.ts` 导出 `useModelStore`：
   - `get(name)`：返回 `Promise<ModelDef>`。第一次调用 `metaApi.model(name)`，结果按模型名缓存，之后直接返回缓存。
   - `invalidate(name?)`：清掉一个模型的缓存；不传参数时清空全部。
2. `src/components/fields/display.ts` 导出：
   - `SYSTEM_FIELD_TITLES`：`{ id: 'ID', created_at: '创建时间', updated_at: '修改时间' }`。
   - `displayValue(field: FieldDef | undefined, value: unknown): string`，规则：
     - 值是 `null`、`undefined` 或空字符串：返回 `—`。
     - 没有字段定义（系统字段）：返回 `String(value)`。
     - `enum`：返回选项里对应的 `label`，找不到就返回原值。
     - `boolean`：返回“是”或“否”。
     - `manyToOne`：值是 `{ id, label }`，返回 `label`。
     - `decimal`：最多保留字段 `scale` 位小数（默认 4），去掉末尾多余的 0。
     - 其它：返回 `String(value)`。
3. `src/components/ModelTable.vue` 是通用的“搜索区 + 工具栏 + 表格”，属性：

   ```ts
   defineProps<{
     /** 模型名 */
     model: string
     /** 固定的过滤条件：每次查询都带上，用户看不到也改不了。任务 F4 的子表会用 */
     fixedFilters?: Filter[]
     /** 不显示的列，同时也不出现在搜索区。任务 F4 的子表会用 */
     hiddenColumns?: string[]
     /** 新建记录时固定带上的值。任务 F3 会把它传给表单 */
     presetValues?: DataRecord
   }>()
   ```

   - 用 `useModelStore().get(model)` 取模型定义。`model` 变化时重新加载，并重置搜索条件、排序和页码。
   - **列**：取 `views.list.columns`，去掉 `hiddenColumns` 里的，去掉既不是模型字段也不是系统字段的。列标题用字段的 `displayName`，系统字段用 `SYSTEM_FIELD_TITLES`。单元格文字用 `displayValue`；`enum` 和 `boolean` 用 `a-tag` 包起来，`boolean` 为“是”时用绿色。除了 `text`、`manyToOne` 类型，其它列都可以点列头排序。
   - **操作列**：放在最后，有“编辑”（链接样式的按钮）和“删除”（用 `a-popconfirm` 确认）。
   - **搜索区**：取 `views.list.searchFields`，去掉 `hiddenColumns` 里的。每个字段按类型选控件和比较方式：

     | 字段类型 | 控件 | 发给接口的条件 |
     |---|---|---|
     | `string`、`text` | 输入框 | `like` |
     | `enum` | 下拉框，可清空 | `eq` |
     | `boolean` | 下拉框，选项为“是”“否”，可清空 | `eq`，值是 `true` 或 `false` |
     | `integer`、`decimal` | 数字输入框 | `eq` |
     | `date`、`datetime` | 日期范围选择 | 两个条件：`ge` 起始和 `le` 结束。`date` 用 `YYYY-MM-DD`；`datetime` 的起始补 ` 00:00:00`，结束补 ` 23:59:59` |
     | `manyToOne` | 本任务先跳过，任务 F4 再做 | |

     没填的条件不发送。有“查询”按钮（回到第 1 页再查）和“重置”按钮（清空条件再查）。一个搜索字段都没有时，不显示搜索区。
   - **查询**：调用 `dataApi.search(model, { page, size, filters, sort })`，`filters` 是 `fixedFilters` 加上搜索区的条件。每页条数初始值用 `views.list.pageSize`，没有就用 10。分页器显示总条数，可以切换每页 10、20、50 条。点列头排序时传 `sort: [{ field, order }]`，取消排序时不传 `sort`。
   - **工具栏**：右侧放“新增”按钮（主按钮样式）。本任务里，点“新增”和“编辑”只弹一条提示“表单在任务 F3 实现”。
   - **删除**：调用 `dataApi.remove`。成功后提示“已删除”并重新查询；如果当前页已经没有数据而且不是第 1 页，回到上一页。失败时不用再弹提示，列表保持不变。
   - 查询期间表格显示加载状态。表格的 `row-key` 用 `id`。
   - 用 `defineExpose` 暴露一个 `reload()` 方法，供外部刷新当前页。
4. `src/pages/ModelListPage.vue` 改成只渲染 `<ModelTable :model="model" />`。

### 你来验收（人做）

1. 点“物料”：表格有 7 列（物料编码、物料名称、规格、类别、单位、安全库存、启用）和一个操作列，共 4 行，从上到下是 ABS 外壳、润滑脂、直流电机、螺丝 M3。
2. “类别”列显示的是“原材料”“半成品”“辅料”，不是英文。“启用”列显示“是”或“否”，ABS 外壳那一行是“否”。
3. 表格上方的搜索区有 3 项：物料编码、物料名称、类别。
4. 物料名称输入“螺丝”，点“查询”：只剩 1 行。点“重置”：恢复 4 行。
5. 类别选“半成品”，点“查询”：剩 2 行（直流电机、ABS 外壳）。点“重置”。
6. 点“安全库存”列头：按 10、30、50、500 从小到大排列。再点一次：从大到小。
7. 表格下方显示“共 4 条”之类的总数。
8. 点“螺丝 M3”那一行的“删除”并确认：页面上方弹出红色提示，说这条记录被“产品 BOM”引用，不能删除。列表没有变化。
9. 点“润滑脂”那一行的“删除”并确认：提示“已删除”，列表剩 3 行。
10. 点菜单“编码规则”：同一个页面显示出另一套列（规则名称、前缀、日期格式、流水号位数、当前流水号），共 2 行，搜索区只有“规则名称”一项。
11. 点菜单“产品”：有 4 列（产品编码、产品名称、规格、上市日期），共 2 行。手持吸尘器的上市日期显示“—”。
12. 在编辑器里全局搜索 `material`，范围是 `frontend/src`，排除 `mock` 文件夹：没有结果。

### 要弄懂的问题

- 物料、产品、编码规则三个页面长得不一样，但只写了一个组件。它是从哪里知道该显示哪些列的？

---

## F3 通用表单

### 要实现的（AI 读）

新建 `src/components/fields/` 下的 6 个字段输入组件和 `index.ts`，新建 `src/components/ModelForm.vue`；修改 `src/components/ModelTable.vue`。

1. 字段输入组件，统一的属性和事件：

   ```ts
   defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
   defineEmits<{ 'update:modelValue': [value: unknown] }>()
   ```

   | 文件 | 用于的字段类型 | 做法 |
   |---|---|---|
   | `StringField.vue` | `string` | `a-input`，最大长度用字段的 `length`（默认 255）。清空时发出 `null`，不要发空字符串 |
   | `TextField.vue` | `text` | `a-textarea`，3 行。清空时发出 `null` |
   | `NumberField.vue` | `integer`、`decimal` | `a-input-number`，宽度占满。`integer` 不允许小数；`decimal` 的小数位数用字段的 `scale`（默认 4） |
   | `BooleanField.vue` | `boolean` | `a-switch`。值为 `null` 时按关闭显示 |
   | `DateField.vue` | `date`、`datetime` | `a-date-picker`，用它的 `value-format` 属性让绑定值直接是字符串：`date` 用 `YYYY-MM-DD`；`datetime` 加上 `show-time`，用 `YYYY-MM-DD HH:mm:ss` |
   | `EnumField.vue` | `enum` | `a-select`，选项来自字段的 `options`。字段不是必填时可以清空 |

2. `src/components/fields/index.ts` 导出字段类型到组件的注册表：

   ```ts
   export const fieldComponents: Partial<Record<FieldType, Component>> = {
     string: StringField,
     text: TextField,
     integer: NumberField,
     decimal: NumberField,
     boolean: BooleanField,
     date: DateField,
     datetime: DateField,
     enum: EnumField,
   }
   ```

   表单里一律用 `<component :is="fieldComponents[field.type]" ...>` 渲染。注册表里没有的类型（目前是 `manyToOne`），显示一个禁用的输入框，占位文字“这种字段在任务 F4 实现”。**除了 `src/components/fields/` 目录，别处不要出现按字段类型写的一长串 `v-if` 或 `switch`。**
3. `src/components/ModelForm.vue`，用 `a-drawer` 展示，宽 720：

   ```ts
   defineProps<{
     open: boolean
     model: string
     /** 要编辑的记录 id。没有值表示新建 */
     recordId?: number | null
     /** 固定带上的值：对应的字段不显示在表单里，提交时带上 */
     presetValues?: DataRecord
   }>()
   defineEmits<{ 'update:open': [value: boolean]; saved: [record: DataRecord] }>()
   ```

   - 标题：新建时是“新增”加模型显示名，编辑时是“编辑”加模型显示名。有“取消”和“保存”按钮，保存期间按钮显示加载状态。
   - 每次打开时初始化：用 `useModelStore().get(model)` 取模型定义。新建时，每个字段（`oneToMany` 除外）的初始值是它的 `defaultValue`，没有就是 `null`，然后用 `presetValues` 覆盖。编辑时，用 `dataApi.get(model, recordId)` 取记录。
   - 布局：`a-form` 用垂直布局。`views.form.groups` 里的每一组，先显示组标题，再用 `a-row`、`a-col` 排列字段，每个字段占 `24 / 组的 columns` 格。
   - 这些字段不显示：组里写了但模型里没有的；`oneToMany` 类型的；`presetValues` 里有的。
   - 表单项的标签用字段的 `displayName`。字段 `required` 为 `true` 时加必填规则，提示文字是“请填写”加显示名；`boolean` 类型不加必填规则。
   - 字段 `readonly` 为 `true` 时禁用。新建时如果它没有值，在输入框下方显示说明文字“保存后自动生成”。
   - 提交：先做表单校验。提交的内容是所有非 `oneToMany` 字段的当前值；如果某个值是带 `id` 的对象（`manyToOne` 读出来的 `{ id, label }`），只提交它的 `id`。新建调用 `dataApi.create`，编辑调用 `dataApi.update`。成功后提示“已保存”，发出 `saved` 事件，关闭抽屉。
   - 失败时捕获 `ApiError`：如果 `fieldErrors` 不为空，把每条提示显示在对应表单项的下方（用 `a-form-item` 的 `validate-status` 和 `help`），用户修改这个字段后清除提示。`fieldErrors` 为空时什么都不用做。
   - 关闭时清空表单状态。
   - `views.form.tabs`（子表标签页）本任务不做，任务 F4 再加。
4. 修改 `src/components/ModelTable.vue`：点“新增”打开 `ModelForm`（不传 `recordId`，把自己的 `presetValues` 传下去）；点“编辑”打开 `ModelForm` 并传这一行的 `id`。表单发出 `saved` 后刷新当前页。
5. 任务完成后，另外生成 `docs/decisions/008-视图怎么渲染.md`：按第四部分的决策记录模板，比较“用一长串 `v-if` 判断字段类型”和“用字段类型到组件的注册表”两种做法，说明本项目选后者的理由，以及以后新增一种字段类型（例如图片）时分别要改哪些地方。“讨论记录”和“代价和反思”两节留空。

### 你来验收（人做）

1. 物料页点“新增”：右侧滑出“新增物料”。表单分两组：“基本信息”里每行 2 个字段（物料编码、物料名称、规格、类别、单位、安全库存、启用），“其它”里是备注，独占一行。
2. 默认值：类别是“原材料”，单位是“个”，安全库存是 0，启用是打开的。物料编码是灰色的，不能填，下面写着“保存后自动生成”。
3. 什么都不填直接点“保存”：“物料名称”下面出现红字提示，抽屉没有关。
4. 物料名称填“测试物料”，点“保存”：提示“已保存”，抽屉关闭，列表第一行是“测试物料”，它的编码形如 `MOCK-0005`。
5. 点“测试物料”那一行的“编辑”：表单里是刚才填的内容。把规格改成“A1”，保存：列表里这一行的规格变成“A1”。
6. 到“编码规则”页点“新增”，规则名称填 `material`，保存：“规则名称”输入框下面出现“规则名称已存在”，页面上方没有再弹红色提示。把名称改成 `test` 再保存：成功。
7. 到“产品”页点“新增”：“上市日期”是日期选择器。填产品名称、选一个日期，保存：列表里上市日期显示成 `2026-xx-xx` 的格式。

### 要弄懂的问题

- “字段类型对应哪个输入组件”为什么集中写在 `fields/index.ts` 里？如果以后要加一种“图片”字段，要改哪几个地方？

发到群里：用自己的话说说上面这个问题的答案，开头写【008】。

---

## F4 关联字段

### 要实现的（AI 读）

新建 `src/components/fields/ManyToOneField.vue`；修改 `src/components/fields/index.ts`、`src/components/ModelForm.vue`、`src/components/ModelTable.vue`。

1. `ManyToOneField.vue`，属性和事件与其它字段组件相同：
   - 用 `a-select`，开启搜索（`show-search`），关闭本地过滤（`:filter-option="false"`），可清空，占位文字“输入关键字搜索”。
   - 选项来自 `dataApi.options(field.relation.model, { keyword, displayField: field.relation.displayField, limit: 20 })`。下拉框展开时请求一次；用户输入时带关键字重新请求，加 300 毫秒防抖。
   - `modelValue` 可能是 `{ id, label }`、数字 id 或 `null`。是 `{ id, label }` 时要能直接显示出 `label`，不用等请求。
   - 用户选中一项后，发出 `{ id, label }` 这个对象；清空时发出 `null`。
2. 在 `src/components/fields/index.ts` 的注册表里加上 `manyToOne: ManyToOneField`。
3. `ModelTable.vue` 的搜索区支持 `manyToOne` 字段：用 `ManyToOneField`，条件是 `eq`，值取 `id`。
4. `ModelForm.vue` 加子表标签页：
   - 模型的 `views.form.tabs` 不为空时，在所有分组的下方渲染 `a-tabs`，每一项一个标签页，标题用它的 `title`。
   - 找到这一项的 `field` 对应的 `oneToMany` 字段，记它的 `relation.model` 为子表模型，`relation.mappedBy` 为回指字段。
   - 编辑已有记录时，标签页里渲染：

     ```vue
     <ModelTable
       :model="子表模型"
       :fixed-filters="[{ field: 回指字段, op: 'eq', value: recordId }]"
       :hidden-columns="[回指字段]"
       :preset-values="{ [回指字段]: recordId }"
     />
     ```

   - 新建、还没保存时，标签页里显示一个 `a-alert`：“先保存，再添加明细”。
   - 新建并保存成功后，如果这个模型有子表标签页，抽屉不关闭，转成编辑刚保存的这条记录，这样用户可以接着添加明细。没有子表标签页的模型，保存后照常关闭。
5. 子表里的“新增”和“编辑”会再打开一层 `ModelForm` 抽屉，这是预期的。

### 你来验收（人做）

1. 到“产品”页，点“电动螺丝刀”的“编辑”：表单下方有“BOM 明细”标签页，里面的表格有 3 行，列是“物料”和“数量”，没有“产品”这一列。物料显示的是名称（螺丝 M3、直流电机、ABS 外壳）。
2. 在 BOM 明细里点“新增”：弹出的表单只有“物料”和“数量”两项。在物料框里输入“润滑”，下拉里出现“润滑脂”，选中它。数量默认是 1。保存后明细变成 4 行。
3. 点刚加的那条明细的“编辑”：物料框里显示的是“润滑脂”，不是数字。
4. 删除一条明细：表格变回 3 行。
5. 关掉抽屉，点产品页的“新增”：BOM 明细里显示“先保存，再添加明细”。填产品名称后保存：抽屉没有关，BOM 明细里可以点“新增”了。
6. 在编辑器里全局搜索 `product_bom` 和 `material`，范围是 `frontend/src`，排除 `mock` 文件夹：都没有结果。

### 要弄懂的问题

- “物料”这个字段，从接口读出来是 `{ id, label }`，保存时提交的却只是 `id`。为什么这样设计？
- BOM 明细是单独保存的，没有和产品一起保存。所以新建产品时要先保存才能加明细。还有别的做法吗？

---

## F5 模型设计器

### 要实现的（AI 读）

新建 `src/pages/design/ModuleListPage.vue`、`src/pages/design/ModelDesignListPage.vue`、`src/pages/design/ModelEditPage.vue`；修改 `src/router/index.ts`、`src/layouts/BasicLayout.vue`。页面内部可以再拆子组件，放在 `src/components/design/` 下。

**路由和菜单**

在 `src/router/index.ts` 的 `layout` 路由的 `children` 里加 4 条固定路由：

| 地址 | 页面 | `meta.title` |
|---|---|---|
| `/design/modules` | `ModuleListPage.vue` | 模块管理 |
| `/design/models` | `ModelDesignListPage.vue` | 模型管理 |
| `/design/models/new` | `ModelEditPage.vue` | 新建模型 |
| `/design/models/:name` | `ModelEditPage.vue`，用 `props: true` 接收 `name` | 编辑模型 |

在 `BasicLayout.vue` 的动态菜单后面加一个固定分组“平台设计”（图标 `ToolOutlined`），下面是“模块管理”和“模型管理”。在新建和编辑模型的页面上，菜单高亮“模型管理”。

**模块管理页**

- 表格列：标识、显示名、图标、排序、操作（编辑、删除）。数据来自 `metaApi.modules()`。
- “新增”和“编辑”用 `a-modal` 里的表单：
  - 标识：新建时可以填，输入时就按 `^[a-z][a-z0-9_]{0,29}$` 校验；编辑时禁用。
  - 显示名：必填。
  - 图标：下拉选择，给出这几个：`AppstoreOutlined`、`SettingOutlined`、`DatabaseOutlined`、`ShopOutlined`、`ToolOutlined`、`TeamOutlined`，选项里同时显示图标和名字。
  - 排序：数字，默认 100。
- 保存调用 `metaApi.saveModule`，删除调用 `metaApi.removeModule`（先确认）。成功后刷新列表，并调用 `useMenuStore().reload()`。

**模型管理页**

- 表格列：标识、显示名、所属模块（显示模块的显示名）、字段数、是否显示在菜单、操作。数据来自 `metaApi.models()` 和 `metaApi.modules()`。
- 操作：“编辑”跳到 `/design/models/标识`；“数据”跳到 `/app/标识`，只在“显示在菜单”为是时可点；“删除”先确认，调用 `metaApi.removeModel`，成功后刷新列表、调用 `useMenuStore().reload()` 和 `useModelStore().invalidate(标识)`。
- 页面上方的按钮：
  - “新建模型”：跳到 `/design/models/new`。
  - “导出”：调用 `metaApi.exportAll()`，把结果格式化成 JSON，下载为 `metaforge-meta.json`。
  - “导入”：打开 `a-modal`，可以选择一个 `.json` 文件，也可以直接粘贴 JSON 文本。解析失败时提示“不是合法的 JSON”。解析成功后显示将要导入几个模块、几个模型以及模型的显示名，用户点“确认导入”才调用 `metaApi.importBundle`。成功后提示导入了多少个，刷新列表，调用 `useMenuStore().reload()` 和 `useModelStore().invalidate()`。

**模型编辑页**

页面维护一份草稿，类型是 `ModelInput`。

- 新建时的初始草稿：标识和显示名为空，`module` 取第一个模块，`showInMenu` 为 `true`，`sort` 为 100，`fields` 和 `services` 是空数组，`views` 是 `undefined`。
- 编辑时：用 `metaApi.model(name)` 取定义，深拷贝一份作为草稿。同时记下这时已有的字段标识，下面称为“已保存的字段”。
- 页面顶部：标题、“返回”按钮（回到模型管理页）、“保存”按钮。
- 内容用 `a-tabs` 分成 5 个标签页：

**标签页 1：基本信息**

标识（新建时可填并即时校验，输入框下方说明规则“小写字母开头，只含小写字母、数字、下划线，最长 30 位”；编辑时禁用）、显示名（必填）、说明、所属模块（下拉，来自 `metaApi.modules()`）、显示在菜单（开关）、排序（数字）。

**标签页 2：字段**

一个表格，每行一个字段，表格下方有“添加字段”按钮。列：

- 标识：输入框。已保存的字段禁用。
- 显示名：输入框。
- 类型：下拉框，10 个选项，显示中文：单行文本（`string`）、多行文本（`text`）、整数（`integer`）、小数（`decimal`）、是否（`boolean`）、日期（`date`）、日期时间（`datetime`）、枚举（`enum`）、多对一（`manyToOne`）、一对多（`oneToMany`）。已保存的字段禁用，因为接口不允许修改字段类型。
- 必填、唯一、只读：三个勾选框。`oneToMany` 的字段这三项都禁用。
- 操作：配置、上移、下移、删除。

点“配置”打开弹窗，内容随类型变化：

| 类型 | 可配置的项 |
|---|---|
| `string` | 长度，默认 255 |
| `decimal` | 精度，默认 18；小数位数，默认 4 |
| `enum` | 选项列表，每项有“值”和“显示文字”，可以增删，至少一项 |
| `manyToOne` | 关联模型（下拉，列出所有模型）；显示字段（下拉，列出所选模型里 `string` 类型的字段） |
| `oneToMany` | 子表模型（下拉，列出所有模型）；回指字段（下拉，列出所选模型里 `manyToOne` 类型的字段） |
| 除 `manyToOne`、`oneToMany` 以外的所有类型 | 默认值，用 `fieldComponents` 里对应类型的输入组件 |

切换字段类型时，清掉不属于新类型的配置项。

添加字段时，如果草稿的 `views` 不是 `undefined`，自动把新字段加到 `views.list.columns` 的末尾（`text` 和 `oneToMany` 类型除外）和第一个表单分组的末尾（`oneToMany` 类型除外）。删除字段时，把它从 `views` 里所有引用它的地方移除。

**标签页 3：视图**

- 最上面一个开关“使用默认视图”。打开时草稿的 `views` 是 `undefined`，下面的配置全部禁用，并显示说明“保存后由系统按默认规则生成视图”。新建模型时默认打开。编辑已有模型时默认关闭，显示当前的视图。从打开切到关闭时，按字段生成一份初始视图（规则见第四部分契约里的“默认视图”）。
- 列表显示的列：多选下拉框，选项是所有非 `oneToMany` 的字段加上三个系统字段，选中的顺序就是显示的顺序。
- 搜索字段：多选下拉框，选项是所有非 `oneToMany`、非 `text` 的字段。
- 默认排序：一个字段下拉框加一个“升序、降序”的选择。
- 每页条数：数字。
- 表单分组：可以增删的列表，每组有标题、每行几个字段（1 到 4）、包含的字段（多选下拉框，选项是所有非 `oneToMany` 的字段）。
- 子表标签页：列出所有 `oneToMany` 字段，每个可以勾选是否显示，并可以改标题。

**标签页 4：服务**

可以增删的列表，每项有：标识、显示名、触发时机（下拉，显示中文：手动调用 `manual`、新建前 `beforeCreate`、新建后 `afterCreate`、修改前 `beforeUpdate`、修改后 `afterUpdate`、删除前 `beforeDelete`）、范围（只在触发时机是“手动调用”时显示：针对一条记录 `record`、不针对具体记录 `model`）、启用（开关）、脚本（`a-textarea`，8 行，等宽字体）。

**标签页 5：JSON**

只读显示 `JSON.stringify(草稿, null, 2)`，有一个“复制”按钮。

**保存**

先在前端检查，不通过时切到对应的标签页，并提示具体是哪里不对：

- 模型标识符合规则，显示名不为空。
- 至少有一个字段。
- 每个字段的标识符合规则、互不重复、不是 `id`、`created_at`、`updated_at`，显示名不为空。
- `enum` 字段至少有一个选项；关联字段已经选了关联模型。

通过后，新建调用 `metaApi.createModel`，编辑调用 `metaApi.updateModel`。成功后：提示“已保存”，调用 `useModelStore().invalidate(标识)`，`await useMenuStore().reload()`；如果是新建，跳到 `/design/models/标识`。接口返回的错误已经由全局提示显示，不用再处理。

### 你来验收（人做）

1. 左侧菜单最下面有“平台设计”分组，里面是“模块管理”和“模型管理”。
2. 点“模型管理”：列表有 4 行，标识分别是 `material`、`product`、`product_bom`、`code_rule`。`product_bom` 的“显示在菜单”是否。
3. 点“新建模型”。标识输入 `Supplier 1`：输入框下面立刻出现红字提示。改成 `supplier`，显示名填“供应商”，所属模块选“基础设置”。
4. 切到“字段”标签页，添加三个字段：`name`（名称，单行文本，勾选必填）、`phone`（电话，单行文本）、`level`（信用等级，枚举）。先不给 `level` 配选项，直接点“保存”：提示枚举字段需要选项，没有保存成功。
5. 点 `level` 的“配置”，加三个选项 A、B、C，再点“保存”：提示“已保存”。
6. **不要刷新页面。** 左侧“基础设置”下面多了“供应商”。点进去：列表的列是名称、电话、信用等级。点“新增”，不填名称保存会提示必填；信用等级的下拉里有 A、B、C。填好保存后，这条记录能搜索、编辑、删除。
7. 回到“模型管理”，编辑 `supplier`：`name` 这一行的标识和类型是禁用的。添加字段 `contact`（联系人，单行文本），保存。再去“供应商”页：列表和表单里都有“联系人”了。
8. 编辑 `material`，在“视图”标签页的“列表显示的列”里去掉“规格”，保存。去“物料”页：没有“规格”这一列了。
9. 编辑 `code_rule`，在“服务”标签页能看到“生成编码”，触发时机是“手动调用”。“JSON”标签页里是完整的定义。
10. 在“模型管理”里删除 `material`：提示它被“产品 BOM”的关联字段引用，不能删除。删除 `supplier`：成功，左侧菜单里的“供应商”消失。
11. 点“导出”：下载到一个 JSON 文件，打开能看到 `modules` 和 `models`。点“导入”，选刚下载的文件：显示将导入 2 个模块、4 个模型，确认后提示成功。
12. 点“模块管理”：新增一个模块，标识 `sales`，显示名“销售管理”，保存成功。它下面没有模型，所以不出现在左侧菜单里，这是正常的。删除 `mes`：提示模块下还有模型，不能删除。

### 要弄懂的问题

- 第 6 步里，为什么不刷新页面菜单就变了？`menuStore.reload()` 做了哪几件事？
- 为什么已保存的字段不让改类型？

发到群里：把第 6 步的页面截一张图发到群里，说一句“设计器能用了”，开头写【010】。

---

## F6 服务按钮和界面美化

开始之前，先给首页、物料列表、新增物料的表单各截一张图，保存为 `docs/process/screenshots/ui-before-1.png`、`ui-before-2.png`、`ui-before-3.png`。做完后再各截一张，保存为 `ui-after-1.png` 到 `ui-after-3.png`。这是给文档和 PPT 用的对比材料。

### 要实现的（AI 读）

新建 `src/theme.ts`；修改 `src/App.vue`、`src/layouts/BasicLayout.vue`、`src/pages/HomePage.vue`、`src/components/ModelTable.vue`，以及需要统一样式的其它页面。

**服务按钮（改 `ModelTable.vue`）**

1. 从模型定义里取出 `trigger` 为 `manual`、并且 `enabled` 不是 `false` 的服务。
2. `scope` 为 `model` 的，显示成工具栏上的按钮，放在“新增”的左边。`scope` 为 `record` 或者没填的，显示成每行操作列里的按钮，放在“编辑”的前面。按钮文字用服务的 `displayName`。
3. 点击后调用 `callService(model, 服务标识, { id: 这一行的 id })`；工具栏上的按钮不传 `id`。执行期间按钮显示加载状态。
4. 成功后用 `a-modal` 显示结果，标题是服务的显示名：返回值是字符串或数字时用大号字显示，并提供“复制”按钮；是对象或数组时用 `<pre>` 显示格式化的 JSON；是 `null` 或 `undefined` 时显示“执行成功”。关闭弹窗后刷新列表。

**主题**

5. 新建 `src/theme.ts`，导出一个 Ant Design Vue 的主题配置对象，全站的颜色、圆角、字号只在这里定义。建议的取值：主色 `#3B5BDB`，圆角 8，字号 14。在 `App.vue` 的 `a-config-provider` 上用 `:theme` 属性应用它。
6. 布局和页面里手写的颜色，例如 `#fff`、`#f0f0f0`，改成从主题取值（用 `theme.useToken()`），不要散落在各个文件里。

**布局**

7. 侧边栏改成深色：`a-layout-sider` 和 `a-menu` 都用 `theme="dark"`，侧边栏可以折叠。
8. 左上角放一个简单的标志（用内联 SVG 画，例如一个六边形里一个字母 M）和产品名“MetaForge 元铸”。侧边栏折叠时只显示标志。
9. 内容区背景用浅灰（例如 `#F5F7FA`），页面内容放在白色卡片里。

**首页**

10. 首页改成概览：
    - 一行欢迎语，说明这是什么平台。
    - 三个统计卡片：模块数（`metaApi.modules()`）、模型数（`metaApi.models()`）、字段总数（所有模型的字段数之和）。
    - “快捷入口”：每个显示在菜单里的模型一张小卡片，写着模型的显示名和说明，点击进入它的列表页。
    - 原来的“环境检查”卡片保留，放在最下面。原来“这是项目骨架”那张卡片删掉。

**列表页和表单页**

11. `ModelTable` 的搜索区和表格各放在一张白色卡片里，上下间距 16。浏览器宽度 1366 像素时，页面不能出现横向滚动条。

### 你来验收（人做）

1. “编码规则”列表每一行都有“生成编码”按钮。点一下：弹窗的标题是“生成编码”，内容是一段文字（现在用的是假数据，所以是一句说明；接到真实后端后会是一个编码）。关掉弹窗后列表自动刷新。
2. 打开 `src/theme.ts`，把主色改成 `#E03131` 保存：页面上的主按钮、菜单高亮、链接都变成红色。验完改回去。
3. 侧边栏是深色的，左上角有标志和“MetaForge 元铸”。点折叠按钮，侧边栏变窄，只剩图标。
4. 首页有三个统计数字：模块数 2、模型数 4，还有字段总数。“快捷入口”里有物料、产品、编码规则，点任意一个能进入它的列表页。
5. 把浏览器窗口调到 1366 像素宽，物料列表页没有横向滚动条。
6. 按 F12 看控制台，没有红色报错（那条 502 除外）。
7. 6 张对比截图已经保存到 `docs/process/screenshots/`。

### 要弄懂的问题

- 为什么主色只在 `src/theme.ts` 一个地方定义？如果每个页面各写各的颜色，以后想换一套配色要改多少地方？

---

## F7 AI 助手页（加分项）

组长通知“后端的 AI 接口做好了”之后再做。这个任务要连真实后端才能验收，到时候和组长一起做。

### 要实现的（AI 读）

新建 `src/pages/design/AiAssistantPage.vue`；修改 `src/router/index.ts`、`src/layouts/BasicLayout.vue`。

1. 加一条固定路由 `/design/ai`，`meta.title` 是“AI 助手”。“平台设计”分组里加一项“AI 助手”。
2. 页面上方是一个多行输入框，占位文字“用一句话描述你要管理的东西”，旁边有“生成”按钮。输入框下面放两三个示例，点一下就填进输入框，例如“我要管理供应商，记录名称、联系人、电话和信用等级”。
3. 点“生成”调用 `generateByAi(描述)`，期间显示加载状态。大模型比较慢，可能要等十几秒。
4. 返回的是一个元模型包（`MetaBundle`）。分两个标签页展示：
   - “预览”：列出每个模块；列出每个模型的显示名、标识、所属模块，以及它的字段表（显示名、标识、类型、是否必填）。
   - “JSON”：一个可编辑的多行文本框，内容是格式化的 JSON。用户可以在生效前手工修改。
5. 页面下方有“应用到平台”按钮。点击后把 JSON 文本框里的内容解析出来，调用 `metaApi.importBundle`。解析失败提示“不是合法的 JSON”。成功后提示导入了几个模型，调用 `useMenuStore().reload()` 和 `useModelStore().invalidate()`。
6. **点“生成”只展示结果，不调用导入接口。** 只有用户点了“应用到平台”才生效。
7. 接口返回错误时（例如后端没有配置大模型），全局提示已经显示了原因，页面恢复到可以再次生成的状态。

### 你来验收（人做）

1. “平台设计”下面有“AI 助手”。
2. 输入一句话点“生成”：等一会儿后出现预览，能看到模型和字段。这时左侧菜单没有变化。
3. 点“应用到平台”：提示成功，左侧菜单出现新模型，点进去能新增、查询。
4. 让组长把后端的大模型配置去掉再试：页面弹出清楚的提示，不是白屏。

### 要弄懂的问题

- 为什么 AI 生成的结果不直接生效，而要人点一下“应用到平台”？

---

## F8 收尾（11/16 之后）

没有新功能，是下面几件事：

1. 组长在 PR 里提的意见，原样发给 AI 修改，改完重新提交。
2. 把学习日志补完整，每个任务都要有几句自己的话。
3. 对 AI 说：“用三段话分别说明这个前端的动态路由、通用页面、字段组件注册表是怎么实现的，每段配一小段关键代码。”把结果保存为 `docs/dev-doc/04-项目实现-前端.md` 并提交。这是项目开发文档里你负责的那一章。
4. 把 6 张界面对比截图和设计器的截图发给负责文档的同学。

---

# 第四部分：附录（AI 读，人不用看）

下面是接口契约和现有代码的副本。如果你能读取仓库文件，以仓库里的为准。

## 接口契约

{{include: docs/contract/README.md}}

{{include: docs/contract/meta-model.md}}

{{include: docs/contract/api.md}}

## 示例元模型

mock 里的模型定义和它相同。

{{include: docs/contract/samples/mes.json}}

## 现有代码

依赖和版本：

{{include: frontend/package.json}}

类型定义，不要修改：

{{include: frontend/src/types/meta.ts}}

{{include: frontend/src/types/api.ts}}

接口函数，不要修改：

{{include: frontend/src/api/request.ts}}

{{include: frontend/src/api/meta.ts}}

{{include: frontend/src/api/data.ts}}

入口、路由、布局和首页，任务里会修改它们。下面是任务开始之前的样子；如果前面的任务已经改过，以改过的为准：

{{include: frontend/src/main.ts}}

{{include: frontend/src/App.vue}}

{{include: frontend/src/router/index.ts}}

{{include: frontend/src/layouts/BasicLayout.vue}}

{{include: frontend/src/pages/HomePage.vue}}

## 决策记录模板

{{include: docs/decisions/000-template.md}}
