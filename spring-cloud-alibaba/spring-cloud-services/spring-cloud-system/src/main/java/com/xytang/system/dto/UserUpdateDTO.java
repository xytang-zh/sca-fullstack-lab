package com.xytang.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 更新用户入参。
 */
@Data
public class UserUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID（必填，定位要更新的用户） */
    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    /** 昵称（可选，仅更新传入的非空字段） */
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    /** 邮箱（可选） */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    /** 手机号（可选） */
    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    /** 个人简介（可选） */
    @Size(max = 512, message = "个人简介长度不能超过 512")
    private String bio;

    /** 所属部门 ID（可选） */
    private Long deptId;

    /** 状态（可选，2=正常 3=禁用 5=已删除） */
    private Integer status;

    /** 角色 ID 列表（可选） */
    private List<Long> roleIds;

    /** 乐观锁版本号（可选，用于并发控制） */
    private Integer version;
}
