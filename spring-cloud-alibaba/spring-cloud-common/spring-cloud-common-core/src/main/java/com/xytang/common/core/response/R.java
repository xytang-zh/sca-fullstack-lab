package com.xytang.common.core.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 统一响应包装类（双层响应码体系：HTTP 状态码 + 5 位字符串 bizCode）。
 *
 * <p>字段：{@code code}(HTTP) / {@code bizCode}(业务) / {@code message} /
 * {@code data} / {@code timestamp}(Instant) / {@code traceId} / {@code path} /
 * {@code devMessage}(dev profile 诊断)。
 *
 * <p>失败响应：调用 {@link #fail(BizCode)} 或 {@link #fail(BizCode, String)}，
 * HTTP 码由 BizCode.httpCode() 决定，bizCode 由 BizCode.code() 决定。
 *
 * <p>响应阶段由 {@code RResponseAdvice}（common-web）从 MDC 与 ThreadLocal
 * 自动填充 traceId/path/devMessage。
 *
 * @param <T> 业务数据类型
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String bizCode;
    private String message;
    private T data;
    private Instant timestamp;
    private String traceId;
    private String path;
    private String devMessage;

    public static <T> R<T> ok() {
        return ok(BizCode.SUCCESS.message(), null);
    }

    public static <T> R<T> ok(T data) {
        return ok(BizCode.SUCCESS.message(), data);
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>();
        r.code = BizCode.SUCCESS.httpCode();
        r.bizCode = BizCode.SUCCESS.code();
        r.message = message;
        r.data = data;
        r.timestamp = Instant.now();
        return r;
    }

    public static <T> R<T> fail(BizCode bizCode) {
        return fail(bizCode, bizCode.message());
    }

    public static <T> R<T> fail(BizCode bizCode, String message) {
        R<T> r = new R<>();
        r.code = bizCode.httpCode();
        r.bizCode = bizCode.code();
        r.message = message;
        r.timestamp = Instant.now();
        return r;
    }

    public static <T> R<T> fail(int httpCode, String bizCode, String message) {
        R<T> r = new R<>();
        r.code = httpCode;
        r.bizCode = bizCode;
        r.message = message;
        r.timestamp = Instant.now();
        return r;
    }

    public R<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public R<T> path(String path) {
        this.path = path;
        return this;
    }

    public R<T> devMessage(String devMessage) {
        this.devMessage = devMessage;
        return this;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code == BizCode.SUCCESS.httpCode()
            && BizCode.SUCCESS.code().equals(this.bizCode);
    }
}
