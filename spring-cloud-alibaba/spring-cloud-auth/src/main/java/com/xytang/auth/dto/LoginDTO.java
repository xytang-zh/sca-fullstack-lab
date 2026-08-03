package com.xytang.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登入参数：账号密码 + 文字图形验证码凭据。
 */
@Data
@Schema(description = "登入参数")
public class LoginDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 6, max = 18)
    private String account;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 8, max = 32)
    private String password;

    @Schema(description = "文字验证码 Key（GET /captcha 获取，校验忽略大小写、一次性消费）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String captchaKey;

    @Schema(description = "文字验证码答案", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String captchaCode;

    @Schema(description = "记住我（true=7天，false=30分钟）")
    private Boolean rememberMe = Boolean.FALSE;
}
