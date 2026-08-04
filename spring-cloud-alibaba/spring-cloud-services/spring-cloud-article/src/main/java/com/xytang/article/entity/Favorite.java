package com.xytang.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章收藏记录（user_id + article_id 唯一，保证幂等）。
 *
 * <p>收藏与取消共用同一张表：存在记录即已收藏，再次点击删除记录即取消；
 * 唯一键兜底并发重复点击，重复插入会命中 DuplicateKeyException。</p>
 */
@Data
@TableName("t_favorite")
public class Favorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 收藏记录 ID（雪花 ID） */
    @TableId
    private Long id;

    /** 被收藏的文章 ID */
    private Long articleId;

    /** 收藏用户 ID */
    private Long userId;

    /** 收藏时间 */
    private LocalDateTime createTime;
}
