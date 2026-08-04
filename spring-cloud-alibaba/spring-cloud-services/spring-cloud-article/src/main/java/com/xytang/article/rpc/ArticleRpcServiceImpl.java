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

    @Override
    public boolean existsById(Long articleId) {
        if (articleId == null) {
            return false;
        }
        Article article = articleMapper.selectById(articleId);
        return article != null;
    }
}