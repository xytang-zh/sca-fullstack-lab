package com.xytang.common.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 事件总线配置（对齐 spring-cloud-alibaba/CLAUDE.md §9.2 异步事件）
 *
 * <p>统一使用 JSON 序列化，避免 Java 序列化的可读性差与跨语言问题。
 *
 * <p>Exchange 命名规范（fanout 为主，便于多消费者广播）：
 * <ul>
 *   <li>user.login / user.kickout / user.logout</li>
 *   <li>task.todo / alert.trigger</li>
 *   <li>doc.uploaded / article.published</li>
 *   <li>log.operation / file.uploaded</li>
 *   <li>oauth2.authorize</li>
 *   <li>portal.content.published / portal.content.unpublished</li>
 *   <li>sys.param.changed</li>
 * </ul>
 */
@Configuration
public class EventConfig {

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }
}
