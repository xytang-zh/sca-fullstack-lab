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

    private Long deptId;
    private Integer status;
}
