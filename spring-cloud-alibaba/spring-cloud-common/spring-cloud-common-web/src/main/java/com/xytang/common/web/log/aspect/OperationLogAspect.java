package com.xytang.common.web.log.aspect;

import com.xytang.common.core.constant.HeaderConstants;
import com.xytang.common.web.log.annotation.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面（自 spring-cloud-common-log 迁入，简化后并入 common-web）。
 *
 * <p>拦截 {@link OperationLog} 注解的方法，捕获：
 * <ul>
 *   <li>请求 URL / 方法 / 关键入参（敏感字段截断）</li>
 *   <li>响应结果摘要</li>
 *   <li>耗时（毫秒）</li>
 *   <li>操作人 ID（从 X-Login-Id Header 获取）</li>
 *   <li>客户端 IP</li>
 * </ul>
 *
 * <p>简化后去掉 RabbitMQ 异步发送（原 log 服务已删除），改为本地日志输出，不阻塞业务请求。
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAspect {

    private static final int RESPONSE_MAX_LEN = 2000;
    private static final int ARG_MAX_LEN = 500;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("module", operationLog.module());
        logEntry.put("operation", operationLog.operation());
        logEntry.put("method", buildMethodSignature(pjp));
        logEntry.put("createTime", LocalDateTime.now().toString());

        HttpServletRequest req = currentRequest();
        if (req != null) {
            logEntry.put("requestUrl", req.getRequestURI());
            logEntry.put("requestMethod", req.getMethod());
            logEntry.put("ip", resolveIp(req));
            logEntry.put("userId", req.getHeader(HeaderConstants.X_LOGIN_ID));
        }

        if (operationLog.saveRequest()) {
            logEntry.put("requestParams", sanitizeArgs(pjp.getArgs()));
        }

        Throwable error = null;
        Object result = null;
        try {
            result = pjp.proceed();
            logEntry.put("status", 1);
            if (operationLog.saveResponse() && result != null) {
                logEntry.put("responseResult", truncate(String.valueOf(result), RESPONSE_MAX_LEN));
            }
            return result;
        } catch (RuntimeException e) {
            error = e;
            logEntry.put("status", 0);
            logEntry.put("errorMsg", truncate(e.getMessage(), RESPONSE_MAX_LEN));
            throw e;
        } finally {
            if (operationLog.saveCost()) {
                logEntry.put("costMs", System.currentTimeMillis() - start);
            }
            log.info("[OperationLog] {}", logEntry);
        }
    }

    private String buildMethodSignature(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String resolveIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = req.getHeader("X-Real-IP");
        return ip != null && !ip.isBlank() ? ip : req.getRemoteAddr();
    }

    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            String s = String.valueOf(args[i]);
            sb.append(truncate(s, ARG_MAX_LEN));
        }
        return sb.append("]").toString();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "...(truncated)" : s;
    }
}