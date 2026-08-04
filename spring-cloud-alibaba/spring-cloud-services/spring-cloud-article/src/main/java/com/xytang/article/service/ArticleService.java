package com.xytang.article.service;

import com.xytang.article.dto.ArticleCreateDTO;
import com.xytang.article.dto.ArticlePageQuery;
import com.xytang.article.vo.ArticleDetailVO;
import com.xytang.article.vo.ArticleStatsVO;
import com.xytang.article.vo.ArticleVO;
import com.xytang.common.core.response.PageResult;

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
    PageResult<ArticleVO> page(ArticlePageQuery query);

    /**
     * 文章详情（游客可访问），阅读量 +1。
     *
     * @param id 文章 ID
     * @return 详情 VO
     */
    ArticleDetailVO detail(Long id);

    /**
     * 发布文章（需登录）。
     *
     * @param dto      文章内容（含状态：草稿/发布）
     * @param authorId 作者（当前登录用户）
     * @return 列表项 VO
     */
    ArticleVO create(ArticleCreateDTO dto, Long authorId);

    /**
     * 更新文章（仅作者）。
     *
     * @param dto       文章内容
     * @param articleId 文章 ID
     * @param userId    当前登录用户
     * @return 更新后的列表项 VO
     */
    ArticleVO update(ArticleCreateDTO dto, Long articleId, Long userId);

    /**
     * 我的已发布文章（登录用户，按用户隔离）。
     *
     * @param userId 当前登录用户
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    PageResult<ArticleVO> pageMyArticles(Long userId, int page, int size);

    /**
     * 我的草稿（登录用户，按用户隔离）。
     *
     * @param userId 当前登录用户
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    PageResult<ArticleVO> pageMyDrafts(Long userId, int page, int size);

    /**
     * 我点赞的文章（登录用户，按用户隔离）。
     *
     * @param userId 当前登录用户
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    PageResult<ArticleVO> pageMyLikes(Long userId, int page, int size);

    /**
     * 我收藏的文章（登录用户，按用户隔离）。
     *
     * @param userId 当前登录用户
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    PageResult<ArticleVO> pageMyFavorites(Long userId, int page, int size);

    /**
     * 删除文章（仅作者，软删除）。
     *
     * @param articleId 文章 ID
     * @param userId    当前登录用户
     */
    void delete(Long articleId, Long userId);

    /**
     * 获取文章用于编辑（仅作者，含草稿/待审核状态）。
     *
     * @param articleId 文章 ID
     * @param userId    当前登录用户
     * @return 编辑用详情
     */
    ArticleDetailVO getForEdit(Long articleId, Long userId);

    /**
     * 文章统计（管理员）。
     *
     * @return 统计 VO
     */
    ArticleStatsVO stats();

    /**
     * 待审核文章分页（管理员）。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<ArticleVO> pagePending(int page, int size);

    /**
     * 审核文章（管理员，3=通过 4=驳回）。
     *
     * @param articleId 文章 ID
     * @param status    审核结果
     */
    void audit(Long articleId, Integer status);

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