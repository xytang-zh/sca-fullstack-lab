package com.xytang.article.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xytang.common.core.response.PageResult;
import com.xytang.article.vo.ArticleVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 本地缓存：文章列表（热度/时间排序）缓存 5 分钟，降低 DB 压力。
 */
@Configuration
public class CaffeineConfig {

    /** 缓存过期时间：5 分钟（列表数据时效敏感度低，可接受短暂滞后） */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /** 缓存最大条目数：100，防止内存无限增长 */
    private static final long CACHE_MAX_SIZE = 100L;

    /**
     * 文章列表本地缓存（Key 为排序+分页+作者过滤组合串）。
     *
     * @return Caffeine 缓存实例
     */
    @Bean
    public Cache<String, PageResult<ArticleVO>> articleListCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL)
                .maximumSize(CACHE_MAX_SIZE)
                .build();
    }
}
