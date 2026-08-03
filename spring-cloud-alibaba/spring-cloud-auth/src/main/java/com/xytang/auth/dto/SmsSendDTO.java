package com.xytang.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 短信验证码发送参数：手机号 + 滑块验证凭据。
 */
@Data
@Schema(description = "短信验证码发送参数")
public class SmsSendDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "滑块验证码凭据（POST /captcha/check 获取，一次性）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "滑块验证码凭据不能为空")
    private String checkToken;
}
