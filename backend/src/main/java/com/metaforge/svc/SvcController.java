package com.metaforge.svc;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metaforge.common.Result;

/**
 * 手动服务接口，对应 docs/contract/api.md 的“服务接口”。
 * 只允许调用 trigger 为 manual 的服务；其它 trigger 在 ServiceRunner 里返回 404。
 */
@RestController
@RequestMapping("/api/svc/{model}")
public class SvcController {

    private final ServiceRunner runner;

    public SvcController(ServiceRunner runner) {
        this.runner = runner;
    }

    @PostMapping("/{service}")
    public Result<Object> call(@PathVariable String model, @PathVariable String service,
            @RequestBody(required = false) ServiceCall body) {
        Long id = body == null ? null : body.id();
        return Result.ok(runner.runManual(model, service, id, body == null ? null : body.params()));
    }
}
