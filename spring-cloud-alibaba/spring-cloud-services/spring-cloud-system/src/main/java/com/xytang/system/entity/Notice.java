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
 * 通知公告实体。
 */
@Data
@TableName("sys_notice")
public class Notice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知 ID（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 通知标题 */
    private String noticeTitle;
    /** 通知类型（如 1=公告 2=通知） */
    private Integer noticeType;
    /** 通知内容 */
    private String noticeContent;
    /** 状态（2=已发布 3=已撤回，其余为草稿/未发布，见 NoticeServiceImpl） */
    private Integer status;
    /** 发布时间（发布成功时写入） */
    private LocalDateTime publishTime;

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
