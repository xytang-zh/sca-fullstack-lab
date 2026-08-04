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
 * 系统部门实体。
 */
@Data
@TableName("sys_dept")
public class Dept implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部门 ID（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父部门 ID（顶级为 0） */
    private Long parentId;
    /** 祖级路径（如 0,1,2），用于快速查询子孙部门，避免递归 */
    private String ancestors;
    /** 部门名称 */
    private String deptName;
    /** 部门编码（唯一） */
    private String deptCode;
    /** 部门负责人 */
    private String leader;
    /** 联系电话 */
    private String phone;
    /** 排序号（越小越靠前） */
    private Integer sort;
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
