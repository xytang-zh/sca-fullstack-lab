package com.xytang.auth.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.CaptchaResponse;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * 行为滑块验证码服务：生成滑块、轨迹校验、签发一次性 checkToken。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CHECK_TOKEN_PREFIX = "auth:captcha:check:";
    private static final Duration CHECK_TOKEN_TTL = Duration.ofSeconds(60);

    private final ImageCaptchaApplication imageCaptchaApplication;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaVO generate() {
        CaptchaResponse<ImageCaptchaVO> response =
                imageCaptchaApplication.generateCaptcha(CaptchaTypeConstant.SLIDER);
        ImageCaptchaVO captcha = response.getCaptcha();
        return CaptchaVO.builder()
                .captchaId(response.getId())
                .id(response.getId())
                .type(captcha.getType())
                .backgroundImage(captcha.getBackgroundImage())
                .templateImage(captcha.getTemplateImage())
                .backgroundImageWidth(captcha.getBackgroundImageWidth())
                .backgroundImageHeight(captcha.getBackgroundImageHeight())
                .templateImageWidth(captcha.getTemplateImageWidth())
                .templateImageHeight(captcha.getTemplateImageHeight())
                .build();
    }

    @Override
    public String check(String captchaId, ImageCaptchaTrack track) {
        if (!StringUtils.hasText(captchaId) || track == null) {
            return null;
        }
        try {
            ApiResponse<?> response = imageCaptchaApplication.matching(captchaId, track);
            if (!response.isSuccess()) {
                log.warn("[Auth] captcha track check failed: captchaId={} code={} msg={} points={} firstX={} lastX={}",
                        captchaId, response.getCode(), response.getMsg(),
                        track.getTrackList() == null ? -1 : track.getTrackList().size(),
                        track.getTrackList() == null || track.getTrackList().isEmpty()
                            ? null : track.getTrackList().get(0).getX(),
                        track.getTrackList() == null || track.getTrackList().isEmpty()
                            ? null : track.getTrackList().get(track.getTrackList().size() - 1).getX());
                return null;
            }
        } catch (RuntimeException e) {
            log.warn("[Auth] captcha matching failed: captchaId={}", captchaId, e);
            return null;
        }
        String checkToken = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                CHECK_TOKEN_PREFIX + checkToken, "1", CHECK_TOKEN_TTL);
        return checkToken;
    }

    @Override
    public boolean verifyCheckToken(String checkToken) {
        if (!StringUtils.hasText(checkToken)) {
            return false;
        }
        String redisKey = CHECK_TOKEN_PREFIX + checkToken;
        Boolean deleted = stringRedisTemplate.delete(redisKey);
        return Boolean.TRUE.equals(deleted);
    }
}
