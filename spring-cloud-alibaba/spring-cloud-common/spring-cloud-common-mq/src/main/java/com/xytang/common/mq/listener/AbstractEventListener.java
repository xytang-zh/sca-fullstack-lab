package com.xytang.common.mq.listener;

import com.xytang.common.core.constant.CacheKeyConstants;
import com.xytang.common.core.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 事件监听器抽象基类（对齐 spring-cloud-alibaba/CLAUDE.md §9.2 异步事件规范）
 *
 * <p>所有 RabbitMQ Listener 必须继承本类以实现幂等消费：
 * <ul>
 *   <li>消费前检查 {@code mq:consumed:{eventId}} 是否已处理</li>
 *   <li>处理成功后写入 Redis，TTL 24h（防重入）</li>
 *   <li>处理失败抛出异常，由 RabbitMQ 重试机制处理</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEventListener<T extends BaseEvent> {

    private static final Duration CONSUMED_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    protected abstract void handle(T event);

    public void onMessage(T event) {
        if (event == null || event.getEventId() == null) {
            log.warn("[MQ] event or eventId is null, ignore");
            return;
        }
        String key = "mq:consumed:" + event.getEventId();
        Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(key, "1", CONSUMED_TTL);
        if (Boolean.FALSE.equals(firstTime)) {
            log.info("[MQ] duplicated event, eventId={} type={}, skip", event.getEventId(), event.getEventType());
            return;
        }
        try {
            handle(event);
            log.info("[MQ] consumed, eventId={} type={}", event.getEventId(), event.getEventType());
        } catch (RuntimeException e) {
            redisTemplate.delete(key);
            log.error("[MQ] consume failed, eventId={} type={}", event.getEventId(), event.getEventType(), e);
            throw e;
        }
    }

    @SuppressWarnings("unused")
    private void unused() {
        // 防止编译期常量折叠移除引用
        String ignored = CacheKeyConstants.PREFIX;
    }
}
