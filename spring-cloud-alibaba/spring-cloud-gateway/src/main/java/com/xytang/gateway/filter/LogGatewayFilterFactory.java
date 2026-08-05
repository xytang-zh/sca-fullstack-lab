package com.xytang.gateway.filter;

import com.xytang.common.core.constant.HeaderConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 网关日志过滤器（对齐 spring-cloud-gateway/CLAUDE.md §9.1 + §12.5，T037 重构）
 *
 * <p>职责（重构后）：
 * <ol>
 *   <li>从 exchange attribute 或 {@code X-Trace-Id} 头复用 traceId（由
 *       {@link TraceIdGatewayFilterFactory} 已生成，避免重复生成）</li>
 *   <li>记录请求方法 / URI / 耗时 / 状态码</li>
 *   <li>慢请求（&gt;1s）WARN 日志</li>
 *   <li>禁止打印请求 body（可能含敏感数据）</li>
 * </ol>
 *
 * <p>顺序：必须在 {@link TraceIdGatewayFilterFactory} 之后执行，
 * 否则拿不到 traceId（路由配置中 TraceId 必须在 Log 之前声明）。
 */
@Component
@Slf4j
public class LogGatewayFilterFactory extends AbstractGatewayFilterFactory<LogGatewayFilterFactory.Config> {

    private static final long SLOW_THRESHOLD_MS = 1000L;
    private static final String UNKNOWN_TRACE = "unknown";

    public LogGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long start = System.currentTimeMillis();
            String tid = resolveTraceId(exchange);
            return chain.filter(exchange)
                .doFinally(signal -> {
                    long cost = System.currentTimeMillis() - start;
                    var resp = exchange.getResponse();
                    var req = exchange.getRequest();
                    String uri = req.getURI().getPath();
                    String method = req.getMethod().name();
                    var status = resp.getStatusCode();
                    String logMsg = String.format("[REQ] traceId=%s %s %s %dms status=%s",
                            tid, method, uri, cost, status);
                    if (cost > SLOW_THRESHOLD_MS) {
                        log.warn("[SLOW] {}", logMsg);
                    } else {
                        log.info(logMsg);
                    }
                });
        };
    }

    // 解析 traceId：优先 exchange attribute（TraceId 过滤器已写入），其次请求头 X-Trace-Id，兜底 unknown
    private String resolveTraceId(org.springframework.web.server.ServerWebExchange exchange) {
        Object attr = exchange.getAttribute(TraceIdGatewayFilterFactory.TRACE_ID_ATTR);
        if (attr instanceof String s && StringUtils.hasText(s)) {
            return s;
        }
        String header = exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_TRACE_ID);
        return StringUtils.hasText(header) ? header : UNKNOWN_TRACE;
    }

    /**
     * 过滤器配置（无参数）。
     */
    @Data
    public static class Config {
    }
}
