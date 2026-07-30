package com.xytang.common.satoken.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 公共配置入口（对齐 spring-cloud-alibaba/CLAUDE.md §11.6 安全规范）
 *
 * <p>本配置类扫描 {@code com.xytang.common.satoken} 包下的所有 StpInterfaceImpl / Sa-Token
 * 拦截器配置，作为 common-satoken 模块的自动装配入口。
 *
 * <p>具体业务实现（StpInterfaceImpl）由各业务服务在自身工程内提供。
 *
 * <p>注意：本配置类不直接依赖业务模块；为避免循环依赖，所有 Sa-Token 业务实现必须放
 * 在业务服务内部，由 StpUtil 调用。
 */
@Configuration
@AutoConfiguration
@ComponentScan(basePackages = "com.xytang.common.satoken")
public class SaTokenAutoConfiguration {

}
