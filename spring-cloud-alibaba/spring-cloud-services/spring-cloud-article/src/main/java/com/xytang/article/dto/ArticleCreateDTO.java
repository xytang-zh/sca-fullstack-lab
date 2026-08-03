package com.xytang.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章创建参数（登录用户）。
 */
@Data
@Schema(description = "文章创建参数")
public class ArticleCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最长 128 字符")
    private String title;

    @Schema(description = "摘要")
    @Size(max = 512, message = "摘要最长 512 字符")
    private String summary;

    @Schema(description = "Markdown 正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "正文不能为空")
    private String contentMd;

    @Schema(description = "URL 友好标识（可空）")
    @Size(max = 255, message = "slug 最长 255 字符")
    private String slug;

    @Schema(description = "封面图 URL")
    private String coverImage;

    @Schema(description = "所属专栏 ID（可空）")
    private Long columnId;

    @Schema(description = "状态：1=草稿 3=发布（默认发布）", defaultValue = "3")
    private Integer status;
}
