package com.xytang.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xytang.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 认证用用户查询 Mapper（CRUD 在 system 服务维护）。
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}
