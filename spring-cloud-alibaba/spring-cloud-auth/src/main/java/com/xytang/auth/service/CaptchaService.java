package com.xytang.auth.service;

import com.xytang.auth.vo.CaptchaVO;

/**
 * 文字图形验证码服务：生成图片验证码，校验答案（忽略大小写、一次性消费）。
 */
public interface CaptchaService {

    CaptchaVO generate();

    /**
     * 校验并消费验证码答案。
     *
     * @param captchaKey 验证码 Key
     * @param code       用户输入的答案
     * @return 校验通过 true；key 不存在、已消费或答案错误 false
     */
    boolean verify(String captchaKey, String code);
}
