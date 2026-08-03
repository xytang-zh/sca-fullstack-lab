package com.xytang.article.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 热度排序权重配置（Nacos spring-cloud-article.yaml 可动态调整）。
 *
 * <p>score = views * viewsWeight + likes * likesWeight
 *        + favorites * favoritesWeight + comments * commentsWeight
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "blog.hot")
public class HotWeightProperties {

    /** 阅读量权重 */
    private double views = 1.0;

    /** 点赞数权重 */
    private double likes = 3.0;

    /** 收藏数权重 */
    private double favorites = 4.0;

    /** 评论数权重 */
    private double comments = 2.0;
}
