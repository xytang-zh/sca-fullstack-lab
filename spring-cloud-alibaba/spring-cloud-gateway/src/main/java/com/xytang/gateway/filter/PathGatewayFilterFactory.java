package com.xytang.gateway.filter;

import com.xytang.common.core.constant.HeaderConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 网关层路径过滤器（T032）。
 *
 * <p>职责：把网关层实际 URI（如 {@code /api/auth/login}）通过
 * {@code X-Gateway-Path} 请求头透传到下游，供下游日志与排查定位使用。
 *
 * <p>注意：WebFlux 响应式模型禁止 ThreadLocal，path 跨服务传递必须用请求头。
 */
@Component
@Slf4j
public class PathGatewayFilterFactory extends AbstractGatewayFilterFactory<PathGatewayFilterFactory.Config> {

    public PathGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String gatewayPath = exchange.getRequest().getPath().value();
            if (StringUtils.hasText(gatewayPath)) {
                var mutated = exchange.getRequest().mutate()
                        .header(HeaderConstants.X_GATEWAY_PATH, gatewayPath)
                        .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            }
            return chain.filter(exchange);
        };
    }

    /**
     * 过滤器配置（无参数）。
     */
    public static class Config {
    }
}
