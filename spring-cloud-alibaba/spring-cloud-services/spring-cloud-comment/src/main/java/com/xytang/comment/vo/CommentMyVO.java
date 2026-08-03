package com.xytang.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 我的评论出参（用户中心"回答"页，含所属文章标题）。
 */
@Data
@Schema(description = "我的评论")
public class CommentMyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评论 ID")
    private String id;

    @Schema(description = "所属文章 ID")
    private String articleId;

    @Schema(description = "所属文章标题")
    private String articleTitle;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞数")
    private Long likeCount;

    @Schema(description = "状态：1=待审核 2=已审核 3=已驳回")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}