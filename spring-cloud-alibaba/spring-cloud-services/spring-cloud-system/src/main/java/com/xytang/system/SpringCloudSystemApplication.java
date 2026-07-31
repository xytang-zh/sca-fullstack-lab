package com.xytang.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.xytang.system", "com.xytang.common"})
@EnableDiscoveryClient
@MapperScan("com.xytang.system.mapper")
public class SpringCloudSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudSystemApplication.class, args);
    }
}
