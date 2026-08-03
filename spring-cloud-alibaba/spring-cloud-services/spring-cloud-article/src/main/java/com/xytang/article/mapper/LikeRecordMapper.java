package com.xytang.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xytang.article.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章点赞记录 Mapper。
 */
@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {
}
