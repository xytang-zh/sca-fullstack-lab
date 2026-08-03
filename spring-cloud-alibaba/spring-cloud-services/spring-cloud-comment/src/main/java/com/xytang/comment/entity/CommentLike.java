package com.xytang.comment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论点赞记录（user_id + comment_id 唯一，保证幂等）。
 */
@Data
@TableName("t_comment_like")
public class CommentLike implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long commentId;
    private Long userId;
    private LocalDateTime createTime;
}