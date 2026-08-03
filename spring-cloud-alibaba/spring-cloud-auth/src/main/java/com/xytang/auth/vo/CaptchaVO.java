package com.xytang.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行为滑块验证码返回结构。
 */
@Data
@Builder
@Schema(description = "行为滑块验证码返回")
public class CaptchaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验证码 ID（提交登录时回传）")
    private String captchaId;

    @Schema(description = "验证码 ID（tianai-captcha 前端 SDK 读取的字段）")
    private String id;

    @Schema(description = "验证码类型（SLIDER）")
    private String type;

    @Schema(description = "背景图（带缺口，base64）")
    private String backgroundImage;

    @Schema(description = "滑块图（base64）")
    private String templateImage;

    @Schema(description = "背景图宽度")
    private Integer backgroundImageWidth;

    @Schema(description = "背景图高度")
    private Integer backgroundImageHeight;

    @Schema(description = "滑块图宽度")
    private Integer templateImageWidth;

    @Schema(description = "滑块图高度")
    private Integer templateImageHeight;
}
