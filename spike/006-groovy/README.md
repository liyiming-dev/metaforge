# 决策 006 试做：Groovy

## 结论状态

试做完成后由组长选择；如果最终不选 Groovy，这个分支保留，不删除。

## 脚本

`generate.groovy`，用 Groovy 4.0.33 + JSR-223：

```groovy
def rule = db.get('code_rule', params.ruleId)
def serial = db.increment('code_rule', rule.id, 'current_serial')
def digits = serial.toString()
while (digits.length() < rule.serial_digits) {
    digits = '0' + digits
}
return rule.prefix + db.today(rule.date_format) + digits
```

## 试做输出

在临时 Maven 工程里用同一个 `Db` 替身跑通：

```text
groovy.first=MAT-202611200001
groovy.second=MAT-202611200002
groovy.cold2_ms=855
groovy.compile_ms=12
groovy.eval1000_ms=187
```

- 依赖：`org.apache.groovy:groovy:4.0.33`（7.4 MB）+ `groovy-jsr223:4.0.33`（很小）。
- 语法：Java 风格，`db.get(...)`、`while`、字符串拼接都直接可用。
- 沙箱：Groovy 有 `SecureASTCustomizer` / `SandboxTransformer`，但需要额外配置；JSR-223 本身没有执行超时，死循环要自己用线程/Executor 兜。
- 如果选 Groovy，B5 的 `ScriptEngine` 实现里要显式配置沙箱和超时，不能直接把 `GroovyShell` 暴露给用户脚本。
