package com.xytang.auth.service;

import com.xytang.auth.vo.CaptchaVO;

/**
 * 验证码服务：生成 4 位字母数字混合图片，存 Redis（TTL 5min），校验后删除
 */
public interface CaptchaService {

    CaptchaVO generate();

    /**
     * @param captchaKey 验证码 Key
     * @param input      用户输入
     * @return 校验通过 true；Key 不存在或值不匹配 false
     */
    boolean verify(String captchaKey, String input);
}
