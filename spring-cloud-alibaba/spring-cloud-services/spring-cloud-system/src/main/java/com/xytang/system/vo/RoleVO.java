package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色出参。
 */
@Data
@Builder
@Schema(description = "角色视图对象")
public class RoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer dataScope;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
