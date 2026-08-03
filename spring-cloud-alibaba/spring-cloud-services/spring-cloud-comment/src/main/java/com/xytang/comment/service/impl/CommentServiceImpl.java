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
import com.xytang.common.core.response.PageVO;
import lombok.RequiredArgsConstructor;
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

    @Override
    public PageVO<CommentVO> pageByArticle(Long articleId, int pageNum, int pageSize, Long currentUserId) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        IPage<Comment> result = commentMapper.selectPage(page, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, STATUS_APPROVED)
                .orderByAsc(Comment::getCreateTime));
        Set<Long> likedIds = resolveLikedIds(result.getRecords(), currentUserId);
        List<CommentVO> list = result.getRecords().stream()
                .map(c -> toVO(c, likedIds))
                .collect(Collectors.toList());
        return PageVO.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public CommentVO create(CommentCreateDTO dto, Long userId, String ip, String ua) {
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setArticleTitle(dto.getArticleTitle());
        comment.setUserId(userId);
        comment.setNickname(dto.getNickname());
        comment.setAvatar(dto.getAvatar());
        comment.setParentId(0L);
        comment.setReplyToId(0L);
        comment.setReplyToNickname("");
        comment.setContent(dto.getContent());
        comment.setStatus(STATUS_PENDING);
        comment.setIp(ip);
        comment.setUserAgent(ua);
        comment.setLikes(0L);
        commentMapper.insert(comment);
        return toVO(comment, Set.of());
    }

    @Override
    public CommentVO reply(Long commentId, CommentReplyDTO dto, Long userId, String ip, String ua) {
        Comment parent = requireComment(commentId);
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setArticleTitle(dto.getArticleTitle());
        comment.setUserId(userId);
        comment.setNickname(dto.getNickname());
        comment.setAvatar(dto.getAvatar());
        comment.setParentId(parent.getId());
        comment.setReplyToId(commentId);
        comment.setReplyToNickname(dto.getReplyTo());
        comment.setContent(dto.getContent());
        comment.setStatus(STATUS_PENDING);
        comment.setIp(ip);
        comment.setUserAgent(ua);
        comment.setLikes(0L);
        commentMapper.insert(comment);
        return toVO(comment, Set.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long commentId, Long userId) {
        requireComment(commentId);
        CommentLike record = likeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId)
                .last("LIMIT 1"));
        if (record == null) {
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
                return false;
            }
        }
        likeMapper.deleteById(record.getId());
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .setSql("likes = CASE WHEN likes > 0 THEN likes - 1 ELSE 0 END"));
        return false;
    }

    @Override
    public PageVO<CommentMyVO> pageMyComments(Long userId, int pageNum, int pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        IPage<Comment> result = commentMapper.selectPage(page, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreateTime));
        List<CommentMyVO> list = result.getRecords().stream()
                .map(this::toMyVO)
                .collect(Collectors.toList());
        return PageVO.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public PageVO<CommentVO> pagePending(int pageNum, int pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        IPage<Comment> result = commentMapper.selectPage(page, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, STATUS_PENDING)
                .orderByAsc(Comment::getCreateTime));
        List<CommentVO> list = result.getRecords().stream()
                .map(c -> toVO(c, Set.of()))
                .collect(Collectors.toList());
        return PageVO.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long commentId, CommentAuditDTO dto) {
        Comment comment = requireComment(commentId);
        if (!Objects.equals(STATUS_PENDING, comment.getStatus())) {
            throw new BizException(BizCode.CONTENT_STATUS_INVALID);
        }
        comment.setStatus(dto.getStatus());
        commentMapper.updateById(comment);
    }

    private Comment requireComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        return comment;
    }

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