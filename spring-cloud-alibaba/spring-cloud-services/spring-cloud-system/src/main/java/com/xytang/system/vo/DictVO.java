package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典出参。
 */
@Data
@Builder
@Schema(description = "字典视图对象")
public class DictVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典 ID */
    private Long id;
    /** 字典类型编码 */
    private String dictType;
    /** 字典标签 */
    private String dictLabel;
    /** 字典键值 */
    private String dictValue;
    /** 排序号 */
    private Integer sort;
    /** 状态 */
    private Integer status;
    /** 备注 */
    private String remark;
}
