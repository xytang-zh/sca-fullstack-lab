package com.xytang.common.web.advice;

import com.xytang.common.core.constant.HeaderConstants;
import com.xytang.common.core.response.DevMessageHolder;
import com.xytang.common.core.response.PathHolder;
import com.xytang.common.core.response.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * R&lt;T&gt; 响应体增强：自动填充 traceId/path/devMessage。
 *
 * <p>从 MDC 取 traceId（由 TraceIdFilter 从 X-Trace-Id 头读取或生成），
 * 从 PathHolder ThreadLocal 取 path（由网关 PathGatewayFilterFactory 透传
 * X-Gateway-Id 头、下游 TraceIdFilter 读取后填充，或回退到 HttpServletRequest.getRequestURI()），
 * 从 DevMessageHolder ThreadLocal 取 devMessage（仅 dev profile 由
 * GlobalExceptionHandler 填充异常堆栈，prod 为 null 不序列化）。
 *
 * <p>dev/prod 切换：devMessage 字段只在 dev profile 下读取 DevMessageHolder；
 * 即使 GlobalExceptionHandler 误填充，prod profile 也会屏蔽，避免泄露敏感诊断信息。
 */
@Order
@RestControllerAdvice
@RequiredArgsConstructor
public class RResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String MDC_TRACE_ID = "traceId";

    private final Environment environment;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

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
        if (r.getPath() == null) {
            String path = PathHolder.get();
            if (path == null || path.isBlank()) {
                path = extractHeader(request, HeaderConstants.X_GATEWAY_PATH);
            }
            if (path == null || path.isBlank()) {
                path = extractUri(request);
            }
            if (path != null && !path.isBlank()) {
                r.path(path);
            }
        }
        if (r.getDevMessage() == null && environment.acceptsProfiles(Profiles.of("dev"))) {
            String devMessage = DevMessageHolder.get();
            if (devMessage != null && !devMessage.isBlank()) {
                r.devMessage(devMessage);
            }
        }
        return r;
    }

    private String extractUri(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            return req.getRequestURI();
        }
        return null;
    }

    private String extractHeader(ServerHttpRequest request, String headerName) {
        String value = request.getHeaders().getFirst(headerName);
        return (value == null || value.isBlank()) ? null : value;
    }
}
