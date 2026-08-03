package com.xytang.auth.service;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.xytang.auth.vo.CaptchaVO;

/**
 * 验证码服务：生成行为滑块验证码，轨迹校验后签发一次性 checkToken（登录使用）
 */
public interface CaptchaService {

    CaptchaVO generate();

    /**
     * 校验滑块轨迹，通过则签发一次性 checkToken（短 TTL，登录时消费）。
     *
     * @param captchaId 验证码 ID
     * @param track     用户滑动轨迹
     * @return checkToken；校验失败返回 null
     */
    String check(String captchaId, ImageCaptchaTrack track);

    /**
     * 校验并消费一次性 checkToken。
     *
     * @param checkToken 登录携带的凭据
     * @return 有效 true；不存在或已消费 false
     */
    boolean verifyCheckToken(String checkToken);
}
