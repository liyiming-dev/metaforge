package com.metaforge.common;

import java.util.Map;

/**
 * 业务错误。在任何地方抛出它，GlobalExceptionHandler 会把它变成契约规定的错误响应。
 * fieldErrors 的键是字段名，值是给用户看的提示，前端会显示在对应的表单项下面。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> fieldErrors;

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public BizException(ErrorCode errorCode, String message, Map<String, String> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public static BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
