package com.xytang.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@Schema(description = "验证码返回")
public class CaptchaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码 Key（UUID，提交登录时回传）")
    private String captchaKey;

    @Schema(description = "验证码图片（data:image/png;base64,...）")
    private String captchaImg;
}
