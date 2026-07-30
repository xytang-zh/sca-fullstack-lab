package com.xytang.common.core.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页出参（对齐 common-patterns.md §3.2）
 *
 * @param <T> 列表元素类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;
    private int pages;

    public static <T> PageVO<T> of(List<T> list, long total, int pageNum, int pageSize) {
        int pages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return PageVO.<T>builder()
            .list(list)
            .total(total)
            .pageNum(pageNum)
            .pageSize(pageSize)
            .pages(pages)
            .build();
    }

    public static <T> PageVO<T> empty(int pageNum, int pageSize) {
        return PageVO.<T>builder()
            .list(List.of())
            .total(0L)
            .pageNum(pageNum)
            .pageSize(pageSize)
            .pages(0)
            .build();
    }
}
