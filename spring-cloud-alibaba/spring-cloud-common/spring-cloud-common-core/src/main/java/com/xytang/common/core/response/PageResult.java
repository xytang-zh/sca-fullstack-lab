package com.xytang.common.core.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页出参（对齐《响应体字段设计.md》§4）。
 *
 * @param <T> 列表元素类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页大小 */
    private int size;

    /** 总页数 */
    private int pages;

    /** 是否有上一页 */
    private boolean hasPrevious;

    /** 是否有下一页 */
    private boolean hasNext;

    /**
     * 构建分页结果：由总数与分页参数计算总页数、是否含上一页/下一页。
     *
     * @param records 当前页数据
     * @param total   总记录数
     * @param page    当前页码（从 1 开始）
     * @param size    每页大小
     * @param <T>    列表元素类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        int pages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return PageResult.<T>builder()
            .records(records)
            .total(total)
            .page(page)
            .size(size)
            .pages(pages)
            .hasPrevious(page > 1)
            .hasNext(page < pages)
            .build();
    }

    /**
     * 构建空分页结果（无数据时返回，避免前端判空）。
     *
     * @param page 请求的页码
     * @param size 请求的每页大小
     * @param <T>  列表元素类型
     * @return 空分页结果对象
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return PageResult.<T>builder()
            .records(List.of())
            .total(0L)
            .page(page)
            .size(size)
            .pages(0)
            .hasPrevious(false)
            .hasNext(false)
            .build();
    }
}