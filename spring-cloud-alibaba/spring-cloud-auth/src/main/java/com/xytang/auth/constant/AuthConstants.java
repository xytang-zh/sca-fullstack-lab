package com.xytang.auth.constant;

/**
 * 认证模块 Redis Key 前缀与 MQ Exchange 常量
 *
 * <p>对齐 spring-cloud-auth/CLAUDE.md §8.2 与 specs/001-platform-mvp/contracts/auth-api.md。
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    public static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";
    public static final String LOGIN_LOCK_PREFIX = "auth:login:lock:";
    public static final String CAPTCHA_PREFIX = "auth:captcha:";
    public static final String IP_BLACKLIST_PREFIX = "auth:ip:blacklist:";

    public static final String SUPER_ADMIN_PERMS = "*:*:*";

    public static final int LOGIN_MAX_FAIL_COUNT = 5;
    public static final long LOGIN_LOCK_MINUTES = 15L;
    public static final long CAPTCHA_TTL_MINUTES = 5L;
    public static final long LOGIN_TOKEN_TIMEOUT_SECONDS = 1800L;
    public static final long REMEMBER_ME_TIMEOUT_SECONDS = 7 * 24 * 3600L;

    public static final String EXCHANGE_USER_LOGIN = "user.login";
    public static final String EXCHANGE_USER_KICKOUT = "user.kickout";
    public static final String EXCHANGE_USER_LOGOUT = "user.logout";
    public static final String EXCHANGE_LOG_LOGIN = "log.login.create";
}
