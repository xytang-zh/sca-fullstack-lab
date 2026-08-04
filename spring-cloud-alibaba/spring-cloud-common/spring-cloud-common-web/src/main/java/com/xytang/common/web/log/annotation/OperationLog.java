package com.xytang.common.web.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（自 spring-cloud-common-log 迁入，简化后并入 common-web）。
 *
 * <p>用法：在 Controller 方法上标注，由 OperationLogAspect 切面自动记录接口调用。
 *
 * <p>日志落地：迁入后去掉 MQ 依赖，改为本地日志（@Slf4j）输出，不再经 RabbitMQ 发送。
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