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

    /** 业务状态码：200 成功，1xxxx 参数 / 2xxxx 用户权限 / 3xxxx 业务 / 4xxxx 第三方 / 5xxxx 系统 */
    private Integer code;

    /** 用户友好文案（失败时展示给前端，成功默认 "操作成功"） */
    private String message;

    /** 业务数据（成功时携带，失败时为 null） */
    private T data;

    /** 响应生成时间戳（毫秒），前端可用于计算耗时 */
    private Long timestamp;

    /** 链路追踪 ID：由网关生成并经 MDC 透传，响应阶段由 RResponseAdvice 自动填充 */
    private String traceId;

    /**
     * 成功响应（无数据）。
     *
     * @param <T> 业务数据类型
     * @return 成功包装对象
     */
    public static <T> R<T> ok() {
        return ok(BizCode.SUCCESS.getUserMessage(), null);
    }

    /**
     * 成功响应（携带数据）。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功包装对象
     */
    public static <T> R<T> ok(T data) {
        return ok(BizCode.SUCCESS.getUserMessage(), data);
    }

    /**
     * 成功响应（自定义文案 + 数据）。
     *
     * @param message 自定义成功文案
     * @param data    业务数据
     * @param <T>     业务数据类型
     * @return 成功包装对象
     */
    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>();
        r.code = BizCode.SUCCESS.getCode();
        r.message = message;
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    /**
     * 失败响应，使用错误码默认的友好文案。
     *
     * @param errorCode 业务错误码（定义 code/message/httpStatus）
     * @param <T>       业务数据类型
     * @return 失败包装对象
     */
    public static <T> R<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getUserMessage());
    }

    /**
     * 失败响应，覆盖自定义文案。
     *
     * @param errorCode 业务错误码
     * @param message   覆盖后的友好文案（不得泄露敏感信息）
     * @param <T>       业务数据类型
     * @return 失败包装对象
     */
    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    /**
     * 失败响应，直接指定码值（用于无法枚举的错误码，谨慎使用）。
     *
     * @param code    业务状态码
     * @param message 友好文案
     * @param <T>     业务数据类型
     * @return 失败包装对象
     */
    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    /**
     * 链式设置链路追踪 ID（响应阶段由 RResponseAdvice 覆盖）。
     *
     * @param traceId 全局链路 ID
     * @return 当前对象（支持链式调用）
     */
    public R<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * 判断响应是否成功（code 等于 200）。
     *
     * @return true 表示成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return this.code != null && this.code.equals(BizCode.SUCCESS.getCode());
    }
}