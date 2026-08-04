package com.xytang.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章点赞记录（user_id + article_id 唯一，保证幂等）。
 *
 * <p>点赞与取消共用同一张表：存在记录即已点赞，再次点击删除记录即取消；
 * 唯一键兜底并发重复点击，重复插入会命中 DuplicateKeyException。</p>
 */
@Data
@TableName("t_like_record")
public class LikeRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 点赞记录 ID（雪花 ID） */
    @TableId
    private Long id;

    /** 被点赞的文章 ID */
    private Long articleId;

    /** 点赞用户 ID */
    private Long userId;

    /** 点赞时间 */
    private LocalDateTime createTime;
}
