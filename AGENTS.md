# 给 AI 编程工具的项目说明

这是一个三人小组的课程作品：模型驱动的低代码平台 MetaForge。组员各自用 AI 工具辅助开发，下面是所有人和所有工具都要遵守的约定。

## 开始任何任务之前

1. 读 `docs/contract/README.md`、`docs/contract/meta-model.md`、`docs/contract/api.md`。前后端只通过这份契约对接。
2. 读当前任务的说明，按其中的要求做，以其中的验收标准为完成的标准。后端任务在 `docs/tasks/backend.md`；前端和文档任务在 `docs/handoff/` 下的两份任务包里。

## 必须遵守

- 不要擅自修改 `docs/contract/`。实现时发现契约有问题，停下来告诉用户，由用户按契约的修改规则处理。
- 不要把任何业务模型写死在平台代码里。除了 `frontend/src/mock` 和 `docs/contract/samples`，代码里不应该出现 `material`、`product` 这类具体的模型名或字段名。
- 后端：表名和列名只能来自校验过的元模型，值一律用参数绑定，不要把请求里的字符串拼进 SQL。
- 后端：出错时抛 `com.metaforge.common.BizException`，不要自己拼错误响应。
- 前端：调接口只用 `src/api/` 里已有的函数，类型只用 `src/types/` 里已有的定义，不要另写 axios 实例。
- 不要把密码、API key 写进会被提交的文件。

## 技术栈

- 后端：Spring Boot 4.0.5、Java 21、MySQL、`NamedParameterJdbcTemplate`。不引入 MyBatis、JPA、Lombok。Spring Boot 4 用 Jackson 3，`ObjectMapper` 在 `tools.jackson.databind` 包里。
- 前端：Vue 3 组合式 API 加 `<script setup lang="ts">`、Ant Design Vue 4、Pinia、Vue Router、Vite。

## 怎么回答

组员需要理解并能讲清楚每个设计，老师会问“为什么这样做”。所以：

- 给出方案时，同时说明至少一个备选方案，以及为什么没选它。
- 涉及 `docs/decisions/README.md` 里列出的问题时，提醒用户这是一个要写决策记录的问题，先让用户自己做选择。
- 不要一次生成大量用户没看过的代码。按任务说明里的步骤一步一步来，每一步说明做了什么。
- 代码注释和提交信息用中文。
