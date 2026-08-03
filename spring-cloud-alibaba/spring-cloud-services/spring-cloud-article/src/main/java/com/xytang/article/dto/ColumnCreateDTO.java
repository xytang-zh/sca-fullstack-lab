package com.xytang.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 专栏创建/编辑参数（登录用户）。
 */
@Data
@Schema(description = "专栏创建参数")
public class ColumnCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "专栏名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "专栏名称不能为空")
    @Size(max = 64, message = "专栏名称最长 64 字符")
    private String name;

    @Schema(description = "专栏简介")
    @Size(max = 512, message = "专栏简介最长 512 字符")
    private String description;

    @Schema(description = "封面图 URL")
    private String coverImage;
}