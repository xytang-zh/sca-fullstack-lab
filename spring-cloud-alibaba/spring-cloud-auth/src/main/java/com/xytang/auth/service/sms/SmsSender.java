package com.xytang.auth.service.sms;

/**
 * 短信发送通道抽象：开发环境走 Mock，生产接入真实服务（如阿里云 SMS）。
 */
public interface SmsSender {

    /**
     * 发送短信验证码。
     *
     * @param phone 目标手机号
     * @param code  6 位数字验证码
     */
    void send(String phone, String code);
}
