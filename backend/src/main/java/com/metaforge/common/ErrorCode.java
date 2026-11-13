package com.metaforge.common;

/** 错误码和对应的 HTTP 状态码，和 docs/contract/README.md 里的表保持一致。 */
public enum ErrorCode {

    /** 请求参数或数据校验不通过 */
    BAD_REQUEST(40000, 400),
    /** 模型、记录或服务不存在 */
    NOT_FOUND(40400, 404),
    /** 冲突：名称重复、违反唯一约束、数据被别处引用 */
    CONFLICT(40900, 409),
    /** 系统错误 */
    INTERNAL(50000, 500);

    private final int code;
    private final int httpStatus;

    ErrorCode(int code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
