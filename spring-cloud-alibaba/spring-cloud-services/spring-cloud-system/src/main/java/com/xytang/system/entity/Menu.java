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
 * 菜单实体。
 */
@Data
@TableName("sys_menu")
public class Menu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单 ID（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父菜单 ID（顶级为 0） */
    private Long parentId;
    /** 菜单名称 */
    private String menuName;
    /** 菜单类型（1=目录 2=菜单 3=按钮） */
    private Integer menuType;
    /** 路由地址（前端路由用） */
    private String path;
    /** 前端组件路径（如 system/user/index） */
    private String component;
    /** 权限标识（如 system:user:add，按钮级权限用） */
    private String perms;
    /** 菜单图标 */
    private String icon;
    /** 排序号（越小越靠前） */
    private Integer sort;
    /** 是否可见（1=显示 0=隐藏） */
    private Integer visible;
    /** 状态（2=正常 3=禁用，见 CommonConstants） */
    private Integer status;

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

    /** 乐观锁版本号（并发更新时校验） */
    @Version
    private Integer version;

    /** 逻辑删除标记（0=未删 1=已删） */
    @TableLogic
    private Integer deleted;
}
