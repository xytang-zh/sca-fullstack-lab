package com.xytang.article.service;

import com.xytang.article.dto.ArticleCreateDTO;
import com.xytang.article.dto.ArticlePageQuery;
import com.xytang.article.vo.ArticleDetailVO;
import com.xytang.article.vo.ArticleVO;
import com.xytang.common.core.response.PageVO;

/**
 * 博客文章服务：游客分页浏览（时间/热度）、详情、发布、点赞/收藏（幂等）。
 */
public interface ArticleService {

    /**
     * 已发布文章分页（游客可访问）：sort=time 按发布时间倒序，hot 按加权热度降序。
     *
     * @param query 分页与排序参数
     * @return 分页结果
     */
    PageVO<ArticleVO> page(ArticlePageQuery query);

    /**
     * 文章详情（游客可访问），阅读量 +1。
     *
     * @param id 文章 ID
     * @return 详情 VO
     */
    ArticleDetailVO detail(Long id);

    /**
     * 发布文章（需登录）：MVP 直接置为已发布，审核流程由后续变更承接。
     *
     * @param dto      文章内容
     * @param authorId 作者（当前登录用户）
     * @return 列表项 VO
     */
    ArticleVO create(ArticleCreateDTO dto, Long authorId);

    /**
     * 点赞/取消（幂等）：第一次点赞返回 true，再次执行取消返回 false。
     *
     * @param articleId 文章 ID
     * @param userId    当前用户
     * @return true=本次已点赞，false=本次已取消
     */
    boolean toggleLike(Long articleId, Long userId);

    /**
     * 收藏/取消（幂等）：第一次收藏返回 true，再次执行取消返回 false。
     *
     * @param articleId 文章 ID
     * @param userId    当前用户
     * @return true=本次已收藏，false=本次已取消
     */
    boolean toggleFavorite(Long articleId, Long userId);
}
