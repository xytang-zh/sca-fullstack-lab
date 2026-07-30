package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 账号锁定异常（连续登录失败 5 次后触发；HTTP 423 + bizCode "01302"）
 */
public class AccountLockedException extends AuthException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AccountLockedException() {
        super(BizCode.AUTH_USER_LOCKED);
    }

    public AccountLockedException(String message) {
        super(BizCode.AUTH_USER_LOCKED, message);
    }

    public AccountLockedException(String message, String devMessage) {
        super(BizCode.AUTH_USER_LOCKED, message, devMessage);
    }
}
