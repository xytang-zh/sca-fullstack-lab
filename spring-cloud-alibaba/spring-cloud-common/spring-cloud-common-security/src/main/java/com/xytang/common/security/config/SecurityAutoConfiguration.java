package com.xytang.common.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 网关鉴权公共配置入口（对齐 spring-cloud-gateway/CLAUDE.md §6 鉴权规范）
 *
 * <p>本配置扫描 common-security 包下的 WebFilter / GatewayFilterFactory 实现。
 *
 * <p>注意：本模块仅用于响应式（WebFlux）环境，禁止依赖 spring-boot-starter-web。
 */
@Configuration
@AutoConfiguration
@ComponentScan(basePackages = "com.xytang.common.security")
public class SecurityAutoConfiguration {

}
