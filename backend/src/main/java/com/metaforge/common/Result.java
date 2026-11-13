package com.metaforge.common;

/**
 * 所有接口统一的响应格式，见 docs/contract/README.md。
 * 成功时 code 为 0；失败时 code 是错误码，HTTP 状态码同时表示错误大类。
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, message, data);
    }
}
