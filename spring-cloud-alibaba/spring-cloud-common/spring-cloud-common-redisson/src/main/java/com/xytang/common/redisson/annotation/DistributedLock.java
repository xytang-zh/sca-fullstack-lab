package com.xytang.common.redisson.annotation;

import com.xytang.common.core.response.BizCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解（对齐 spring-cloud-alibaba/CLAUDE.md §11.3 并发规范 + data-model.md §6 锁 Key 规范）。
 *
 * <p>用法：在 Service 方法上标注，AOP 切面包裹 Redisson RLock。
 *
 * <p>锁 Key 格式：{@code lock:{resourceType}:{resourceId}}
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** 锁 Key（支持 SpEL：{@code #user.id}）；为空时使用 类名 + 方法名 + 参数 */
    String key() default "";

    /** 资源类型前缀（用于 {@code lock:{type}:{id}} 格式）；为空时使用方法名 */
    String resourceType() default "";

    /** 等待时间（单位：{@link #waitTimeUnit()}），默认 3 秒 */
    long waitTime() default 3L;

    TimeUnit waitTimeUnit() default TimeUnit.SECONDS;

    /** 持有时间（单位：{@link #leaseTimeUnit()}），默认 30 秒；-1 表示启用 watchdog 自动续期 */
    long leaseTime() default 30L;

    TimeUnit leaseTimeUnit() default TimeUnit.SECONDS;

    /** 失败时抛出的业务异常码；默认 RATE_LIMIT（429 / "00201"） */
    BizCode failCode() default BizCode.RATE_LIMIT;

    /** 失败提示消息（覆盖 failCode 的 message） */
    String failMsg() default "";
}
