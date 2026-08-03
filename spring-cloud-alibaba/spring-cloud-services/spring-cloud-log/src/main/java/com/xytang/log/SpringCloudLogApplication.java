package com.xytang.log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 日志服务启动类。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class SpringCloudLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudLogApplication.class, args);
    }
}
