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

    /** 点赞记录 ID（雪花 ID） */
    @TableId
    private Long id;

    /** 被点赞的评论 ID */
    private Long commentId;
    /** 点赞用户 ID（与 comment_id 组成唯一索引，保证一人对一评论最多一条记录，实现点赞幂等） */
    private Long userId;
    /** 点赞时间 */
    private LocalDateTime createTime;
}