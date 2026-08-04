package com.xytang.common.core.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页入参基类（与出参 {@link PageResult} 字段名对齐）。
 *
 * <p>所有 *PageQuery DTO 应继承本类以复用分页字段与校验。
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须从 1 起")
    private Integer page = 1;

    @Min(value = 1, message = "每页大小至少 1")
    @Max(value = 100, message = "每页大小最大 100")
    private Integer size = 10;

    /** 排序字段，格式 {@code field asc,field2 desc}；为空表示默认排序 */
    private String orderBy;

    /** 关键字搜索（可选） */
    private String keyword;
}