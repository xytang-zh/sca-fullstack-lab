package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 禁止删除最后一个超级管理员（HTTP 409 + bizCode "02103"）
 */
public class LastSuperAdminException extends BizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LastSuperAdminException() {
        super(BizCode.SYS_LAST_ADMIN);
    }

    public LastSuperAdminException(String message) {
        super(BizCode.SYS_LAST_ADMIN, message);
    }

    public LastSuperAdminException(String message, String devMessage) {
        super(BizCode.SYS_LAST_ADMIN, message, devMessage);
    }
}
