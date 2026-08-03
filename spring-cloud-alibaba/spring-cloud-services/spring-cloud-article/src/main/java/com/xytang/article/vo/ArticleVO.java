package com.xytang.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章列表项（ID 为 String，避免 JS 精度丢失）。
 */
@Data
@Schema(description = "文章列表项")
public class ArticleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文章 ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "封面图 URL")
    private String coverImage;

    @Schema(description = "作者 ID")
    private String authorId;

    @Schema(description = "阅读量")
    private Long views;

    @Schema(description = "点赞数")
    private Long likes;

    @Schema(description = "收藏数")
    private Long favorites;

    @Schema(description = "评论数")
    private Long comments;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
