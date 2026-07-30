package com.xytang.common.core.response;

/**
 * 开发者诊断信息 ThreadLocal 持有者。
 *
 * <p>仅 dev profile 下由 GlobalExceptionHandler 填充异常堆栈/SQL/具体原因，
 * RResponseAdvice 在响应阶段读取并填到 R&lt;T&gt;.devMessage 字段；
 * prod profile 不填充，响应体通过 @JsonInclude(NON_NULL) 自动屏蔽。
 *
 * <p>请求结束由 TraceIdFilter 在 finally 块清理，防止线程池复用串号。
 */
public final class DevMessageHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private DevMessageHolder() {
    }

    public static void set(String devMessage) {
        HOLDER.set(devMessage);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
