package com.xytang.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 认证用用户实体（只读 sys_user，仅查询认证需要的字段）
 *
 * <p>用户的 CRUD 操作在 spring-cloud-system 服务中；本模块仅做登录校验与密码修改。
 */
@Data
@TableName("sys_user")
public class AuthUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Long deptId;
    private Integer status;
    private Integer failCount;
    private LocalDateTime lockUntil;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
