package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 系统类异常（data-model.md §5）
 */
public class SystemException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SystemException(BizCode bizCode) {
        super(bizCode);
    }

    public SystemException(BizCode bizCode, String message) {
        super(bizCode, message);
    }

    public SystemException(BizCode bizCode, String message, String devMessage) {
        super(bizCode, message, devMessage);
    }

    public SystemException(BizCode bizCode, Throwable cause) {
        super(bizCode, cause);
    }
}
