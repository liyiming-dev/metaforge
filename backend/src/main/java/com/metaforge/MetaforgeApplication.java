package com.metaforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MetaforgeApplication {

    public static void main(String[] args) {
        useWorkingDirForLoopbackSockets();
        SpringApplication.run(MetaforgeApplication.class, args);
    }

    /**
     * 有的 Windows 电脑上，JDK 在系统临时目录里建本地套接字会失败，Tomcat 因此启动不了，
     * 报 "Unable to establish loopback connection"。把套接字目录改到当前工作目录可以避开。
     * 如果启动命令里已经用 -Djdk.net.unixdomain.tmpdir 指定了目录，这里不覆盖。
     * 这个目录不可写或路径太长时，JDK 会自动退回普通的 TCP 回环连接，不影响启动。
     */
    private static void useWorkingDirForLoopbackSockets() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (windows && System.getProperty("jdk.net.unixdomain.tmpdir") == null) {
            System.setProperty("jdk.net.unixdomain.tmpdir", System.getProperty("user.dir"));
        }
    }
}
