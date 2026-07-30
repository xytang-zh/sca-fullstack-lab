package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * 公开内容状态变更非法（HTTP 400 + bizCode "00203"）
 */
public class ContentStatusTransitionException extends BizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ContentStatusTransitionException() {
        super(BizCode.CONTENT_STATUS_INVALID);
    }

    public ContentStatusTransitionException(String message) {
        super(BizCode.CONTENT_STATUS_INVALID, message);
    }

    public ContentStatusTransitionException(String message, String devMessage) {
        super(BizCode.CONTENT_STATUS_INVALID, message, devMessage);
    }
}
