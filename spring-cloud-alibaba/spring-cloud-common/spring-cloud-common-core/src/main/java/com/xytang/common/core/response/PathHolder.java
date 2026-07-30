package com.xytang.common.core.response;

/**
 * 请求路径 ThreadLocal 持有者。
 *
 * <p>网关层由 PathGatewayFilterFactory 填实际 URI（如 /api/auth/login）；
 * 下游层开发期由 RResponseAdvice 从 HttpServletRequest.getRequestURI() 取实际 URI 占位，
 * 后续监控上线后再补 pattern 提取（如 /api/users/{id}）。
 *
 * <p>请求结束由 TraceIdFilter 在 finally 块清理，防止线程池复用串号。
 */
public final class PathHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private PathHolder() {
    }

    public static void set(String path) {
        HOLDER.set(path);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
