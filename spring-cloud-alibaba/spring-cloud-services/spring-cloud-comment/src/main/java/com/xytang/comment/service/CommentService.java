package com.xytang.comment.service;

import com.xytang.comment.dto.CommentAuditDTO;
import com.xytang.comment.dto.CommentCreateDTO;
import com.xytang.comment.dto.CommentReplyDTO;
import com.xytang.comment.vo.CommentMyVO;
import com.xytang.comment.vo.CommentVO;
import com.xytang.common.core.response.PageVO;

/**
 * 博客评论服务：游客读列表，登录发表/回复/点赞，管理员审核。
 */
public interface CommentService {

    /**
     * 文章评论分页（游客可访问，仅已审核，按 parent_id 分组）。
     *
     * @param articleId     文章 ID
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @param currentUserId 当前登录用户 ID（可空，用于标注是否已点赞）
     * @return 分页结果
     */
    PageVO<CommentVO> pageByArticle(Long articleId, int pageNum, int pageSize, Long currentUserId);

    /**
     * 发表评论（登录用户，默认待审核）。
     *
     * @param dto    评论内容
     * @param userId 当前登录用户
     * @param ip     IP
     * @param ua     User-Agent
     * @return 评论 VO
     */
    CommentVO create(CommentCreateDTO dto, Long userId, String ip, String ua);

    /**
     * 回复评论（登录用户，二级嵌套回复，默认待审核）。
     *
     * @param commentId 被回复的评论 ID
     * @param dto       回复内容
     * @param userId    当前登录用户
     * @param ip        IP
     * @param ua        User-Agent
     * @return 评论 VO
     */
    CommentVO reply(Long commentId, CommentReplyDTO dto, Long userId, String ip, String ua);

    /**
     * 评论点赞/取消（幂等）。
     *
     * @param commentId 评论 ID
     * @param userId    当前登录用户
     * @return true=本次已点赞，false=本次已取消
     */
    boolean toggleLike(Long commentId, Long userId);

    /**
     * 我的评论（登录用户，含所属文章，按用户隔离）。
     *
     * @param userId   当前登录用户
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageVO<CommentMyVO> pageMyComments(Long userId, int pageNum, int pageSize);

    /**
     * 待审核评论列表（管理员）。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageVO<CommentVO> pagePending(int pageNum, int pageSize);

    /**
     * 审核评论（管理员，通过/驳回）。
     *
     * @param commentId 评论 ID
     * @param dto       审核结果
     */
    void audit(Long commentId, CommentAuditDTO dto);
}