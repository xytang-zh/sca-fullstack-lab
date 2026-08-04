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
 * 系统参数实体。
 */
@Data
@TableName("sys_param")
public class Param implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 参数 ID（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 参数键（唯一，业务读取标识） */
    private String paramKey;
    /** 参数值 */
    private String paramValue;
    /** 参数类型（如 1=字符串 2=数字 3=布尔） */
    private Integer paramType;
    /** 备注 */
    private String remark;

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
