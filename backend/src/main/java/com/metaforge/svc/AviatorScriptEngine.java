package com.metaforge.svc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.Options;
import com.googlecode.aviator.runtime.function.AbstractVariadicFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import com.metaforge.common.BizException;

/**
 * AviatorScript 5.4.4 实现。
 *
 * 安全边界：
 * - 开 sandbox，脚本不能 new 任意类、读文件或反射；
 * - 设置执行超时和循环上限，死循环不会把请求线程一直占住；
 * - 只注册 db.* 这几个受控函数，不把 JdbcTemplate 或任意 Java 对象放进环境。
 *
 * 编译结果按脚本原文缓存；脚本改了就是新的 key，所以改脚本不用重启。
 */
@Component
public class AviatorScriptEngine implements ScriptEngine {

    /** 单次脚本执行上限，够生成编码这类短脚本用；可在 B5 验收时按需调整。 */
    private static final long TIMEOUT_MS = 2000L;
    private static final long MAX_LOOP_COUNT = 10000L;

    private final AviatorEvaluatorInstance evaluator;
    private final Map<String, Expression> compiled = new ConcurrentHashMap<>();

    public AviatorScriptEngine() {
        this.evaluator = AviatorEvaluator.newInstance();
        this.evaluator.enableSandboxMode();
        this.evaluator.setOption(Options.EVAL_TIMEOUT_MS, TIMEOUT_MS);
        this.evaluator.setOption(Options.MAX_LOOP_COUNT, MAX_LOOP_COUNT);
        this.evaluator.addFunction(new DbGet());
        this.evaluator.addFunction(new DbFind());
        this.evaluator.addFunction(new DbUpdate());
        this.evaluator.addFunction(new DbIncrement());
        this.evaluator.addFunction(new DbToday());
        this.evaluator.addFunction(new DbCall());
    }

    @Override
    public Object execute(String script, ScriptContext context) {
        if (script == null || script.isBlank()) {
            throw BizException.badRequest("服务脚本不能为空");
        }
        Map<String, Object> env = new HashMap<>();
        env.put("record", context.record());
        env.put("params", context.params());
        env.put("db", context.db());
        Expression expression = compiled.computeIfAbsent(script, text -> evaluator.compile(text, true));
        return expression.execute(env);
    }

    private abstract static class DbFunction extends AbstractVariadicFunction {

        protected static ScriptDb db(Map<String, Object> env) {
            Object db = env.get("db");
            if (!(db instanceof ScriptDb scriptDb)) {
                throw BizException.badRequest("脚本里没有可用的 db");
            }
            return scriptDb;
        }

        protected static Object value(AviatorObject arg, Map<String, Object> env) {
            if (arg == null || arg == AviatorNil.NIL) {
                return null;
            }
            return arg.getValue(env);
        }

        protected String text(AviatorObject arg, Map<String, Object> env) {
            Object value = value(arg, env);
            if (value == null) {
                throw BizException.badRequest(getName() + " 的参数不能为空");
            }
            return String.valueOf(value);
        }

        protected static AviatorObject result(Object value) {
            return value == null ? AviatorNil.NIL : AviatorRuntimeJavaType.valueOf(value);
        }

        protected void expect(String name, AviatorObject[] args, int... allowed) {
            for (int count : allowed) {
                if (args.length == count) {
                    return;
                }
            }
            throw BizException.badRequest(name + " 的参数个数不对");
        }
    }

    private static final class DbGet extends DbFunction {
        @Override
        public String getName() {
            return "db.get";
        }

        @Override
        public AviatorObject variadicCall(Map<String, Object> env, AviatorObject... args) {
            expect(getName(), args, 2);
            return result(db(env).get(text(args[0], env), value(args[1], env)));
        }
    }

    private static final class DbFind extends DbFunction {
        @Override
        public String getName() {
            return "db.find";
        }

        @Override
        public AviatorObject variadicCall(Map<String, Object> env, AviatorObject... args) {
            expect(getName(), args, 3);
            return result(db(env).find(text(args[0], env), text(args[1], env), value(args[2], env)));
        }
    }

    private static final class DbUpdate extends DbFunction {
        @Override
        public String getName() {
            return "db.update";
        }

        @Override
        public AviatorObject variadicCall(Map<String, Object> env, AviatorObject... args) {
            expect(getName(), args, 3);
            return result(db(env).update(text(args[0], env), value(args[1], env), value(args[2], env)));
        }
    }

    private static final class DbIncrement extends DbFunction {
        @Override
        public String getName() {
            return "db.increment";
        }

        @Override
        public AviatorObject variadicCall(Map<String, Object> env, AviatorObject... args) {
            expect(getName(), args, 3);
            return result(db(env).increment(text(args[0], env), value(args[1], env), text(args[2], env)));
        }
    }

    private static final class DbToday extends DbFunction {
        @Override
        public String getName() {
            return "db.today";
        }

        @Override
        public AviatorObject variadicCall(Map<String, Object> env, AviatorObject... args) {
            expect(getName(), args, 1);
            return result(db(env).today(text(args[0], env)));
        }
    }

    private static final class DbCall extends DbFunction {
        @Override
        public String getName() {
            return "db.call";
        }

        @Override
        public AviatorObject variadicCall(Map<String, Object> env, AviatorObject... args) {
            expect(getName(), args, 3, 4);
            Object params = args.length == 4 ? value(args[3], env) : Map.of();
            return result(db(env).call(text(args[0], env), text(args[1], env), value(args[2], env), params));
        }
    }
}
