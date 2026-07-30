package com.xytang.common.core.response;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 业务错误码枚举（双层响应码体系：HTTP 状态码 + 5 位字符串 bizCode）。
 *
 * <p>编码规则：5 位字符串 = 模块号(2) + 错误类别(1) + 具体错误(2)
 * <ul>
 *   <li>模块号：00 通用 / 01 auth / 02 system / 03 workflow / 04 ai / 05 monitor
 *       / 06 message / 07 search / 08 file / 09 log / 10 portal / 11 job / 12 report / 99 gateway</li>
 *   <li>错误类别：0 成功 / 1 参数 / 2 业务 / 3 权限 / 4 系统 / 5 第三方</li>
 * </ul>
 *
 * <p>HTTP 状态码遵循 RESTful 严格模式：成功 200 / 参数错误 400 / 未登录 401 / 无权限 403
 * / 资源不存在 404 / 资源冲突 409 / 锁定 422/423 / 限流 429 / 系统异常 500
 * / 下游不可用 503 / 下游超时 504。
 */
public enum BizCode {

    SUCCESS(200, "00000", "操作成功"),

    PARAM_ERROR(400, "00101", "参数校验失败"),
    PARAM_MISSING(400, "00102", "缺少必要参数"),
    PARAM_TYPE_ERROR(400, "00103", "参数类型错误"),
    PASSWORD_WEAK(400, "00104", "密码强度不够"),
    METHOD_NOT_ALLOWED(405, "00105", "请求方法不支持"),

    RATE_LIMIT(429, "00201", "请求过于频繁，请稍后重试"),
    OPTIMISTIC_LOCK(409, "00202", "资源已被他人修改，请刷新后重试"),
    CONTENT_STATUS_INVALID(400, "00203", "内容状态变更非法"),
    DATA_SCOPE_DENIED(403, "00302", "无权访问该数据范围"),

    SYS_ERROR(500, "00401", "系统繁忙，请稍后重试"),
    RPC_ERROR(500, "00402", "远程服务调用失败"),
    DB_ERROR(500, "00403", "数据库操作异常"),
    CONFIG_ERROR(500, "00404", "系统配置异常"),

    AUTH_USER_NOT_FOUND(404, "01101", "用户名或密码错误"),
    AUTH_USER_EXISTED(409, "01102", "用户已存在"),
    AUTH_USERNAME_INVALID(400, "01103", "用户名格式不正确"),
    AUTH_PASSWORD_ERROR(400, "01105", "用户名或密码错误"),
    AUTH_SSO_TICKET_INVALID(401, "01303", "SSO Ticket 无效或已过期"),
    AUTH_USER_DISABLED(403, "01301", "用户已被禁用"),
    AUTH_USER_LOCKED(423, "01302", "用户已被锁定，请{0}分钟后重试"),
    AUTH_CAPTCHA_ERROR(400, "01201", "验证码错误或已过期"),

    SYS_USER_NOT_FOUND(404, "02101", "用户不存在"),
    SYS_USER_EXISTED(409, "02102", "用户已存在"),
    SYS_LAST_ADMIN(409, "02103", "禁止删除最后一个超级管理员"),
    SYS_ROLE_NOT_FOUND(404, "02201", "角色不存在"),
    SYS_MENU_NOT_FOUND(404, "02301", "菜单不存在"),
    SYS_DEPT_NOT_FOUND(404, "02401", "部门不存在"),

    GW_ROUTE_NOT_FOUND(404, "99104", "路由不存在"),
    GW_DOWNSTREAM_TIMEOUT(504, "99404", "下游服务响应超时"),
    GW_DOWNSTREAM_UNAVAILABLE(503, "99304", "下游服务不可用"),
    GW_TOKEN_MISSING(401, "99301", "Token 缺失"),
    GW_TOKEN_INVALID(401, "99302", "Token 无效或已过期");

    private final int httpCode;
    private final String code;
    private final String message;

    BizCode(int httpCode, String code, String message) {
        this.httpCode = httpCode;
        this.code = code;
        this.message = message;
    }

    public int httpCode() {
        return httpCode;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return MessageFormat.format(message, args);
    }

    public static Optional<BizCode> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(b -> Objects.equals(b.code, code))
            .findFirst();
    }
}
