package com.campus.education.common;

/**
 * 业务异常类，封装系统中的业务校验失败场景。
 */

public class BusinessException extends RuntimeException {
    private final int code;

    // 处理业务异常
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    // 处理业务异常
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    // 获取状态码
    public int getCode() {
        return code;
    }
}
