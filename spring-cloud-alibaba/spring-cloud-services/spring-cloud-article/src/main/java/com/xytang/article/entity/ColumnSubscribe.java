package com.xytang.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 专栏订阅记录（user_id + column_id 唯一，保证幂等）。
 */
@Data
@TableName("t_column_subscribe")
public class ColumnSubscribe implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long userId;
    private Long columnId;
    private LocalDateTime createTime;
}