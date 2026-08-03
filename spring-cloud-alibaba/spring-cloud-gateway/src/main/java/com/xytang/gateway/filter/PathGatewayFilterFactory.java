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
 * {@code X-Gateway-Path} 请求头透传到下游，下游 {@code TraceIdFilter}
 * 读取后填到 {@code PathHolder}，{@code RResponseAdvice} 取出填到
 * R&lt;T&gt;.path 字段。
 *
 * <p>方案 D（spec.md §path 字段策略）：
 * <ul>
 *   <li>网关层：填实际 URI（如 {@code /api/auth/login}），运维查路由问题</li>
 *   <li>下游层：开发期先用实际 URI 占位（即网关传过来的值），
 *       等监控上线再补 pattern 提取（如 {@code /api/users/{id}}）</li>
 *   <li>prod 输出 pattern，prod 屏蔽实际 URI</li>
 * </ul>
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
