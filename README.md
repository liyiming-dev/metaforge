# MetaForge 元铸

一个模型驱动的低代码平台：开发者在页面上定义模型、字段、视图和服务，平台据此建表、提供增删改查接口、渲染列表和表单，改了定义不用重启。这是《中级软件设计实作》课题 4 的小组作品，平台上搭了一个小型 MES 作为示例。

目前是项目骨架：后端和前端都能启动并互相连通，业务功能还没有开始写。

## 目录

| 目录 | 内容 |
|---|---|
| `backend/` | 后端：Spring Boot 4、Java 21、MySQL，用 JdbcTemplate 访问数据库 |
| `frontend/` | 前端：Vue 3、TypeScript、Ant Design Vue、Vite |
| `docs/contract/` | 前后端的接口契约和示例元模型 |
| `docs/tasks/` | 任务总览、时间表、后端任务的步骤和验收标准 |
| `docs/handoff/` | 发给前端和文档同学的任务包，各一个文件，可以整个交给 AI 照着做 |
| `docs/decisions/` | 问题清单和每个设计问题的决策记录 |
| `docs/process/` | 讨论截图、联调纪要、每个人的学习日志 |

## 怎么跑起来

完整步骤和常见问题见 [docs/tasks/00-setup.md](docs/tasks/00-setup.md)。简要版：

1. 把 `backend/src/main/resources/application-local.example.yml` 复制成 `application-local.yml`，填本机 MySQL 的账号密码。
2. 在 `backend` 目录下启动后端，地址是 <http://localhost:8200>：

   ```bash
   .\mvnw.cmd spring-boot:run
   ```

3. 在 `frontend` 目录下安装依赖并启动前端，地址是 <http://localhost:5173>：

   ```bash
   npm install
   ```

   ```bash
   npm run dev
   ```

首页的“环境检查”显示“后端连接正常”就说明都通了。

## 从哪里开始看

- 要做什么：[docs/tasks/README.md](docs/tasks/README.md)
- 前后端怎么对接：[docs/contract/README.md](docs/contract/README.md)
- 怎么提交代码：[CONTRIBUTING.md](CONTRIBUTING.md)
- 怎么留过程记录：[docs/process/README.md](docs/process/README.md)
