package com.xytang.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xytang.comment.dto.CommentAuditDTO;
import com.xytang.comment.dto.CommentCreateDTO;
import com.xytang.comment.dto.CommentReplyDTO;
import com.xytang.comment.entity.Comment;
import com.xytang.comment.entity.CommentLike;
import com.xytang.comment.mapper.CommentLikeMapper;
import com.xytang.comment.mapper.CommentMapper;
import com.xytang.comment.service.CommentService;
import com.xytang.comment.vo.CommentMyVO;
import com.xytang.comment.vo.CommentVO;
import com.xytang.common.core.exception.BizException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.PageResult;
import com.xytang.common.dubbo.ArticleRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 博客评论服务实现。
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /** 评论状态：待审核 */
    private static final int STATUS_PENDING = 1;

    /** 评论状态：已审核（对外可见） */
    private static final int STATUS_APPROVED = 2;

    private final CommentMapper commentMapper;
    private final CommentLikeMapper likeMapper;

    /**
     * 文章存在性校验（Dubbo 远程调用 article 服务）。
     * 注：Dubbo 3.3.6 的 @DubboReference 不支持构造器参数注入，采用字段注入（Dubbo 官方惯例）。
     */
    @DubboReference
    private ArticleRpcService articleRpcService;

    /**
     * 分页查询文章已审核（APPROVED）评论，并批量标注当前用户的点赞态。
     *
     * @param articleId     文章 ID
     * @param page          页码（从 1 开始）
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID（游客为 null）
     * @return 评论分页结果
     */
    @Override
    public PageResult<CommentVO> pageByArticle(Long articleId, int page, int size, Long currentUserId) {
        // 只查询已审核（APPROVED）评论，待审核/驳回的评论不对游客与普通用户展示
        Page<Comment> commentPage = new Page<>(page, size);
        IPage<Comment> result = commentMapper.selectPage(commentPage, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, STATUS_APPROVED)
                .orderByAsc(Comment::getCreateTime));
        // 批量解析当前用户已点赞的评论 ID，避免逐条判断产生 N+1 查询
        Set<Long> likedIds = resolveLikedIds(result.getRecords(), currentUserId);
        List<CommentVO> list = result.getRecords().stream()
                .map(c -> toVO(c, likedIds))
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 发表一级评论，默认进入待审核（PENDING），入库前记录 IP 与 UA 用于反垃圾溯源。
     *
     * @param dto    评论入参（文章、内容、昵称、头像）
     * @param userId 当前登录用户 ID
     * @param ip     客户端真实 IP（反垃圾溯源）
     * @param ua     User-Agent（反垃圾溯源）
     * @return 评论 VO
     */
    @Override
    public CommentVO create(CommentCreateDTO dto, Long userId, String ip, String ua) {
        // 1. 校验所属文章存在（Dubbo 远程调用 article 服务），防止评论挂在已删除文章下
        requireArticleExists(dto.getArticleId());
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setArticleTitle(dto.getArticleTitle());
        comment.setUserId(userId);
        comment.setNickname(dto.getNickname());
        comment.setAvatar(dto.getAvatar());
        // 一级评论：父评论与被回复评论均置 0
        comment.setParentId(0L);
        comment.setReplyToId(0L);
        comment.setReplyToNickname("");
        // 安全约束：评论内容按需求规划应经敏感词 + XSS 过滤后存纯文本，骨架阶段直接落库，过滤逻辑待接入
        comment.setContent(dto.getContent());
        // 新评论默认待审核（PENDING），管理员审核通过后才对外可见
        comment.setStatus(STATUS_PENDING);
        comment.setIp(ip);
        comment.setUserAgent(ua);
        comment.setLikes(0L);
        commentMapper.insert(comment);
        return toVO(comment, Set.of());
    }

    /**
     * 回复评论：校验父评论与文章存在后写入二级评论，默认进入待审核（PENDING）。
     *
     * @param commentId 被回复的评论 ID（指向一级评论，形成二级嵌套）
     * @param dto      回复入参（文章、父评论、被回复者昵称、内容）
     * @param userId   当前登录用户 ID
     * @param ip       客户端真实 IP（反垃圾溯源）
     * @param ua       User-Agent（反垃圾溯源）
     * @return 评论 VO
     */
    @Override
    public CommentVO reply(Long commentId, CommentReplyDTO dto, Long userId, String ip, String ua) {
        // 1. 校验被回复的评论存在，防止回复已删除/不存在的评论
        Comment parent = requireComment(commentId);
        // 2. 校验所属文章存在（Dubbo 远程调用 article 服务）
        requireArticleExists(dto.getArticleId());
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setArticleTitle(dto.getArticleTitle());
        comment.setUserId(userId);
        comment.setNickname(dto.getNickname());
        comment.setAvatar(dto.getAvatar());
        // 二级回复：parent_id 指向一级评论，reply_to_id 指向被回复的评论（供 @ 通知联动）
        comment.setParentId(parent.getId());
        comment.setReplyToId(commentId);
        comment.setReplyToNickname(dto.getReplyTo());
        // 安全约束：评论内容按需求规划应经敏感词 + XSS 过滤后存纯文本，骨架阶段直接落库，过滤逻辑待接入
        comment.setContent(dto.getContent());
        // 新回复默认待审核（PENDING），管理员审核通过后才对外可见
        comment.setStatus(STATUS_PENDING);
        comment.setIp(ip);
        comment.setUserAgent(ua);
        comment.setLikes(0L);
        commentMapper.insert(comment);
        return toVO(comment, Set.of());
    }

    /**
     * 点赞/取消点赞（幂等）：已点赞则取消，未点赞则新增并计数 +1。
     *
     * <p>幂等与并发安全依赖 t_comment_like 表 (comment_id, user_id) 唯一索引：
     * 并发重复插入会命中唯一索引抛出 DuplicateKeyException，此时按"已点赞"兜底返回。</p>
     *
     * @param commentId 评论 ID
     * @param userId    当前登录用户 ID
     * @return true=本次执行后为已点赞，false=本次执行后为未点赞
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long commentId, Long userId) {
        requireComment(commentId);
        // 1. 查询当前用户是否已点赞（唯一索引保证同一用户对同一评论最多一条记录）
        CommentLike record = likeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId)
                .last("LIMIT 1"));
        if (record == null) {
            // 2. 未点赞：插入点赞记录并给评论点赞数 +1
            CommentLike insert = new CommentLike();
            insert.setCommentId(commentId);
            insert.setUserId(userId);
            try {
                likeMapper.insert(insert);
                commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId, commentId)
                        .setSql("likes = likes + 1"));
                return true;
            } catch (DuplicateKeyException e) {
                // 并发下重复插入命中唯一索引，视为本次已点赞，返回相同语义保证幂等
                return false;
            }
        }
        // 3. 已点赞：删除点赞记录并给评论点赞数 -1（下限 0，防止脏数据导致负数）
        likeMapper.deleteById(record.getId());
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .setSql("likes = CASE WHEN likes > 0 THEN likes - 1 ELSE 0 END"));
        return false;
    }

    /**
     * 按用户分页查询"我的评论"（含所属文章标题），按创建时间倒序。
     *
     * @param userId 当前登录用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return "我的评论"分页结果
     */
    @Override
    public PageResult<CommentMyVO> pageMyComments(Long userId, int page, int size) {
        Page<Comment> commentPage = new Page<>(page, size);
        IPage<Comment> result = commentMapper.selectPage(commentPage, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreateTime));
        List<CommentMyVO> list = result.getRecords().stream()
                .map(this::toMyVO)
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 分页查询待审核（PENDING）评论供管理员处理，按创建时间正序（先提审的先审）。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 待审核评论分页结果
     */
    @Override
    public PageResult<CommentVO> pagePending(int page, int size) {
        Page<Comment> commentPage = new Page<>(page, size);
        IPage<Comment> result = commentMapper.selectPage(commentPage, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, STATUS_PENDING)
                .orderByAsc(Comment::getCreateTime));
        List<CommentVO> list = result.getRecords().stream()
                .map(c -> toVO(c, Set.of()))
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 审核评论：仅待审核状态可流转（通过后对外可见，驳回后不再展示），审核结果落库。
     *
     * @param commentId 评论 ID
     * @param dto       审核入参（2=通过 3=驳回）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long commentId, CommentAuditDTO dto) {
        Comment comment = requireComment(commentId);
        // 状态机约束：仅待审核（PENDING）评论可被审核，二次审核或已办结评论直接拒绝
        if (!Objects.equals(STATUS_PENDING, comment.getStatus())) {
            throw new BizException(BizCode.CONTENT_STATUS_INVALID);
        }
        comment.setStatus(dto.getStatus());
        commentMapper.updateById(comment);
    }

    // 校验文章存在：Dubbo 远程调用 article 服务，防止评论挂在已删除/不存在的文章下
    private void requireArticleExists(Long articleId) {
        if (!articleRpcService.existsById(articleId)) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND, "文章不存在");
        }
    }

    // 校验评论存在并返回，不存在时抛业务异常，避免后续对空对象操作
    private Comment requireComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        return comment;
    }

    // 批量解析当前用户已点赞的评论 ID：一次 IN 查询避免逐条判断的 N+1 问题
    private Set<Long> resolveLikedIds(List<Comment> comments, Long currentUserId) {
        if (currentUserId == null || comments.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = comments.stream().map(Comment::getId).collect(Collectors.toList());
        return likeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, currentUserId)
                        .in(CommentLike::getCommentId, ids))
                .stream()
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());
    }

    // 实体转评论区 VO：雪花 ID 转 String 避免前端 JS 精度丢失；一级评论 parentId 归一为 null 便于前端识别
    private CommentVO toVO(Comment comment, Set<Long> likedIds) {
        CommentVO vo = new CommentVO();
        vo.setId(String.valueOf(comment.getId()));
        vo.setArticleId(String.valueOf(comment.getArticleId()));
        vo.setParentId(comment.getParentId() == null || comment.getParentId() == 0L
                ? null : String.valueOf(comment.getParentId()));
        vo.setReplyTo(comment.getReplyToNickname());
        vo.setNickname(comment.getNickname());
        vo.setAvatar(comment.getAvatar());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikes());
        vo.setLiked(likedIds.contains(comment.getId()));
        vo.setCreatedAt(comment.getCreateTime());
        return vo;
    }

    // 实体转"我的评论" VO：保留状态位供前端展示审核结果（1=待审核 2=已审核 3=已驳回）
    private CommentMyVO toMyVO(Comment comment) {
        CommentMyVO vo = new CommentMyVO();
        vo.setId(String.valueOf(comment.getId()));
        vo.setArticleId(String.valueOf(comment.getArticleId()));
        vo.setArticleTitle(comment.getArticleTitle());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikes());
        vo.setStatus(comment.getStatus());
        vo.setCreatedAt(comment.getCreateTime());
        return vo;
    }
}