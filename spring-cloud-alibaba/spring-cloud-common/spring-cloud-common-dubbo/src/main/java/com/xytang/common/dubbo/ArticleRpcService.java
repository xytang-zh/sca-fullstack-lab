package com.xytang.common.dubbo;

/**
 * 文章服务 RPC 契约：供 comment 等服务校验文章是否存在。
 */
@FunctionalInterface
public interface ArticleRpcService {

    /**
     * 校验文章是否存在（未软删除）。
     *
     * @param articleId 文章 ID
     * @return true=存在，false=不存在
     */
    boolean existsById(Long articleId);
}