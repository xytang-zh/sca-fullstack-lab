package com.xytang.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xytang.article.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章收藏记录 Mapper。
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
