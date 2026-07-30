package com.xytang.starter.sso.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * SSO Client 自动装配入口（对齐 spring-cloud-starters/CLAUDE.md）
 *
 * <p>本 Starter 负责为业务服务提供 Sa-Token SSO Client 能力：
 * <ul>
 *   <li>自动装配 SsoClientConfig（SSO 模式 3 Ticket 校验）</li>
 *   <li>注册 SsoClientInterceptor（Client 端登录态校验）</li>
 * </ul>
 *
 * <p>注意：MVP 阶段使用 SSO 模式 2（前后端分离 + 跨域 Ticket），模式 3 在 Phase 3+ 启用。
 */
@Configuration
@AutoConfiguration
@ComponentScan(basePackages = "com.xytang.starter.sso.client")
public class SsoClientAutoConfiguration {

}
