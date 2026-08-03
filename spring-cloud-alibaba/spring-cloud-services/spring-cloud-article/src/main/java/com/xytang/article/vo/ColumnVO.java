package com.xytang.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 专栏出参（ID 为 String，避免 JS 精度丢失）。
 */
@Data
@Schema(description = "专栏")
public class ColumnVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "专栏 ID")
    private String id;

    @Schema(description = "作者 ID")
    private String userId;

    @Schema(description = "专栏名称")
    private String name;

    @Schema(description = "专栏简介")
    private String description;

    @Schema(description = "封面图 URL")
    private String coverImage;

    @Schema(description = "专栏下文章数")
    private Long articleCount;

    @Schema(description = "订阅数")
    private Long subscribeCount;

    @Schema(description = "当前用户是否已订阅")
    private Boolean subscribed;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}