package com.xytang.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 新增用户入参。
 */
@Data
public class UserCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录账号（3-64 位，仅允许字母/数字/下划线/中划线） */
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 64, message = "账号长度必须在 3-64 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "账号仅允许字母/数字/下划线/中划线")
    private String username;

    /** 登录密码（8-64 位，入库前统一 Argon2id 加密） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在 8-64 之间")
    private String password;

    /** 昵称（展示名） */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    /** 邮箱（可选） */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    /** 手机号（可选） */
    @Pattern(regexp = "^(\\+?\\d{1,3}-)?1\\d{10}$|^$", message = "手机号格式不正确")
    private String phone;

    /** 所属部门 ID（可选） */
    private Long deptId;

    /** 初始角色 ID 列表（可选，空则默认普通用户） */
    private List<Long> roleIds;
}
