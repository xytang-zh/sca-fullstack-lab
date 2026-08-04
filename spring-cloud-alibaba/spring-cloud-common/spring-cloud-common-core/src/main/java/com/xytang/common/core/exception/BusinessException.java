package com.xytang.common.core.exception;

import com.xytang.common.core.response.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常基类（单一业务码体系）。
 *
 * <p>字段：{@code errorCode}（ErrorCode 接口）+ {@code devMessage}（dev 环境诊断信息，可选）。
 * message 由 {@code super(errorCode.getUserMessage())} 传递，可通过构造器覆盖。
 *
 * <p>所有自定义异常必须继承本类，由 common-web 的 GlobalExceptionHandler 统一捕获。
 * 禁止用 {@code throw new RuntimeException(...)}。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String devMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getUserMessage());
        this.errorCode = errorCode;
        this.devMessage = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.devMessage = null;
    }

    public BusinessException(ErrorCode errorCode, String message, String devMessage) {
        super(message);
        this.errorCode = errorCode;
        this.devMessage = devMessage;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getUserMessage(), cause);
        this.errorCode = errorCode;
        this.devMessage = cause != null ? cause.toString() : null;
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public Integer getCode() {
        return errorCode.getCode();
    }

    public String getUserMessage() {
        return errorCode.getUserMessage();
    }
}