# 决策 006 试做：AviatorScript

## 结论状态

试做完成后由组长选择；如果最终不选 Aviator，这个分支保留，不删除。

## 两种调用方式

### 方式 A：反射调用（`generate.av`）

```javascript
let rule = get(db, "code_rule", params.ruleId);
...
```

需要在 Java 侧：

```java
AviatorEvaluator.setFunctionMissing(JavaMethodReflectionFunctionMissing.getInstance());
```

Aviator 的实例方法写法是 `method(instance, args)`，不是 `instance.method(args)`。
实测：能跑通，但开启 sandbox 后反射被拦住：

```text
aviator.sandbox=BLOCKED FunctionNotFoundException: Function not found: get
```

### 方式 B：受控函数（`generate-functions.av`，推荐）

B5 在 Java 侧注册三个函数，脚本仍然写 `db.get(...)`：

```javascript
let rule = db.get("code_rule", params.ruleId);
let serial = db.increment("code_rule", rule.id, "current_serial");
let digits = "" + serial;
while (string.length(digits) < rule.serial_digits) {
  digits = "0" + digits;
}
return rule.prefix + db.today(rule.date_format) + digits;
```

实测（开启 sandbox）：

```text
sandbox.function.first=MAT-202611200001
sandbox.function.second=MAT-202611200002
```

## 试做输出

```text
aviator.first=MAT-202611200001
aviator.second=MAT-202611200002
aviator.cold2_ms=11
aviator.compile_ms=0
aviator.eval1000_ms=38
aviator.sandbox=BLOCKED FunctionNotFoundException: Function not found: get
sandbox.function.first=MAT-202611200001
sandbox.function.second=MAT-202611200002
```

- 依赖：`com.googlecode.aviator:aviator:5.4.4`（0.6 MB）。
- 语法：支持 `while`、`let`、字符串、函数；用受控函数后脚本可读性和 Groovy 接近。
- 沙箱：内置 `enableSandboxMode()`，还有 `EVAL_TIMEOUT_MS` / `MAX_LOOP_COUNT` 等选项；受控函数方式下 sandbox 可用。
- 性能：冷启动约 11 ms，1000 次执行约 38 ms；比 Groovy 轻很多。
- 代价：B5 要写一层“把受控 db 方法注册成 Aviator 函数”的适配代码，不能直接把任意 Java 对象丢给脚本。
