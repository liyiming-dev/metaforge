package com.metaforge.schema;

/**
 * 改表时要执行的一步。除了 SQL，还带着“这一步是在做什么、针对哪个字段”，
 * 这样执行失败时能给出看得懂的提示，例如“字段 code 已有重复的值，不能设为唯一”。
 */
public record DdlStep(Kind kind, String field, String sql) {

    public enum Kind {
        /** 改已有列的长度或精度 */
        MODIFY_COLUMN,
        /** 给列加唯一约束 */
        ADD_UNIQUE,
        /** 去掉列的唯一约束 */
        DROP_UNIQUE,
        /** 加一列 */
        ADD_COLUMN,
        /** 字段被删除：把列改名留作备份 */
        BACKUP_COLUMN
    }
}
