package com.xytang.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xytang.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}
