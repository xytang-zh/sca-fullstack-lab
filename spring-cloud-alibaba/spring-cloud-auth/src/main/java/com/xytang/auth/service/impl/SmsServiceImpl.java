package com.xytang.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.xytang.auth.constant.AuthConstants;
import com.xytang.auth.dto.SmsSendDTO;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.service.SmsService;
import com.xytang.auth.service.sms.SmsSender;
import com.xytang.common.core.exception.AuthException;
import com.xytang.common.core.response.BizCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现：滑块前置校验、60s 频率限制、验证码 TTL 5 分钟。
 */
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private static final long CODE_TTL_SECONDS = 5 * 60L;
    private static final long SEND_INTERVAL_SECONDS = 60L;
    private static final int CODE_LENGTH = 6;

    private final CaptchaService captchaService;
    private final SmsSender smsSender;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void sendCode(SmsSendDTO dto) {
        // 1. 消费滑块 checkToken（一次性，校验失败即拒绝）
        if (!captchaService.verifyCheckToken(dto.getCheckToken())) {
            throw new AuthException(BizCode.AUTH_CAPTCHA_ERROR);
        }

        // 2. 60 秒频率限制
        String limitKey = AuthConstants.SMS_LIMIT_PREFIX + dto.getPhone();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            throw new AuthException(BizCode.RATE_LIMIT, "发送过于频繁，请稍后再试");
        }

        // 3. 生成验证码并存储（5 分钟有效）
        String code = RandomUtil.randomNumbers(CODE_LENGTH);
        stringRedisTemplate.opsForValue().set(
                AuthConstants.SMS_CODE_PREFIX + dto.getPhone(), code, CODE_TTL_SECONDS, TimeUnit.SECONDS);

        // 4. 记录发送间隔，防止 60 秒内重复发送
        stringRedisTemplate.opsForValue().set(limitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 5. 走短信通道发送
        smsSender.send(dto.getPhone(), code);
    }
}
