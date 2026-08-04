package com.xytang.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 专栏订阅记录（user_id + column_id 唯一，保证幂等）。
 *
 * <p>订阅与取消订阅共用同一张表：存在记录即已订阅，再次点击删除记录即取消；
 * 唯一键兜底并发重复点击，重复插入会命中 DuplicateKeyException。</p>
 */
@Data
@TableName("t_column_subscribe")
public class ColumnSubscribe implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订阅记录 ID（雪花 ID） */
    @TableId
    private Long id;

    /** 订阅用户 ID */
    private Long userId;

    /** 被订阅的专栏 ID */
    private Long columnId;

    /** 订阅时间 */
    private LocalDateTime createTime;
}