package com.xytang.comment;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 博客评论服务启动类：评论列表（游客可读）/发表/回复/点赞/我的评论/审核。
 */
@SpringBootApplication(scanBasePackages = {"com.xytang.comment", "com.xytang.common"})
@EnableDiscoveryClient
@EnableDubbo
@MapperScan("com.xytang.comment.mapper")
public class SpringCloudCommentApplication {

    /**
     * 启动博客评论服务（HTTP 8094 / Dubbo 20894）。
     *
     * @param args 命令行启动参数（如 --server.port 覆盖端口）
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringCloudCommentApplication.class, args);
    }
}