package com.xytang.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xytang.common.core.constant.HeaderConstants;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.R;
import com.xytang.gateway.filter.TraceIdGatewayFilterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 网关全局异常处理（响应式 WebFlux）。
 *
 * <p>统一返回单一业务码 R&lt;T&gt; JSON 结构，按 HTTP 状态码映射 BizCode：
 * 429→RATE_LIMIT / 503→GW_DOWNSTREAM_UNAVAILABLE / 504→GW_DOWNSTREAM_TIMEOUT
 * / 404→GW_ROUTE_NOT_FOUND / 401→GW_TOKEN_INVALID / 其他→SYS_ERROR。
 *
 * <p>网关响应式层没有 RResponseAdvice（RestControllerAdvice 仅 Servlet MVC），
 * 故手动填 traceId + timestamp 到 R&lt;T&gt;。
 */
@Configuration
@Order(-1)
@RequiredArgsConstructor
@Slf4j
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse resp = exchange.getResponse();
        if (resp.isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatus status = resolveStatus(ex);
        resp.setStatusCode(status);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        BizCode bizCode = resolveBizCode(status, ex);
        String path = exchange.getRequest().getPath().value();
        String traceId = resolveTraceId(exchange);
        resp.getHeaders().add(HeaderConstants.X_TRACE_ID, traceId);
        R<Void> r = R.<Void>fail(bizCode)
                .traceId(traceId);
        String body;
        try {
            body = MAPPER.writeValueAsString(r);
        } catch (JsonProcessingException e) {
            body = "{\"code\":" + bizCode.getCode()
                    + ",\"message\":\"" + bizCode.getUserMessage() + "\""
                    + ",\"data\":null"
                    + ",\"traceId\":\"" + traceId + "\"}";
        }
        DataBuffer buf = resp.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        log.warn("[GatewayException] path={} traceId={} status={} code={} err={}",
                path, traceId, status, bizCode.getCode(), ex.getMessage());
        return resp.writeWith(Mono.just(buf));
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        Object attr = exchange.getAttribute(TraceIdGatewayFilterFactory.TRACE_ID_ATTR);
        if (attr instanceof String s && StringUtils.hasText(s)) {
            return s;
        }
        String header = exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_TRACE_ID);
        return StringUtils.hasText(header) ? header
            : UUID.randomUUID().toString().replace("-", "");
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
            return HttpStatus.resolve(rse.getStatusCode().value()) != null
                ? HttpStatus.valueOf(rse.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private BizCode resolveBizCode(HttpStatus status, Throwable ex) {
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return BizCode.RATE_LIMIT;
        }
        if (status == HttpStatus.NOT_FOUND) {
            return BizCode.GW_ROUTE_NOT_FOUND;
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            return BizCode.GW_TOKEN_INVALID;
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return BizCode.GW_DOWNSTREAM_UNAVAILABLE;
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return BizCode.GW_DOWNSTREAM_TIMEOUT;
        }
        return BizCode.SYS_ERROR;
    }
}
