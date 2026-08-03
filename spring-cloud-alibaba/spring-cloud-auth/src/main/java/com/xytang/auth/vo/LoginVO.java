package com.xytang.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录成功返回。
 */
@Data
@Builder
@Schema(description = "登录返回")
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Token 名称（HTTP Header 名）")
    private String tokenName;

    @Schema(description = "Token 值（含 Bearer 前缀）")
    private String tokenValue;

    @Schema(description = "过期秒数")
    private Long expiresIn;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "角色 code 列表")
    private List<String> roles;

    @Schema(description = "权限点列表")
    private List<String> perms;

    @Schema(description = "Refresh Token（7d 有效，用于静默续期）")
    private String refreshToken;
}
