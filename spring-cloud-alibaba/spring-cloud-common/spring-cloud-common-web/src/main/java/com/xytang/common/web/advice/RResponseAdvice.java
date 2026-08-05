package com.xytang.common.web.advice;

import com.xytang.common.core.constant.HeaderConstants;
import com.xytang.common.core.response.R;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * R&lt;T&gt; 响应体增强：自动填充 traceId。
 *
 * <p>从 MDC 取 traceId（由 TraceIdFilter 从 X-Trace-Id 头读取或生成），
 * MDC 缺失则回退到 X-Trace-Id 请求头，保证响应体 traceId 与全链路一致。
 */
@Order
@RestControllerAdvice
public class RResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String MDC_TRACE_ID = "traceId";

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    /**
     * 写入响应体前回调：仅对 R&lt;T&gt; 类型补 traceId，非 R 类型原样放行。
     *
     * <p>traceId 优先级：响应体已带 &gt; MDC（TraceIdFilter 写入）&gt;
     * 请求头 X-Trace-Id（直连场景兜底）。
     *
     * @param body 原始响应体
     * @return 补全 traceId 后的响应体
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class converterType, ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof R<?> r)) {
            return body;
        }
        if (r.getTraceId() == null) {
            String traceId = MDC.get(MDC_TRACE_ID);
            if (traceId == null || traceId.isBlank()) {
                traceId = extractHeader(request, HeaderConstants.X_TRACE_ID);
            }
            if (traceId != null && !traceId.isBlank()) {
                r.traceId(traceId);
            }
        }
        return r;
    }

    // 从请求头提取 traceId（仅 Servlet 环境可用，WebFlux 场景返回 null）
    private String extractHeader(ServerHttpRequest request, String headerName) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            String value = req.getHeader(headerName);
            return (value == null || value.isBlank()) ? null : value;
        }
        return null;
    }
}