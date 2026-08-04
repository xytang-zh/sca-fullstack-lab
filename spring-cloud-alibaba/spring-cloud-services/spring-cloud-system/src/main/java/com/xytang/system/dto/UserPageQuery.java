package com.xytang.system.dto;

import com.xytang.common.core.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery {

    /** 部门 ID 过滤条件 */
    private Long deptId;

    /** 状态过滤条件（2=正常 3=禁用） */
    private Integer status;
}
