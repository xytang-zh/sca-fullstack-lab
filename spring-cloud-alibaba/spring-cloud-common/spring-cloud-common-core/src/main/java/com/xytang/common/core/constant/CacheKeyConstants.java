package com.xytang.common.core.constant;

/**
 * 缓存 Key 规范（对齐 data-model.md §6）
 *
 * <p>所有 Redis Key 必须以 {@code spring-cloud:{service}:{biz}:{id}} 格式，避免冲突。
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {
    }

    public static final String PREFIX = "spring-cloud";

    public static final String USER_PERMS = PREFIX + ":auth:user:perms:";
    public static final String USER_ROLES = PREFIX + ":auth:user:roles:";
    public static final String DICT = PREFIX + ":system:dict:";
    public static final String PARAM = PREFIX + ":system:param:";
    public static final String LOCK = "lock:";

    public static final String SA_TOKEN_LOGIN = "satoken:login:token:";
    public static final String SA_SSO_TICKET = "sa:sso:ticket:";
}
