package com.xytang.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xytang.comment.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞 Mapper。
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
}