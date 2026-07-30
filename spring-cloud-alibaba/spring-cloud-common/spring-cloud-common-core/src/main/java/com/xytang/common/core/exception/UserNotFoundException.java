package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 用户不存在异常（HTTP 404 + bizCode "02101"）。
 *
 * <p>注意：auth 登录链路调用方应使用 {@link AuthException}(BizCode.AUTH_USER_NOT_FOUND)
 * 以模糊化"用户名或密码错误"防账号枚举；system 模块查询用户时直接抛本类。
 */
public class UserNotFoundException extends BizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserNotFoundException() {
        super(BizCode.SYS_USER_NOT_FOUND);
    }

    public UserNotFoundException(String message) {
        super(BizCode.SYS_USER_NOT_FOUND, message);
    }

    public UserNotFoundException(String message, String devMessage) {
        super(BizCode.SYS_USER_NOT_FOUND, message, devMessage);
    }
}
