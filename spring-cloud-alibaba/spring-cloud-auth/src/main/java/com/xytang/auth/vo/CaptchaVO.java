package com.xytang.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文字图形验证码返回结构。
 */
@Data
@Builder
@Schema(description = "文字图形验证码返回")
public class CaptchaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码 Key（提交登录时回传，用于匹配答案）")
    private String captchaKey;

    @Schema(description = "验证码图片（base64 data URI）")
    private String imageBase64;
}
