package com.xytang.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体。
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录账号（唯一） */
    private String username;
    /** 密码（Argon2id 哈希，禁止明文存储） */
    private String password;
    /** 昵称 */
    private String nickname;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 头像 URL */
    private String avatar;
    /** 个人简介 */
    private String bio;
    /** 所属部门 ID */
    private Long deptId;
    /** 状态（1=待激活 2=正常 3=禁用 4=锁定 5=已删除，见 CommonConstants） */
    private Integer status;
    /** 登录失败计数（用于触发锁定） */
    private Integer failCount;
    /** 锁定截止时间（null 表示未锁定） */
    private LocalDateTime lockUntil;
    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
    /** 最后登录 IP */
    private String lastLoginIp;

    /** 创建人 ID（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private Long creator;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人 ID（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 乐观锁版本号（并发更新冲突检测） */
    @Version
    private Integer version;

    /** 逻辑删除标记（0=未删 1=已删） */
    @TableLogic
    private Integer deleted;
}
