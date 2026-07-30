package com.xytang.common.core.constant;

/**
 * HTTP Header 常量（对齐 common-patterns.md §6 + gateway CLAUDE.md §6）
 */
public final class HeaderConstants {

    private HeaderConstants() {
    }

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String X_LOGIN_ID = "X-Login-Id";
    public static final String X_TOKEN = "X-Token";
    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String X_GATEWAY_PATH = "X-Gateway-Path";
    public static final String X_API_VERSION = "X-API-Version";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_IDEMPOTENCY_KEY = "X-Idempotency-Key";

    public static final String RETRY_AFTER = "Retry-After";
}
