package com.xytang.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客专栏（用户自定义博客分类）。
 *
 * <p>一个用户可创建多个专栏，文章通过 column_id 归属专栏；
 * 删除专栏只解除关联不清空文章，专栏删除后其下文章仍可正常访问。</p>
 */
@Data
@TableName("t_column")
public class Column implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 专栏 ID（雪花 ID） */
    @TableId
    private Long id;

    /** 创建者（作者）用户 ID */
    private Long userId;

    /** 专栏名称 */
    private String name;

    /** 专栏简介 */
    private String description;

    /** 封面图 URL */
    private String coverImage;

    /** 状态：1=正常（当前仅启用该状态，预留扩展） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号（MyBatis-Plus @Version 防并发覆盖） */
    @Version
    private Integer version;

    /** 逻辑删除标记：0=未删 1=已删 */
    @TableLogic
    private Integer deleted;
}