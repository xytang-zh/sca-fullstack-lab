package com.xytang.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论审核入参（管理员）。
 */
@Data
@Schema(description = "评论审核参数")
public class CommentAuditDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "审核结果：2=通过 3=驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核结果不能为空")
    @Min(value = 2, message = "审核结果仅支持 2=通过 3=驳回")
    @Max(value = 3, message = "审核结果仅支持 2=通过 3=驳回")
    private Integer status;
}