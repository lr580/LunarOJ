package com.lunaroj.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    BAD_REQUEST(40000, "请求参数错误"),
    VALIDATION_ERROR(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或登录已失效"),
    FORBIDDEN(40300, "无权限访问"),
    REGISTER_DISABLED(40301, "当前暂未开放注册"),
    USER_NOT_FOUND(40401, "用户不存在"),
    USERNAME_EXISTS(40901, "用户名已存在"),
    CAPTCHA_INVALID(40902, "验证码错误或已过期"),
    PASSWORD_INCORRECT(40903, "账号或密码错误"),
    TOKEN_INVALID(40904, "Token 无效"),
    TOKEN_EXPIRED(40905, "Token 已过期"),
    REFRESH_SESSION_INVALID(40906, "刷新会话无效或已失效"),
    EMAIL_EXISTS(40907, "邮箱已存在"),
    INTERNAL_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;
}
