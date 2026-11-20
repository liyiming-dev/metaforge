package com.metaforge.svc;

/**
 * 脚本引擎接口。业务代码只依赖它，换引擎时只改实现和配置。
 * 输入是脚本文本和上下文，输出是脚本的返回值。
 */
public interface ScriptEngine {

    Object execute(String script, ScriptContext context);
}
