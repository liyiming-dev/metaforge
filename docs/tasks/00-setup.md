# 完整环境准备

目标：在一台电脑上把后端和前端都跑起来。预计 40 分钟。

谁需要做：

- 组长现在就做，第 0 步建远程仓库要最先完成，否则组员拿不到代码。
- 前端和文档同学这个周末不用做这一份。他们的任务包里有更简单的准备步骤：前端只需要 Node.js，文档只需要 Git。联调在组长的电脑上进行。以后他们需要在自己电脑上运行后端时，再回来做第 1 步和第 4 步。

## 第 0 步（只有组长做）：建远程仓库

1. 在 GitHub 上新建一个空仓库，名字 `metaforge`，可见性选 Private，不要勾选自动生成 README、.gitignore 和 License。
2. 在本地项目目录里关联并推送：

   ```bash
   git remote add origin https://github.com/你的用户名/metaforge.git
   ```

   ```bash
   git push -u origin main
   ```

3. 在仓库的 Settings → Collaborators 里邀请另外两位组员，让他们接受邀请。
4. 可选：在 Settings → Branches 里给 `main` 加一条规则，要求合并前必须有 PR。免费账号的私有仓库加不了这种规则；加不了就靠约定，所有人都不直接往 `main` 推。

## 第 1 步：装软件

| 软件 | 版本要求 | 怎么确认 |
|---|---|---|
| Git | 任意较新版本 | `git --version` |
| JDK | 21 或更高 | `java -version` |
| Node.js | 20.19 以上，或 22.12 以上 | `node -v` |
| MySQL | 8 或更高，课上装的那个就行 | 能用客户端连上 `127.0.0.1:3306` |
| 开发工具 | 后端用 IntelliJ IDEA，前端用 VS Code 或 IDEA 都可以 | |

不用单独装 Maven，项目自带了 Maven Wrapper（`mvnw.cmd`）。

## 第 2 步：配置 Git 身份

提交记录是证明每个人都参与了的主要材料。Git 里的邮箱必须和你 GitHub 账号的邮箱一致，否则 GitHub 上你的提交不会算到你头上。

```bash
git config --global user.name "你的名字"
```

```bash
git config --global user.email "你注册 GitHub 用的邮箱"
```

## 第 3 步：下载代码

```bash
git clone https://github.com/组长的用户名/metaforge.git
```

## 第 4 步：启动后端

1. 进入 `backend/src/main/resources/`，把 `application-local.example.yml` 复制一份，改名为 `application-local.yml`，填上你本机 MySQL 的账号和密码。这个文件不会被提交。
2. 在 `backend` 目录下启动：

   ```bash
   .\mvnw.cmd spring-boot:run
   ```

   第一次会下载依赖，要等几分钟。也可以用 IDEA 打开 `backend/pom.xml`，运行 `MetaforgeApplication`。
3. 浏览器打开 <http://localhost:8200/api/health>，看到下面这样的内容就对了：

   ```json
   { "code": 0, "message": "ok", "data": { "status": "UP", "database": "MySQL 8.0.36", "time": "..." } }
   ```

不用手工建数据库，第一次启动时会自动创建名为 `metaforge` 的库。

## 第 5 步：启动前端

后端保持运行，另开一个终端，在 `frontend` 目录下执行：

```bash
npm install
```

```bash
npm run dev
```

浏览器打开 <http://localhost:5173>。

## 验收标准

- [ ] 首页“环境检查”里，“后端连接”显示绿色的“正常”，后面带着数据库版本。
- [ ] “页面数据来源”显示“前端内存 mock（示例里有 4 个模型）”。
- [ ] 把后端停掉，点“重新检查”，变成红色的“连不上”。再启动后端，点“重新检查”，恢复绿色。
- [ ] 把首页截图发到群里。

## 第 6 步：完成第一次提交

这一步是为了确认每个人都能提交和提 PR，同时建好自己的学习日志。

1. 新建分支：

   ```bash
   git checkout -b docs/journal-你的名字拼音
   ```

2. 把 `docs/process/journal/TEMPLATE.md` 复制一份，改名为 `你的名字拼音.md`，写上今天的第一条：环境准备花了多久、遇到了什么问题、怎么解决的。
3. 提交并推送：

   ```bash
   git add docs/process/journal
   ```

   ```bash
   git commit -m "docs: 新建学习日志"
   ```

   ```bash
   git push -u origin docs/journal-你的名字拼音
   ```

4. 在 GitHub 上对这个分支提 PR，等组长合并。

验收：GitHub 仓库的 Insights → Contributors 里能看到你的头像。看不到就是第 2 步的邮箱没配对。

## 常见问题

| 现象 | 原因和处理 |
|---|---|
| 健康检查返回“连不上数据库：Access denied” | `application-local.yml` 没建，或者密码不对 |
| 健康检查返回“连不上数据库：Communications link failure” | MySQL 没启动 |
| 启动时报 `Port 8200 was already in use` | 上一次的后端没关。关掉它，或者在任务管理器里结束 `java.exe` |
| 启动时报 `Unable to establish loopback connection` | 个别 Windows 电脑的系统临时目录有问题。启动类里已经做了规避，正常不会出现；还出现的话，在启动命令或 IDEA 的 VM options 里加 `-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp` |
| `mvnw.cmd` 下载 Maven 失败 | 用 IDEA 打开 `backend/pom.xml`，用 IDEA 自带的 Maven |
| `java -version` 低于 21 | 装 JDK 21，并确认 `JAVA_HOME` 或 IDEA 的项目 SDK 指向它 |
| `npm install` 很慢或失败 | 项目里的 `.npmrc` 已经指向国内镜像。确认 Node 版本符合要求，删掉 `node_modules` 重试 |
| 首页“后端连接”一直红色，但直接访问 8200 正常 | 前端开发服务器没重启，或者 5173 端口被别的项目占了，看终端里实际的端口号 |
