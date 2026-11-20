package com.metaforge.svc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.metaforge.common.BizException;
import com.metaforge.data.DataService;
import com.metaforge.meta.MetaRegistry;
import com.metaforge.meta.model.ModelDef;
import com.metaforge.meta.model.ServiceDef;
import com.metaforge.meta.model.ServiceTriggers;

/**
 * 执行模型上定义的脚本服务：
 * - manual：/api/svc 或别的脚本通过 db.call 调用；
 * - 生命周期钩子：DataService 在写库前后调用 runHooks。
 *
 * 事务：manual 入口带 @Transactional；钩子运行在 DataService 的写事务里。
 * db.call 走 runManualInternal，不再开新事务，和当前操作同生共死。
 */
@Service
public class ServiceRunner {

    private final MetaRegistry registry;
    private final ScriptEngine scripts;
    private final DataService data;

    /** 防止服务之间 db.call 形成环；只在当前线程有效。 */
    private final ThreadLocal<Deque<String>> callStack = ThreadLocal.withInitial(ArrayDeque::new);

    public ServiceRunner(MetaRegistry registry, ScriptEngine scripts, DataService data) {
        this.registry = registry;
        this.scripts = scripts;
        this.data = data;
    }

    @Transactional
    public Object runManual(String modelName, String serviceName, Long id, Map<String, Object> params) {
        return runManualInternal(modelName, serviceName, id, params);
    }

    /** 给 db.call 用：不开新事务，保持调用方的事务。 */
    Object runManualInternal(String modelName, String serviceName, Long id, Map<String, Object> params) {
        ModelDef model = registry.requireModel(modelName);
        ServiceDef service = findService(model, serviceName);
        if (service == null || !ServiceTriggers.MANUAL.equals(service.trigger())
                || Boolean.FALSE.equals(service.enabled())) {
            // 不是 manual 的服务一律当作不存在，契约规定 404
            throw BizException.notFound("服务 " + modelName + "/" + serviceName + " 不存在");
        }

        Map<String, Object> record = new LinkedHashMap<>();
        if (!ServiceTriggers.SCOPE_MODEL.equals(service.scope())) {
            if (id == null) {
                throw BizException.badRequest("服务 " + serviceName + " 需要传记录 id");
            }
            record = data.getForScript(modelName, id);
            if (record == null) {
                throw BizException.notFound("记录不存在");
            }
        }
        return execute(model, service, record, params == null ? Map.of() : params);
    }

    /** DataService 的钩子入口；按定义顺序执行所有启用的同 trigger 服务。 */
    public void runHooks(ModelDef model, String trigger, Map<String, Object> record, Map<String, Object> params) {
        if (model.services() == null || model.services().isEmpty()) {
            return;
        }
        Map<String, Object> actualParams = params == null ? Map.of() : params;
        for (ServiceDef service : model.services()) {
            if (service == null || Boolean.FALSE.equals(service.enabled())
                    || !trigger.equals(service.trigger())) {
                continue;
            }
            execute(model, service, record, actualParams);
        }
    }

    private Object execute(ModelDef model, ServiceDef service, Map<String, Object> record,
            Map<String, Object> params) {
        String key = model.name() + "/" + service.name();
        Deque<String> stack = callStack.get();
        if (stack.contains(key)) {
            throw BizException.badRequest("服务调用形成环：" + String.join(" -> ", stack) + " -> " + key);
        }
        stack.push(key);
        try {
            ScriptDb db = new ScriptDb(data, this);
            ScriptContext context = new ScriptContext(record, params, db);
            return scripts.execute(service.script(), context);
        } catch (BizException e) {
            // 保留 400/404/409 和 fieldErrors，只在消息前面补上服务名
            throw new BizException(e.getErrorCode(), "服务 " + key + " 执行出错：" + e.getMessage(),
                    e.getFieldErrors());
        } catch (Throwable e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw BizException.badRequest("服务 " + key + " 执行出错：" + message);
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                callStack.remove();
            }
        }
    }

    private static ServiceDef findService(ModelDef model, String name) {
        if (model.services() == null) {
            return null;
        }
        for (ServiceDef service : model.services()) {
            if (service != null && name.equals(service.name())) {
                return service;
            }
        }
        return null;
    }
}
