package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 业务类异常（data-model.md §5）
 */
public class BizException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BizException(BizCode bizCode) {
        super(bizCode);
    }

    public BizException(BizCode bizCode, String message) {
        super(bizCode, message);
    }

    public BizException(BizCode bizCode, String message, String devMessage) {
        super(bizCode, message, devMessage);
    }
}
