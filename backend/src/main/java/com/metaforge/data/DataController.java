package com.metaforge.data;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.metaforge.common.Result;

/**
 * 通用数据接口，和 docs/contract/api.md 的“数据接口”一节一一对应。
 *
 * {model} 是路径变量：同一个控制器服务所有模型，模型名去 MetaRegistry 里查，
 * 所以新建一个模型之后不重启就能调它的接口。这就是课题要求的“动态生成 api”，
 * 另一个做法（运行时给每个模型注册一组新的映射）的对比见决策记录 003。
 *
 * 这里只做转发，校验和拼 SQL 在 DataService、SqlBuilder 里。
 */
@RestController
@RequestMapping("/api/data/{model}")
public class DataController {

    private final DataService service;

    public DataController(DataService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public Result<SearchResult> search(@PathVariable String model,
            @RequestBody(required = false) SearchRequest request) {
        return Result.ok(service.search(model, request));
    }

    @GetMapping("/options")
    public Result<List<OptionItem>> options(@PathVariable String model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String displayField) {
        return Result.ok(service.options(model, keyword, limit, displayField));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable String model, @PathVariable String id) {
        return Result.ok(service.get(model, id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@PathVariable String model,
            @RequestBody(required = false) Map<String, Object> body) {
        return Result.ok(service.create(model, body));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable String model, @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {
        return Result.ok(service.update(model, id, body));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String model, @PathVariable String id) {
        service.delete(model, id);
        return Result.ok();
    }
}
