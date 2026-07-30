package com.xytang.system.dto;

import com.xytang.common.core.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery {

    private Long deptId;
    private Integer status;
}
