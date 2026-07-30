package com.xytang.auth.event;

import com.xytang.common.core.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 用户登录事件（异步发送到 MQ Exchange = user.login / log.login.create）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLoginEvent extends BaseEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private Integer loginType;
    private String deviceType;
    private String ip;
    private String userAgent;
    private Boolean success;
    private String failReason;
    private LocalDateTime loginTime;
}
