package com.xytang.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 系统管理服务启动类。
 */
// 显式扫描 com.xytang.common：common 子模块尚未启用 Spring Boot 自动装配（缺 AutoConfiguration.imports），
// 需通过扫描包让 common 中的配置类（异常处理/MyBatis 拦截器等）生效
@SpringBootApplication(scanBasePackages = {"com.xytang.system", "com.xytang.common"})
@EnableDiscoveryClient
@MapperScan("com.xytang.system.mapper")
public class SpringCloudSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudSystemApplication.class, args);
    }
}
