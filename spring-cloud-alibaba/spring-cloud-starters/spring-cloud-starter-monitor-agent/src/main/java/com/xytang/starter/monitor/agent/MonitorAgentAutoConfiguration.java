package com.xytang.starter.monitor.agent;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Monitor Agent 自动装配入口
 *
 * <p>MVP 后启用：为 spring-cloud-monitor 服务提供 OSHI 采集 + 上报能力。
 */
@Configuration
@AutoConfiguration
@ComponentScan(basePackages = "com.xytang.starter.monitor.agent")
public class MonitorAgentAutoConfiguration {

}
