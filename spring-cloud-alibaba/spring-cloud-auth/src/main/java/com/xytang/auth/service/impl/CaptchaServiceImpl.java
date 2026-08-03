package com.xytang.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import com.xytang.auth.constant.AuthConstants;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 文字图形验证码服务：生成图片验证码，答案存 Redis（TTL 5 分钟），校验忽略大小写且一次性消费。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final int CAPTCHA_CODE_COUNT = 4;
    private static final int CAPTCHA_LINE_COUNT = 20;
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(AuthConstants.CAPTCHA_TTL_MINUTES);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaVO generate() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_CODE_COUNT, CAPTCHA_LINE_COUNT);
        String captchaKey = IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(
                AuthConstants.CAPTCHA_IMAGE_PREFIX + captchaKey, captcha.getCode(), CAPTCHA_TTL);
        return CaptchaVO.builder()
                .captchaKey(captchaKey)
                .imageBase64("data:image/png;base64," + Base64.encode(captcha.getImageBytes()))
                .build();
    }

    @Override
    public boolean verify(String captchaKey, String code) {
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(code)) {
            return false;
        }
        String redisKey = AuthConstants.CAPTCHA_IMAGE_PREFIX + captchaKey;
        String savedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (savedCode == null) {
            return false;
        }
        // 一次性消费：无论答案是否匹配都删除，防止对同一验证码暴力重试
        stringRedisTemplate.delete(redisKey);
        return savedCode.equalsIgnoreCase(code.trim());
    }
}
