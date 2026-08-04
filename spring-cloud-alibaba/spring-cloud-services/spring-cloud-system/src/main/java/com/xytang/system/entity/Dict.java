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
 * 字典实体。
 */
@Data
@TableName("sys_dict")
public class Dict implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典 ID（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 字典类型编码（如 sys_user_sex），同一类型下多个字典项 */
    private String dictType;
    /** 字典标签（展示给用户的文本） */
    private String dictLabel;
    /** 字典键值（业务实际使用的值） */
    private String dictValue;
    /** 排序号（越小越靠前） */
    private Integer sort;
    /** 状态（2=正常 3=禁用，见 CommonConstants） */
    private Integer status;
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
