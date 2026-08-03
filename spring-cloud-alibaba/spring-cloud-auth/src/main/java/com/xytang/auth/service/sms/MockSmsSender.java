package com.xytang.auth.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发环境短信通道：仅输出日志，验证码可在日志/控制台查看，方便本地联调。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    @Override
    public void send(String phone, String code) {
        log.info("[SMS][MOCK] 向手机号 {} 发送验证码：{}（开发环境请以日志为准）", phone, code);
    }
}
