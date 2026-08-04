package com.xytang.common.dubbo;

/**
 * 评论服务 RPC 契约：供 article 等服务聚合评论数。
 */
public interface CommentRpcService {

    /**
     * 统计某篇文章的评论数。
     *
     * @param articleId 文章 ID
     * @return 评论数
     */
    long countByArticleId(Long articleId);
}