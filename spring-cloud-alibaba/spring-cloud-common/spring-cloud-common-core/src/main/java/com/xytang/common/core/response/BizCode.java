package com.xytang.common.core.response;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 业务错误码枚举（单一业务码体系，实现 {@link ErrorCode}）。
 *
 * <p>编码规则：5 位数字 = 区段(1) + 模块(1) + 序号(3)
 * <ul>
 *   <li>区段：1 参数校验 / 2 用户权限 / 3 业务规则 / 4 第三方服务 / 5 系统内部</li>
 *   <li>模块：0 通用 / 1 auth / 2 system / 3 article / 4 comment / 5 portal
 *       / 6 message / 7 search / 8 file / 99 gateway</li>
 * </ul>
 *
 * <p>成功码固定为 {@code 200}（<code>SUCCESS</code>），与 HTTP 语义对齐。
 * 通用错误模块号取 0，使区段可识别；具体服务错误通过模块号区分。
 *
 * <p>HTTP 状态码遵循 RESTful 严格模式：成功 200 / 参数错误 400 / 未登录 401 / 无权限 403
 * / 资源不存在 404 / 资源冲突 409 / 锁定 422/423 / 限流 429 / 系统异常 500
 * / 下游不可用 503 / 下游超时 504。
 */
public enum BizCode implements ErrorCode {

    SUCCESS(200, 200, "操作成功", "操作成功"),

    // ===== 参数校验 1xxxx（模块 0 通用 / 1 auth） =====
    PARAM_ERROR(10001, 400, "参数校验失败", "参数校验失败: {}"),
    PARAM_MISSING(10002, 400, "缺少必要参数", "缺少必要参数: {}"),
    PARAM_TYPE_ERROR(10003, 400, "参数类型错误", "参数类型错误: {}"),
    PASSWORD_WEAK(10004, 400, "密码强度不够", "密码强度不够: {}"),
    METHOD_NOT_ALLOWED(10005, 405, "请求方法不支持", "请求方法不支持: {}"),
    AUTH_CAPTCHA_ERROR(11001, 400, "验证码错误或已过期", "验证码错误或已过期: {}"),
    AUTH_USERNAME_INVALID(11002, 400, "用户名格式不正确", "用户名格式不正确: {}"),

    // ===== 用户/权限 2xxxx（模块 0 通用 / 1 auth / 2 system / 99 gateway） =====
    OPTIMISTIC_LOCK(20001, 409, "资源已被他人修改，请刷新后重试", "乐观锁冲突: {}"),
    DATA_SCOPE_DENIED(20002, 403, "无权访问该数据范围", "数据权限拒绝: userId={}, scope={}"),
    AUTH_USER_NOT_FOUND(21001, 404, "用户名或密码错误", "用户不存在: userId={}"),
    AUTH_USER_EXISTED(21002, 409, "用户已存在", "用户已存在: username={}"),
    AUTH_PASSWORD_ERROR(21003, 400, "用户名或密码错误", "密码错误: userId={}"),
    AUTH_SSO_TICKET_INVALID(21004, 401, "SSO Ticket 无效或已过期", "SSO Ticket 无效: {}"),
    AUTH_USER_DISABLED(21005, 403, "用户已被禁用", "用户已被禁用: userId={}"),
    AUTH_USER_LOCKED(21006, 423, "用户已被锁定，请稍后再试", "用户已被锁定: userId={}, remain={}"),
    AUTH_TOKEN_EXPIRED(21007, 401, "登录已过期，请重新登录", "Token 已过期: token={}"),
    AUTH_TOKEN_KICKED_OUT(21008, 401, "您的登录已失效，请重新登录", "登录被踢下线: {}"),
    AUTH_TOKEN_MISSING(21009, 401, "未登录，请先登录", "未登录: path={}"),
    AUTH_REFRESH_TOKEN_INVALID(21010, 401, "Refresh Token 无效或已过期", "Refresh Token 无效: {}"),
    AUTH_SSO_CLIENT_INVALID(21011, 401, "SSO Client 凭证校验失败", "SSO Client 凭证校验失败: {}"),
    SYS_USER_NOT_FOUND(22001, 404, "用户不存在", "用户不存在: userId={}"),
    SYS_USER_EXISTED(22002, 409, "用户已存在", "用户已存在: username={}"),
    SYS_LAST_ADMIN(22003, 409, "禁止删除最后一个超级管理员", "禁止删除最后一个超管: {}"),
    SYS_ROLE_NOT_FOUND(22004, 404, "角色不存在", "角色不存在: id={}"),
    SYS_MENU_NOT_FOUND(22005, 404, "菜单不存在", "菜单不存在: id={}"),
    SYS_DEPT_NOT_FOUND(22006, 404, "部门不存在", "部门不存在: id={}"),
    GW_TOKEN_MISSING(29001, 401, "未登录，请先登录", "Token 缺失: path={}"),
    GW_TOKEN_INVALID(29002, 401, "登录已失效，请重新登录", "Token 无效: path={}"),

    // ===== 业务规则 3xxxx（模块 0 通用） =====
    RATE_LIMIT(30001, 429, "请求过于频繁，请稍后重试", "请求过于频繁: path={}"),
    CONTENT_STATUS_INVALID(30002, 400, "内容状态变更非法", "内容状态变更非法: {}"),
    CONTENT_NOT_FOUND(30003, 404, "内容不存在", "内容不存在: id={}"),

    // ===== 系统错误 5xxxx（模块 0 通用 / 99 gateway） =====
    SYS_ERROR(50000, 500, "系统繁忙，请稍后再试", "系统异常: {}"),
    RPC_ERROR(50001, 500, "系统繁忙，请稍后再试", "RPC 调用失败: {}"),
    DB_ERROR(50002, 500, "系统繁忙，请稍后再试", "数据库异常: {}"),
    CONFIG_ERROR(50003, 500, "系统繁忙，请稍后再试", "系统配置异常: {}"),
    GW_ROUTE_NOT_FOUND(50004, 404, "请求路径不存在", "路由不存在: {}"),
    GW_DOWNSTREAM_TIMEOUT(50005, 504, "下游服务响应超时", "下游服务响应超时: {}"),
    GW_DOWNSTREAM_UNAVAILABLE(50006, 503, "下游服务不可用", "下游服务不可用: {}");

    private final int code;
    private final int httpStatus;
    private final String userMessage;
    private final String devMessage;

    BizCode(int code, int httpStatus, String userMessage, String devMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.userMessage = userMessage;
        this.devMessage = devMessage;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getUserMessage() {
        return userMessage;
    }

    @Override
    public String getDevMessage() {
        return devMessage;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 构建带参数的开发者消息（{@code {}} 占位符）。
     *
     * @param args 占位符填充参数
     * @return 填充后的开发者消息
     */
    public String formatDevMessage(Object... args) {
        if (args == null || args.length == 0) {
            return devMessage;
        }
        return MessageFormat.format(devMessage.replace("{}", "{0}"), args);
    }

    public static Optional<BizCode> fromCode(int code) {
        return Arrays.stream(values())
            .filter(b -> Objects.equals(b.code, code))
            .findFirst();
    }
}