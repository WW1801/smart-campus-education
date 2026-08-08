package com.campus.education.common;

/**
 * 统一返回结果类，封装接口响应的标准结构。
 */

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    // 处理结果
    private Result() {}

    // 返回成功结果
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    // 返回成功结果
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 返回成功结果
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // 返回错误结果
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 返回包含业务失败明细的错误结果
    public static <T> Result<T> error(int code, String message, T data) {
        Result<T> result = error(code, message);
        result.setData(data);
        return result;
    }

    // 返回错误结果
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    // 返回未授权结果
    public static <T> Result<T> unauthorized(String message) {
        return error(401, message);
    }

    // 返回禁止访问结果
    public static <T> Result<T> forbidden(String message) {
        return error(403, message);
    }

    // 返回错误请求结果
    public static <T> Result<T> badRequest(String message) {
        return error(400, message);
    }
}
