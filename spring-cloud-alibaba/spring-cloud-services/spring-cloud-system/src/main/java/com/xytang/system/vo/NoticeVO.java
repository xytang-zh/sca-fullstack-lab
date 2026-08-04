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

    /** 通知 ID */
    private Long id;
    /** 通知标题 */
    private String noticeTitle;
    /** 通知类型 */
    private Integer noticeType;
    /** 通知内容 */
    private String noticeContent;
    /** 状态 */
    private Integer status;
    /** 发布时间 */
    private LocalDateTime publishTime;
    /** 创建时间 */
    private LocalDateTime createTime;
}
