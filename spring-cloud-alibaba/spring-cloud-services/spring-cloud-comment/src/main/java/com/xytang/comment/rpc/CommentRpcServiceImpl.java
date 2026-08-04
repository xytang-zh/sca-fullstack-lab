package com.xytang.comment.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xytang.comment.entity.Comment;
import com.xytang.comment.mapper.CommentMapper;
import com.xytang.common.dubbo.CommentRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 评论 RPC 提供者：对 article 等外部服务暴露评论数统计。
 * 仅统计已审核（APPROVED）评论，与对外展示口径一致。
 */
@DubboService
@RequiredArgsConstructor
public class CommentRpcServiceImpl implements CommentRpcService {

    /** 评论状态：已审核（对外可见） */
    private static final int STATUS_APPROVED = 2;

    private final CommentMapper commentMapper;

    @Override
    public long countByArticleId(Long articleId) {
        if (articleId == null) {
            return 0L;
        }
        Long count = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, STATUS_APPROVED));
        return count == null ? 0L : count;
    }
}