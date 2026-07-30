package com.xytang.log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SpringCloudLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudLogApplication.class, args);
    }
}
