package com.xytang.common.core.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 事件基类（对齐 spring-cloud-alibaba/CLAUDE.md §9.2 异步事件规范）
 *
 * <p>所有 RabbitMQ 事件必须继承本类，包含 eventId（幂等键）、eventType、timestamp。
 * <p>所有 Listener 必须继承 {@code AbstractEventListener<T>}（在 common-mq 中）以实现幂等消费。
 */
@Data
public abstract class BaseEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventId = UUID.randomUUID().toString();
    private String eventType = getClass().getSimpleName();
    private LocalDateTime timestamp = LocalDateTime.now();
}
