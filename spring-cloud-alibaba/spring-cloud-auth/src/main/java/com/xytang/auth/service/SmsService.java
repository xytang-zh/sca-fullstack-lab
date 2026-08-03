package com.xytang.auth.service;

import com.xytang.auth.dto.SmsSendDTO;

/**
 * 短信验证码服务：滑块校验前置 + 频率限制 + 验证码生成发送。
 */
public interface SmsService {

    /**
     * 发送短信验证码：校验滑块 checkToken（一次性消费），60 秒内同号仅可发送一次。
     *
     * @param dto 手机号与滑块凭据
     */
    void sendCode(SmsSendDTO dto);
}
