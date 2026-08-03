package com.xytang.common.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（对齐 spring-cloud-services/CLAUDE.md §4.8.2 + common-patterns.md §6.3）
 *
 * <p>用法：在 Controller 方法上标注，由 OperationLogAspect 切面自动记录接口调用。
 *
 * <p>日志落地：通过 RabbitMQ 异步发送到 spring-cloud-log 服务，按月分表存储。
 *
 * <p>字段脱敏（data-model.md §3.3）：
 * <ul>
 *   <li>密码、Token、身份证号禁止记录到 request_params</li>
 *   <li>手机号中间 4 位 * 替换</li>
 * </ul>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 模块（如 system/user）
     *
     * @return 模块名
     */
    String module();

    /**
     * 操作类型（如 INSERT/UPDATE/DELETE/KICKOUT/LOGIN）
     *
     * @return 操作类型
     */
    String operation();

    /**
     * 是否记录请求参数（默认 true；含敏感字段的接口设为 false）
     *
     * @return 是否记录请求参数
     */
    boolean saveRequest() default true;

    /**
     * 是否记录响应结果（默认 false；调试时可设为 true）
     *
     * @return 是否记录响应结果
     */
    boolean saveResponse() default false;

    /**
     * 是否记录耗时（默认 true）
     *
     * @return 是否记录耗时
     */
    boolean saveCost() default true;
}
