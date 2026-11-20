# 决策 006 试做：AviatorScript

## 结论状态

试做完成后由组长选择；如果最终不选 Aviator，这个分支保留，不删除。

## 脚本

`generate.av`，用 AviatorScript 5.4.4：

```javascript
let rule = get(db, "code_rule", params.ruleId);
let serial = increment(db, "code_rule", rule.id, "current_serial");
let digits = "" + serial;
while (string.length(digits) < rule.serial_digits) {
  digits = "0" + digits;
}
return rule.prefix + today(db, rule.date_format) + digits;
```

调用前需要：

```java
AviatorEvaluator.setFunctionMissing(JavaMethodReflectionFunctionMissing.getInstance());
```

Aviator 的实例方法调用写法是 `method(instance, args)`，不是 `instance.method(args)`；
`get(db, ...)` 会被反射成 `db.get(...)`。

## 试做输出

```text
aviator.first=MAT-202611200001
aviator.second=MAT-202611200002
aviator.cold2_ms=11
aviator.compile_ms=0
aviator.eval1000_ms=38
aviator.sandbox=BLOCKED FunctionNotFoundException: Function not found: get
```

- 依赖：`com.googlecode.aviator:aviator:5.4.4`（0.6 MB）。
- 语法：支持 `while`、`let`、字符串、函数；但 Java 对象方法要按 `method(obj, args)` 调用，读起来不如 Groovy 自然。
- 沙箱：内置 `enableSandboxMode()`，还有 `EVAL_TIMEOUT_MS` / `MAX_LOOP_COUNT` 等选项；但开启 sandbox 后我们试的反射调用 `get` 被拦住。B5 如果选 Aviator，建议把 `db.get/increment/today` 注册成受控的 Aviator 函数，而不是用反射。
- 性能：冷启动和 1000 次执行都明显快于 Groovy；对“人点按钮”的场景不是决定性因素，但沙箱和包大小是优势。
