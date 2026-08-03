package com.xytang.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论出参（文章详情评论区，ID 为 String）。
 */
@Data
@Schema(description = "评论")
public class CommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评论 ID")
    private String id;

    @Schema(description = "所属文章 ID")
    private String articleId;

    @Schema(description = "父评论 ID（0=一级评论）")
    private String parentId;

    @Schema(description = "被回复的评论者昵称（仅回复场景）")
    private String replyTo;

    @Schema(description = "评论者昵称")
    private String nickname;

    @Schema(description = "评论者头像")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞数")
    private Long likeCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}