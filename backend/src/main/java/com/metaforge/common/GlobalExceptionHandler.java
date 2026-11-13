package com.metaforge.common;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 把所有异常统一变成契约规定的错误响应格式。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Object>> handleBiz(BizException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Object data = ex.getFieldErrors().isEmpty() ? null : Map.of("fieldErrors", ex.getFieldErrors());
        return respond(errorCode.httpStatus(), errorCode.code(), ex.getMessage(), data);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(400, ErrorCode.BAD_REQUEST.code(), "请求体不是合法的 JSON", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleOther(Exception ex) {
        // Spring MVC 自己的异常（地址不存在、请求方法不对等）都实现了 ErrorResponse，自带 HTTP 状态码
        if (ex instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            return respond(status, status * 100, errorResponse.getBody().getDetail(), null);
        }
        log.error("未处理的异常", ex);
        return respond(500, ErrorCode.INTERNAL.code(), "系统错误，请查看后端日志", null);
    }

    private static ResponseEntity<Result<Object>> respond(int httpStatus, int code, String message, Object data) {
        return ResponseEntity.status(httpStatus).body(Result.fail(code, message, data));
    }
}
