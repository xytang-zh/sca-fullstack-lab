package com.xytang.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发表评论入参（登录用户，作者昵称/头像由前端随单点登录态透传）。
 */
@Data
@Schema(description = "发表评论参数")
public class CommentCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属文章 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    @Schema(description = "所属文章标题（冗余，便于我的评论展示）")
    private String articleTitle;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论最长 2000 字符")
    private String content;

    @Schema(description = "评论者昵称")
    @Size(max = 64)
    private String nickname;

    @Schema(description = "评论者头像")
    private String avatar;
}