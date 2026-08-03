package com.xytang.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xytang.system.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注关系 Mapper。
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}