package com.xytang.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章列表查询：游客可访问，支持时间/热度排序。
 */
@Data
@Schema(description = "文章列表查询参数")
public class ArticlePageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码（从 1 开始）", defaultValue = "1")
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    @Min(value = 1, message = "每页条数不能小于 1")
    private Integer pageSize = 10;

    @Schema(description = "排序方式：time=按发布时间倒序，hot=按热度降序", defaultValue = "time")
    @Pattern(regexp = "^(time|hot)$", message = "sort 仅支持 time 或 hot")
    private String sort = "time";

    @Schema(description = "作者 ID 过滤（逗号分隔，可选，用于关注 Feed）")
    private String authorIds;
}
