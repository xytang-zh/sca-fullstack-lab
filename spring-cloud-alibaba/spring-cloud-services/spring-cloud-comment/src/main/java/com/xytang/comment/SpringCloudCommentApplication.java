package com.xytang.comment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 博客评论服务启动类：评论列表（游客可读）/发表/回复/点赞/我的评论/审核。
 */
@SpringBootApplication(scanBasePackages = {"com.xytang.comment", "com.xytang.common"})
@EnableDiscoveryClient
@MapperScan("com.xytang.comment.mapper")
public class SpringCloudCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudCommentApplication.class, args);
    }
}