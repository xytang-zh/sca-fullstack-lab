package com.xytang.common.web.filter;

import com.xytang.common.core.constant.HeaderConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器（对齐 spring-cloud-gateway/CLAUDE.md §9.2 + spring-cloud-alibaba/CLAUDE.md §11.1）。
 *
 * <p>从网关注入的 {@code X-Trace-Id} Header 读取 traceId，写入 MDC 供 SLF4J 使用；
 * 若缺失则生成新 UUID（兼容直连服务场景），并写入响应头 {@code X-Trace-Id}。
 *
 * <p>请求结束在 finally 块清理 MDC，防止线程池复用串号。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
        throws ServletException, IOException {
        String traceId = req.getHeader(HeaderConstants.X_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_ID, traceId);
        resp.setHeader(HeaderConstants.X_TRACE_ID, traceId);
        try {
            chain.doFilter(req, resp);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }
}