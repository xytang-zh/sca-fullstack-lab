package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 部门出参。
 */
@Data
@Builder
@Schema(description = "部门视图对象（含子节点树）")
public class DeptVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部门 ID */
    private Long id;
    /** 父部门 ID */
    private Long parentId;
    /** 祖级路径 */
    private String ancestors;
    /** 部门名称 */
    private String deptName;
    /** 部门编码 */
    private String deptCode;
    /** 部门负责人 */
    private String leader;
    /** 联系电话 */
    private String phone;
    /** 排序号 */
    private Integer sort;
    /** 状态 */
    private Integer status;
    /** 子部门列表（树形结构） */
    private List<DeptVO> children;
}
