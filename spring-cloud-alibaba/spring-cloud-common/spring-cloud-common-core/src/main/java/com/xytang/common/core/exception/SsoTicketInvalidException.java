package com.xytang.common.core.exception;

import com.xytang.common.core.response.BizCode;

import java.io.Serial;

/**
 * SSO Ticket 无效或已过期（HTTP 401 + bizCode "01303"）
 */
public class SsoTicketInvalidException extends AuthException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SsoTicketInvalidException() {
        super(BizCode.AUTH_SSO_TICKET_INVALID);
    }

    public SsoTicketInvalidException(String message) {
        super(BizCode.AUTH_SSO_TICKET_INVALID, message);
    }

    public SsoTicketInvalidException(String message, String devMessage) {
        super(BizCode.AUTH_SSO_TICKET_INVALID, message, devMessage);
    }
}
