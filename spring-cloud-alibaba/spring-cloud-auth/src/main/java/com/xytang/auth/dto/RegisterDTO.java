package com.xytang.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册参数：账号 + 密码 + 确认密码。
 */
@Data
@Schema(description = "注册参数")
public class RegisterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "账号（6-18 位，仅大小写英文字母与数字，必须以字母开头）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9]{5,17}$",
            message = "账号须为 6-18 位，仅含大小写英文字母与数字，且必须以字母开头")
    private String account;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 8, max = 32)
    private String password;

    @Schema(description = "确认密码，须与密码一致", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String confirmPassword;
}
