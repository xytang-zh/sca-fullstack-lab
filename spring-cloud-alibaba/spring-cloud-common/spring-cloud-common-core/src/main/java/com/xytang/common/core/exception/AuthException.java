package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 认证类异常（data-model.md §5）
 */
public class AuthException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthException(BizCode bizCode) {
        super(bizCode);
    }

    public AuthException(BizCode bizCode, String message) {
        super(bizCode, message);
    }

    public AuthException(BizCode bizCode, String message, String devMessage) {
        super(bizCode, message, devMessage);
    }
}
