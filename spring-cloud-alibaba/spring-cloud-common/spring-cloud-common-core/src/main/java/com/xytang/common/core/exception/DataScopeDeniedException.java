package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 数据范围拒绝异常（HTTP 403 + bizCode "00302"）
 */
public class DataScopeDeniedException extends PermissionException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DataScopeDeniedException() {
        super(BizCode.DATA_SCOPE_DENIED);
    }

    public DataScopeDeniedException(String message) {
        super(BizCode.DATA_SCOPE_DENIED, message);
    }

    public DataScopeDeniedException(String message, String devMessage) {
        super(BizCode.DATA_SCOPE_DENIED, message, devMessage);
    }
}
