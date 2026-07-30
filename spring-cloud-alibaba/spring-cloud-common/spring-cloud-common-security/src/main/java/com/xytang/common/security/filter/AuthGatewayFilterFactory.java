package com.xytang.common.security.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.xytang.common.core.constant.HeaderConstants;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 鉴权过滤器工厂（对齐 spring-cloud-gateway/CLAUDE.md §6 鉴权规范）。
 *
 * <p>职责：白名单放行 / Token 提取与校验 / 透传 X-Login-Id 与 X-Token；
 * 失败返回 HTTP 401 + 双层响应码（bizCode "99301" Token 缺失 / "99302" Token 无效）。
 */
@Component
@Slf4j
public class AuthGatewayFilterFactory
    extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.Config> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    public AuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("excludePaths");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest req = exchange.getRequest();
            String path = req.getPath().value();

            if (isExcluded(path, config.getExcludePaths())) {
                return chain.filter(exchange);
            }

            String auth = req.getHeaders().getFirst(HeaderConstants.AUTHORIZATION);
            if (auth == null || auth.isBlank()) {
                return unauthorized(exchange, BizCode.GW_TOKEN_MISSING);
            }
            String token = auth.startsWith(HeaderConstants.BEARER_PREFIX)
                ? auth.substring(HeaderConstants.BEARER_PREFIX.length())
                : auth;

            Object loginId;
            try {
                loginId = StpUtil.getLoginIdByToken(token);
            } catch (Exception e) {
                log.warn("[Auth] token invalid, path={} err={}", path, e.getMessage());
                return unauthorized(exchange, BizCode.GW_TOKEN_INVALID);
            }
            if (loginId == null) {
                return unauthorized(exchange, BizCode.GW_TOKEN_INVALID);
            }

            ServerHttpRequest mutated = req.mutate()
                .header(HeaderConstants.X_LOGIN_ID, String.valueOf(loginId))
                .header(HeaderConstants.X_TOKEN, token)
                .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    private boolean isExcluded(String path, List<String> excludePaths) {
        if (CollectionUtils.isEmpty(excludePaths)) {
            return false;
        }
        return excludePaths.stream().anyMatch(p -> MATCHER.match(p, path));
    }

    private Mono<Void> unauthorized(org.springframework.web.server.ServerWebExchange exchange,
                                    BizCode bizCode) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body;
        try {
            body = MAPPER.writeValueAsString(R.fail(bizCode));
        } catch (Exception e) {
            body = "{\"code\":401,\"bizCode\":\"" + bizCode.code()
                + "\",\"message\":\"" + bizCode.message() + "\",\"data\":null}";
        }
        DataBuffer buf = resp.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Mono.just(buf));
    }

    @Data
    public static class Config {
        private List<String> excludePaths = Collections.emptyList();
    }
}
