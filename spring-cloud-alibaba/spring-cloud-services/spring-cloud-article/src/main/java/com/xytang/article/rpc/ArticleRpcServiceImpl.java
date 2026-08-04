package com.xytang.article.rpc;

import com.xytang.article.entity.Article;
import com.xytang.article.mapper.ArticleMapper;
import com.xytang.common.dubbo.ArticleRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 文章 RPC 提供者：对 comment 等外部服务暴露文章存在性校验。
 * selectById 经 MyBatis-Plus 逻辑删除（deleted=0）自动过滤已删除记录。
 */
@DubboService
@RequiredArgsConstructor
public class ArticleRpcServiceImpl implements ArticleRpcService {

    private final ArticleMapper articleMapper;

    /**
     * 校验文章是否存在（供 comment 服务发布评论前校验目标文章）。
     *
     * @param articleId 文章 ID
     * @return true=存在 false=不存在或 ID 为空
     */
    @Override
    public boolean existsById(Long articleId) {
        if (articleId == null) {
            return false;
        }
        Article article = articleMapper.selectById(articleId);
        return article != null;
    }
}