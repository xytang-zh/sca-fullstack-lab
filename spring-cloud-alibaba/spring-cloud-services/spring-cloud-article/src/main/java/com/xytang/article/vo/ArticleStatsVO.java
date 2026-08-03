package com.xytang.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章统计出参（管理员统计页）。
 */
@Data
@Schema(description = "文章统计")
public class ArticleStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文章总数")
    private Long totalArticles;

    @Schema(description = "已发布文章数")
    private Long publishedArticles;

    @Schema(description = "待审核文章数")
    private Long pendingArticles;

    @Schema(description = "草稿数")
    private Long draftArticles;

    @Schema(description = "总点赞数")
    private Long totalLikes;

    @Schema(description = "总收藏数")
    private Long totalFavorites;
}