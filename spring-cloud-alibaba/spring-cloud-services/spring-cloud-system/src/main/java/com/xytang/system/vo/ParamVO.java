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

    /** 参数 ID */
    private Long id;
    /** 参数键 */
    private String paramKey;
    /** 参数值 */
    private String paramValue;
    /** 参数类型 */
    private Integer paramType;
    /** 备注 */
    private String remark;
}
