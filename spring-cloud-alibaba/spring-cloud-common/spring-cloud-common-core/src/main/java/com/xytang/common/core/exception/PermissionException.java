package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 权限类异常（data-model.md §5）
 */
public class PermissionException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PermissionException(BizCode bizCode) {
        super(bizCode);
    }

    public PermissionException(BizCode bizCode, String message) {
        super(bizCode, message);
    }

    public PermissionException(BizCode bizCode, String message, String devMessage) {
        super(bizCode, message, devMessage);
    }
}
