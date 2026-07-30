package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 乐观锁冲突异常（HTTP 409 + bizCode "00202"）
 */
public class OptimisticLockException extends BizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OptimisticLockException() {
        super(BizCode.OPTIMISTIC_LOCK);
    }

    public OptimisticLockException(String message) {
        super(BizCode.OPTIMISTIC_LOCK, message);
    }

    public OptimisticLockException(String message, String devMessage) {
        super(BizCode.OPTIMISTIC_LOCK, message, devMessage);
    }
}
