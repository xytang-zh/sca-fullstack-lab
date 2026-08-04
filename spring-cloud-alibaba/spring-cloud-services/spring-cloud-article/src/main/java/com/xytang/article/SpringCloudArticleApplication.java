package com.xytang.article;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 博客内容服务启动类：文章列表（游客可读）/详情/发布/点赞/收藏/热度排序。
 */
@SpringBootApplication(scanBasePackages = {"com.xytang.article", "com.xytang.common"})
@EnableDiscoveryClient
@EnableDubbo
@MapperScan("com.xytang.article.mapper")
public class SpringCloudArticleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudArticleApplication.class, args);
    }
}
