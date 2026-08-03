package com.xytang.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登入参数：账号密码 + 滑块校验凭据。
 */
@Data
@Schema(description = "登入参数")
public class LoginDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 8, max = 32)
    private String password;

    @Schema(description = "滑块校验通过后签发的一次性凭据（POST /captcha/check 获取，可选，首次登录可不传）")
    private String checkToken;

    @Schema(description = "记住我（true=7天，false=30分钟）")
    private Boolean rememberMe = Boolean.FALSE;
}
