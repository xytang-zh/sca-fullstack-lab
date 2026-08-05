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

    /** 登录账号（唯一） */
    private String username;
    /** Argon2id 密码哈希（含盐与参数，禁止明文） */
    private String password;
    /** 昵称 */
    private String nickname;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 头像 URL */
    private String avatar;
    /** 部门 ID */
    private Long deptId;
    /** 状态：1=正常 0=禁用 */
    private Integer status;
    /** 登录失败累计次数 */
    private Integer failCount;
    /** 锁定截止时间（登录风控用） */
    private LocalDateTime lockUntil;
    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
    /** 最后登录 IP */
    private String lastLoginIp;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
