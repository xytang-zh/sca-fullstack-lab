package com.xytang.auth.constant;

/**
 * 认证模块 Redis Key 前缀与 MQ Exchange 常量
 *
 * <p>对齐 spring-cloud-auth/CLAUDE.md §8.2 与 specs/001-platform-mvp/contracts/auth-api.md。
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** 登录失败计数 Key 前缀：auth:login:fail:{username} */
    public static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";
    /** 登录锁定标记 Key 前缀：auth:login:lock:{username} */
    public static final String LOGIN_LOCK_PREFIX = "auth:login:lock:";
    /** 图形验证码 Key 前缀：auth:captcha:{key} */
    public static final String CAPTCHA_PREFIX = "auth:captcha:";
    /** 验证码图片 Key 前缀：auth:captcha:image:{key} */
    public static final String CAPTCHA_IMAGE_PREFIX = "auth:captcha:image:";
    /** IP 黑名单 Key 前缀：auth:ip:blacklist:{ip} */
    public static final String IP_BLACKLIST_PREFIX = "auth:ip:blacklist:";
    /** 短信验证码 Key 前缀：auth:sms:code:{phone} */
    public static final String SMS_CODE_PREFIX = "auth:sms:code:";
    /** 短信发送限流 Key 前缀：auth:sms:limit:{phone} */
    public static final String SMS_LIMIT_PREFIX = "auth:sms:limit:";
    /** SSO 授权码 Key 前缀：auth:sso:code:{code} */
    public static final String SSO_CODE_PREFIX = "auth:sso:code:";
    /** SSO Client 凭证 Key 前缀：auth:sso:client:{clientId} */
    public static final String SSO_CLIENT_PREFIX = "auth:sso:client:";
    /** Refresh Token Key 前缀：auth:oauth2:refresh:{token} */
    public static final String REFRESH_TOKEN_PREFIX = "auth:oauth2:refresh:";

    /** 超级管理员全权限通配符（Sa-Token 权限码） */
    public static final String SUPER_ADMIN_PERMS = "*:*:*";

    /** 登录失败最大次数（超过则锁定账号） */
    public static final int LOGIN_MAX_FAIL_COUNT = 5;
    /** 账号锁定时长（分钟） */
    public static final long LOGIN_LOCK_MINUTES = 15L;
    /** 验证码有效期（分钟） */
    public static final long CAPTCHA_TTL_MINUTES = 5L;
    /** 登录态默认有效期（秒）：30 分钟 */
    public static final long LOGIN_TOKEN_TIMEOUT_SECONDS = 1800L;
    /** 记住我模式有效期（秒）：7 天 */
    public static final long REMEMBER_ME_TIMEOUT_SECONDS = 7 * 24 * 3600L;
}
