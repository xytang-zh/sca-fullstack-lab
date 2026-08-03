package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统参数出参。
 */
@Data
@Builder
@Schema(description = "参数视图对象")
public class ParamVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String paramKey;
    private String paramValue;
    private Integer paramType;
    private String remark;
}
