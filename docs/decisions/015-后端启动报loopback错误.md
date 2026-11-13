# 015 后端启动报 Unable to establish loopback connection

- 负责人：后端（组长）
- 关联任务：搭项目骨架
- 状态：已解决
- 最后更新：2026-11-13

这份记录里的现象、排查过程和结果，是 2026-11-13 搭骨架时在组长电脑上实际发生的，由组长使用 Claude Code 排查并记录。“讨论记录”和“反思”两节留给小组补充。

## 问题

后端骨架的单元测试通过，但真正启动时 Tomcat 起不来，接口无法访问。日志里的关键信息：

```
java.io.IOException: Unable to establish loopback connection
Caused by: java.net.SocketException: Invalid argument: connect
    at java.base/sun.nio.ch.UnixDomainSockets.connect0(Native Method)
    at java.base/sun.nio.ch.PipeImpl$Initializer$LoopbackConnector.run(PipeImpl.java:123)
    at java.base/sun.nio.ch.WEPollSelectorImpl.<init>(WEPollSelectorImpl.java:78)
    at org.apache.tomcat.util.net.NioEndpoint$Poller.<init>(NioEndpoint.java:631)
```

## 排查过程

1. 单元测试为什么没发现：测试用的是 MockMvc，不启动真正的 Tomcat，所以不会走到这段代码。
2. 写了一个只有一行 `Selector.open()` 的 Java 小程序来复现。结果：
   - JDK 24 和 JDK 21 都失败，说明不是某个 JDK 版本的问题。
   - 报错位置在 JDK 内部：Windows 上，JDK 创建 NIO 的 `Selector` 时，会在临时目录里建一个本地套接字（Unix domain socket）文件，再自己连接它。在这台电脑上，建文件成功，连接失败。
3. 用 JDK 的参数 `-Djdk.net.unixdomain.tmpdir` 把套接字文件换到别的目录：
   - 换到 `C:\Windows\Temp`、`C:\Users\Public`、项目的 `backend\target`：都成功。
   - 用系统默认的临时目录（`C:\Users\用户名\AppData\Local\Temp`）：失败，不管写成长路径还是短路径。
4. 结论：问题只和这台电脑的用户临时目录有关，和路径里有没有空格、路径长短无关。根本原因没有继续查。

## 候选方案

| 方案 | 怎么做 | 好处 | 代价 |
|---|---|---|---|
| A | 在文档里写明：遇到这个错误就加 JVM 参数 | 不改代码 | 每种启动方式（命令行、IDEA、jar）都要各自配置，容易漏 |
| B | 在 `pom.xml` 的 Spring Boot 插件里配置 JVM 参数 | 只改构建配置 | 只对 `mvnw spring-boot:run` 有效，IDEA 直接运行和 `java -jar` 不生效 |
| C | 在启动类 `main` 方法的最开头，用代码设置这个系统属性，指向当前工作目录 | 任何启动方式都生效，组员不用做任何配置 | 启动类里多了几行和业务无关的代码 |

## 结论

选了 C，代码在 `MetaforgeApplication.useWorkingDirForLoopbackSockets()`。几个细节：

- 只在 Windows 上生效。
- 如果启动命令里已经指定了这个参数，不覆盖。
- 当前工作目录不可写或路径太长时，JDK 会自己退回到普通的 TCP 回环连接，所以这样设置不会让原本正常的电脑出问题。

验证：改完后不加任何 JVM 参数，用 `java -jar` 启动，Tomcat 正常启动，健康检查接口返回 `UP`，后端目录里没有残留的套接字文件。

## 讨论记录

| 次 | 时间 | 参与人 | 要点 | 截图 |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |

## 代价和反思

（待补充。可以想的问题：为什么测试全部通过，程序却启动不了？这对“怎样才算验证过”有什么启发？另外两位组员的电脑上有没有出现同样的问题？）

## AI 在这个问题上的作用

（待补充。事实部分：AI 第一次判断原因是“临时目录路径太长”，打印出临时目录的路径后发现只有 36 个字符，这个判断是错的；之后改为逐个目录做对照实验，才定位到是用户临时目录本身的问题。）
