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

/**
 * 统一响应包装类（单一业务码体系）。
 *
 * <p>字段：{@code code}(业务状态码) / {@code message}(友好文案) / {@code data}(业务数据) /
 * {@code timestamp}(毫秒时间戳) / {@code traceId}(链路追踪 ID)。
 *
 * <p>失败响应：调用 {@link #fail(ErrorCode)} 或 {@link #fail(ErrorCode, String)}，
 * HTTP 状态码由调用方依据 {@link ErrorCode#getHttpStatus()} 设置。
 *
 * <p>响应阶段由 {@code RResponseAdvice}（common-web）从 MDC 自动填充 traceId。
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

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private String traceId;

    public static <T> R<T> ok() {
        return ok(BizCode.SUCCESS.getUserMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return ok(BizCode.SUCCESS.getUserMessage(), data);
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>();
        r.code = BizCode.SUCCESS.getCode();
        r.message = message;
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getUserMessage());
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public R<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code != null && this.code.equals(BizCode.SUCCESS.getCode());
    }
}