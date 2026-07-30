package com.xytang.common.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置（Argon2id，OWASP 2024 推荐）。
 *
 * <p>参数从 Nacos {@code spring-cloud-shared.yaml} 的
 * {@code security.password.argon2.*} 路径读取，{@code @RefreshScope}
 * 让参数变更即时生效（无需重启）。
 *
 * <p>开发期默认值：memory=16384KB(16MB) / iterations=3 / parallelism=2
 * / key-length=32 / salt-length=16；生产环境建议 memory=65536(64MB) +
 * parallelism=4（详见 spec.md FR-013）。
 *
 * <p>注：parallelism 下限为 2，OWASP Cheat Sheet 原推荐为 1，但 Bouncy Castle
 * 1.78+ 要求 lanes &gt; 1（lanes == parallelism），低于 2 会抛
 * {@code IllegalStateException: lanes must be greater than 1}。
 *
 * <p>T039：在 {@link #passwordEncoder()} Bean 创建时打印参数，
 * 便于验证 {@code @RefreshScope} 在 Nacos 配置变更后重建 Bean 时新参数生效。
 */
@Configuration
@RefreshScope
@Slf4j
public class PasswordEncoderConfig {

    @Value("${security.password.argon2.memory:16384}")
    private int memory;

    @Value("${security.password.argon2.iterations:3}")
    private int iterations;

    @Value("${security.password.argon2.parallelism:2}")
    private int parallelism;

    @Value("${security.password.argon2.key-length:32}")
    private int keyLength;

    @Value("${security.password.argon2.salt-length:16}")
    private int saltLength;

    @Bean
    @RefreshScope
    public PasswordEncoder passwordEncoder() {
        log.info("[PasswordEncoder] creating Argon2id: memory={}KB iterations={} parallelism={} keyLen={}B saltLen={}B",
            memory, iterations, parallelism, keyLength, saltLength);
        return new Argon2PasswordEncoder(
            saltLength, keyLength,
            parallelism, memory, iterations
        );
    }
}
