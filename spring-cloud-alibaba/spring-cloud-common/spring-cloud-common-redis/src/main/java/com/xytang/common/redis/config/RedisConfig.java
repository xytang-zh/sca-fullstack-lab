package com.xytang.common.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置（对齐 spring-cloud-alibaba/CLAUDE.md §11.4 缓存规范）
 *
 * <p>统一使用 String key + JSON value 序列化，避免 JDK 序列化的可读性差与跨语言问题。
 */
@Configuration
public class RedisConfig {

    /**
     * 注册通用 RedisTemplate：key 用 String 序列化，value 用带类型信息的 JSON 序列化。
     *
     * @param factory Redis 连接工厂（Spring Boot 自动装配）
     * @return 通用操作模板
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = jacksonSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 注册 StringRedisTemplate：key/value 均为字符串，适合计数器、简单 KV。
     *
     * @param factory Redis 连接工厂（Spring Boot 自动装配）
     * @return 字符串操作模板
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    // 构建 JSON 序列化器：注册 Java 8 时间模块 + 启用默认类型信息，保证 LocalDateTime 与多态可反序列化
    private Jackson2JsonRedisSerializer<Object> jacksonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        return new Jackson2JsonRedisSerializer<>(mapper, Object.class);
    }
}
