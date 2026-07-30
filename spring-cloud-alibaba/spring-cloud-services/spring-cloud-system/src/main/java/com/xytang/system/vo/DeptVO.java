package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Schema(description = "部门视图对象（含子节点树）")
public class DeptVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String ancestors;
    private String deptName;
    private String deptCode;
    private String leader;
    private String phone;
    private Integer sort;
    private Integer status;
    private List<DeptVO> children;
}
