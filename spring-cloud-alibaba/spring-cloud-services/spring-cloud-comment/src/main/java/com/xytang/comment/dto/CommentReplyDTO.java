package com.xytang.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 回复评论入参（登录用户，二级嵌套回复）。
 */
@Data
@Schema(description = "回复评论参数")
public class CommentReplyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属文章 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    @Schema(description = "所属文章标题（冗余，便于我的评论展示）")
    private String articleTitle;

    @Schema(description = "父评论 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父评论 ID 不能为空")
    private Long parentId;

    @Schema(description = "被回复评论者昵称")
    @Size(max = 64)
    private String replyTo;

    @Schema(description = "回复内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2000, message = "回复最长 2000 字符")
    private String content;

    @Schema(description = "回复者昵称")
    @Size(max = 64)
    private String nickname;

    @Schema(description = "回复者头像")
    private String avatar;
}