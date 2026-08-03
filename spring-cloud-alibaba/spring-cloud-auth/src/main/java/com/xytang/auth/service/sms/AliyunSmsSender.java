package com.xytang.auth.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信通道（预留）：配置 auth.sms.provider=aliyun 时启用。
 *
 * <p>接入步骤：1) 父 POM 声明 aliyun sms SDK 版本；2) 配置 accessKey/签名/模板；
 * 3) 在本类 send 中调用 SDK。当前仅占位告警，避免误发。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.sms.provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    @Override
    public void send(String phone, String code) {
        log.warn("[SMS][ALIYUN] 阿里云短信通道尚未接入，无法向 {} 发送验证码 {}", phone, code);
    }
}
