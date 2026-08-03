package com.xytang.auth.dto;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 滑块校验入参（tianai SDK validCaptcha 回调体）。
 */
@Data
@Schema(description = "滑块校验入参")
public class CaptchaCheckDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String id;

    @Schema(description = "滑动轨迹", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private ImageCaptchaTrack data;
}
