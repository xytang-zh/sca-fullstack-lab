package com.xytang.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章收藏记录（user_id + article_id 唯一，保证幂等）。
 */
@Data
@TableName("t_favorite")
public class Favorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long articleId;
    private Long userId;
    private LocalDateTime createTime;
}
