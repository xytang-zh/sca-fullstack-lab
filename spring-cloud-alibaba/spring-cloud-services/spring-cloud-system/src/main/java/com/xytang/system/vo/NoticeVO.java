package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知公告出参。
 */
@Data
@Builder
@Schema(description = "通知视图对象")
public class NoticeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String noticeTitle;
    private Integer noticeType;
    private String noticeContent;
    private Integer status;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
