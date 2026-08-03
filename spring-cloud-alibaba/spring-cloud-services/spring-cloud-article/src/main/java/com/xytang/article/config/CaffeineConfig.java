package com.xytang.article.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xytang.common.core.response.PageVO;
import com.xytang.article.vo.ArticleVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 本地缓存：文章列表（热度/时间排序）缓存 5 分钟，降低 DB 压力。
 */
@Configuration
public class CaffeineConfig {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final long CACHE_MAX_SIZE = 100L;

    @Bean
    public Cache<String, PageVO<ArticleVO>> articleListCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL)
                .maximumSize(CACHE_MAX_SIZE)
                .build();
    }
}
