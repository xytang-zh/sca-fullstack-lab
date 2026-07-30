package com.xytang.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SpringCloudPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudPortalApplication.class, args);
    }
}
