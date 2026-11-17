package com.metaforge;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

/** 测试用的帮手：查看和清理业务表（md_ 开头）以及备份表（zz_ 开头）。只在内存数据库上用。 */
public final class TestTables {

    private TestTables() {
    }

    /** 当前有哪些业务表和备份表。内存数据库把表名存成大写，这里统一转成小写再返回。 */
    public static List<String> businessTables(JdbcTemplate jdbc) {
        return jdbc.queryForList(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'", String.class)
                .stream()
                .map(name -> name.toLowerCase(java.util.Locale.ROOT))
                .filter(name -> name.startsWith("md_") || name.startsWith("zz_"))
                .sorted()
                .toList();
    }

    /** 把业务表和备份表全部删掉，让每个测试都从干净的数据库开始。 */
    public static void dropBusinessTables(JdbcTemplate jdbc) {
        for (String table : businessTables(jdbc)) {
            jdbc.execute("DROP TABLE `" + table + "`");
        }
    }
}
