package com.xytang.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "当前用户信息")
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱（脱敏）")
    private String email;

    @Schema(description = "手机号（脱敏）")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "角色列表")
    private List<RoleBriefVO> roles;

    @Schema(description = "权限点列表")
    private List<String> perms;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录 IP")
    private String lastLoginIp;

    @Data
    @Builder
    @Schema(description = "角色简要信息")
    public static class RoleBriefVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private String code;
        private String name;
        private Integer dataScope;
    }
}
