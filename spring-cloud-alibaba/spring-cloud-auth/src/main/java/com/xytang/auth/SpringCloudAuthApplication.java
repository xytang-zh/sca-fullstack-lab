package com.xytang.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证中心启动类。
 */
@SpringBootApplication(scanBasePackages = {"com.xytang.auth", "com.xytang.common"})
@EnableDiscoveryClient
@MapperScan("com.xytang.auth.mapper")
public class SpringCloudAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudAuthApplication.class, args);
    }
}
