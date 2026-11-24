package com.metaforge.svc;

import java.util.Map;

/**
 * 一次脚本执行的上下文。
 * record 是当前记录（前置钩子可以改），params 是调用方参数，db 是受控的数据访问入口。
 */
public record ScriptContext(
        Map<String, Object> record,
        Map<String, Object> params,
        ScriptDb db) {
}
