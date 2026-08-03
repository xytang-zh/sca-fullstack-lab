package com.xytang.auth.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import cloud.tianai.captcha.validator.ImageCaptchaValidator;
import com.xytang.auth.service.impl.TrackNormalizeCaptchaValidator;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 行为验证码配置：业务开关 + 规则式轨迹验证器 + 背景图资源。
 */
@Configuration
@EnableConfigurationProperties(CaptchaConfig.CaptchaProperties.class)
public class CaptchaConfig {

    /**
     * 注册 SLIDER 背景图资源（DefaultBuiltInResources 只注册模板不注册背景图）。
     *
     * @return 含默认背景图的内存资源仓库
     */
    @Bean
    public ResourceStore captchaResourceStore() {
        LocalMemoryResourceStore store = new LocalMemoryResourceStore();
        store.addResource(CaptchaTypeConstant.SLIDER,
            new Resource("classpath", "META-INF/cut-image/resource/1.jpg"));
        return store;
    }

    @Bean
    public ImageCaptchaValidator imageCaptchaValidator() {
        return new TrackNormalizeCaptchaValidator();
    }

    /**
     * 验证码业务开关配置。
     */
    @Data
    @ConfigurationProperties(prefix = "auth.captcha")
    public static class CaptchaProperties {

        /**
         * 登录前是否强制校验行为验证码
         */
        private boolean enabled = true;
    }
}
