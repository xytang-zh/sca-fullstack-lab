package com.xytang.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xytang.article.entity.Article;
import com.xytang.article.vo.ArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文章 Mapper：分页查询支持时间/热度排序（热度权重参数化，防 SQL 注入）。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 分页查询已发布文章（游客可访问）。
     *
     * @param page     分页参数
     * @param sort     time=发布时间倒序，hot=热度降序
     * @param wViews   阅读量权重
     * @param wLikes   点赞数权重
     * @param wFav     收藏数权重
     * @param wComm    评论数权重
     * @return 文章列表分页
     */
    IPage<ArticleVO> selectPublishedPage(IPage<ArticleVO> page,
                                         @Param("sort") String sort,
                                         @Param("wViews") double wViews,
                                         @Param("wLikes") double wLikes,
                                         @Param("wFav") double wFav,
                                         @Param("wComm") double wComm);

    /**
     * 点赞/收藏后同步计数 +1（原子更新）。
     *
     * @param id      文章 ID
     * @param column  计数字段（likes/favorites）
     * @return 影响行数
     */
    int incrCount(@Param("id") Long id, @Param("column") String column);

    /**
     * 取消点赞/收藏后同步计数 -1（下限 0）。
     *
     * @param id     文章 ID
     * @param column 计数字段（likes/favorites）
     * @return 影响行数
     */
    int decrCount(@Param("id") Long id, @Param("column") String column);
}
