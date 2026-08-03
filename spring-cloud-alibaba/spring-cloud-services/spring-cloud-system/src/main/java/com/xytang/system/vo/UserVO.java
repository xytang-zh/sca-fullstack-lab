package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户出参。
 */
@Data
@Builder
@Schema(description = "用户视图对象（敏感字段已脱敏）")
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱（脱敏：x****@example.com）")
    private String email;

    @Schema(description = "手机号（脱敏：138****8888）")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "关注数")
    private Long followCount;

    @Schema(description = "粉丝数")
    private Long followerCount;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "状态：1=待激活 2=正常 3=禁用 4=锁定 5=已删除")
    private Integer status;

    @Schema(description = "角色码列表")
    private List<String> roles;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录 IP")
    private String lastLoginIp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
