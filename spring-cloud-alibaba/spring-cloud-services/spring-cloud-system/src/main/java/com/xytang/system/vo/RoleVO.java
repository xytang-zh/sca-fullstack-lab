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

    /** 角色 ID */
    private Long id;
    /** 角色编码 */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 数据权限范围（1=全部 2=本部门及以下 3=本部门 4=仅本人 5=自定义） */
    private Integer dataScope;
    /** 排序号 */
    private Integer sort;
    /** 状态 */
    private Integer status;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
}
