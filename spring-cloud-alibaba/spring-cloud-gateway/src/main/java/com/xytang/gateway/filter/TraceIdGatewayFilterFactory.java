package com.xytang.gateway.filter;

import com.xytang.common.core.constant.HeaderConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关层链路追踪过滤器（T031）。
 *
 * <p>职责：
 * <ol>
 *   <li>从 {@code X-Trace-Id} 请求头读取 traceId，缺失则生成 UUID（无横线）</li>
 *   <li>透传 traceId 到下游服务请求头</li>
 *   <li>把 traceId 写到响应头 {@code X-Trace-Id}，便于前端/运维关联</li>
 *   <li>把 traceId 写到 exchange attribute（key 见 {@link #TRACE_ID_ATTR}），
 *       供后续过滤器（如 {@link LogGatewayFilterFactory}）复用，避免重复生成</li>
 * </ol>
 *
 * <p>顺序：必须在 {@link LogGatewayFilterFactory} 之前执行，否则 Log 拿不到 traceId。
 * 通过 Spring Cloud Gateway 的过滤器链顺序保证（默认按声明顺序，可用 Order 注解调整）。
 *
 * <p>注意：WebFlux 响应式模型禁止使用 {@link ThreadLocal}，traceId 跨 reactor
 * 阶段传递必须用 {@code exchange.getAttributes()} 或响应头。
 */
@Component
@Slf4j
public class TraceIdGatewayFilterFactory extends AbstractGatewayFilterFactory<TraceIdGatewayFilterFactory.Config> {

    public static final String TRACE_ID_ATTR = "gateway.traceId";

    public TraceIdGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String traceId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_TRACE_ID);
            if (!StringUtils.hasText(traceId)) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            final String tid = traceId;
            exchange.getAttributes().put(TRACE_ID_ATTR, tid);
            var mutated = exchange.getRequest().mutate()
                .header(HeaderConstants.X_TRACE_ID, tid)
                .build();
            var mutatedExchange = exchange.mutate().request(mutated).build();
            mutatedExchange.getResponse().getHeaders().add(HeaderConstants.X_TRACE_ID, tid);
            return chain.filter(mutatedExchange);
        };
    }

    public static class Config {
    }
}
