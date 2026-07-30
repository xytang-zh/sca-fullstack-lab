package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常基类（双层响应码体系）。
 *
 * <p>字段：{@code bizCode}（BizCode 枚举）+ {@code devMessage}（dev 环境诊断信息，可选）。
 * message 由 {@code super(bizCode.message())} 传递，可通过构造器覆盖。
 *
 * <p>所有自定义异常必须继承本类，由 common-web 的 GlobalExceptionHandler 统一捕获。
 * 禁止用 {@code throw new RuntimeException(...)}。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final BizCode bizCode;
    private final String devMessage;

    public BusinessException(BizCode bizCode) {
        super(bizCode.message());
        this.bizCode = bizCode;
        this.devMessage = null;
    }

    public BusinessException(BizCode bizCode, String message) {
        super(message);
        this.bizCode = bizCode;
        this.devMessage = null;
    }

    public BusinessException(BizCode bizCode, String message, String devMessage) {
        super(message);
        this.bizCode = bizCode;
        this.devMessage = devMessage;
    }

    public BusinessException(BizCode bizCode, Throwable cause) {
        super(bizCode.message(), cause);
        this.bizCode = bizCode;
        this.devMessage = cause != null ? cause.toString() : null;
    }

    public int httpCode() {
        return bizCode.httpCode();
    }

    public String code() {
        return bizCode.code();
    }
}
