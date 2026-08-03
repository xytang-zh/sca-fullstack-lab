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

    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private Integer status;
    private String remark;
}
