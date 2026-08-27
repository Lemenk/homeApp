package com.familyhome.common;

import lombok.Getter;

/**
 * 业务异常。code 语义：
 * 400 参数错误 / 401 未认证 / 403 无权限 / 404 不存在 / 409 冲突 / 500 服务器内部
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }

    public static BizException unauthorized(String msg) {
        return new BizException(401, msg);
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }

    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    public static BizException conflict(String msg) {
        return new BizException(409, msg);
    }

    public static BizException server(String msg) {
        return new BizException(500, msg);
    }
}
