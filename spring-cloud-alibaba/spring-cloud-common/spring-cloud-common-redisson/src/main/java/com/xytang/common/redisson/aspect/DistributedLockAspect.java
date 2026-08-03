package com.xytang.common.redisson.aspect;

import com.xytang.common.core.constant.CacheKeyConstants;
import com.xytang.common.core.response.R;
import com.xytang.common.redisson.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面（对齐 common-redisson CLAUDE.md / 宪法 §11.3 并发规范）
 *
 * <p>基于 Redisson RLock 实现，支持：
 * <ul>
 *   <li>SpEL 解析 Key（{@code #user.id}）</li>
 *   <li>等待 + 持有超时</li>
 *   <li>失败抛出 {@link com.xytang.common.core.exception.BusinessException}</li>
 * </ul>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final long MILLIS_PER_SECOND = 1000L;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer PARAM_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = buildLockKey(pjp, distributedLock);
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired;
        try {
            long waitSecs = distributedLock.waitTimeUnit().toMillis(distributedLock.waitTime())
                    / MILLIS_PER_SECOND;
            long leaseSecs = distributedLock.leaseTimeUnit().toMillis(distributedLock.leaseTime())
                    / MILLIS_PER_SECOND;
            if (leaseSecs < 0) {
                acquired = lock.tryLock((long) waitSecs, TimeUnit.MINUTES);
            } else {
                acquired = lock.tryLock(waitSecs, leaseSecs, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new com.xytang.common.core.exception.BusinessException(
                distributedLock.failCode(), "获取分布式锁被中断：" + lockKey);
        }

        if (!acquired) {
            log.warn("[DistributedLock] acquire failed, key={}", lockKey);
            String failMsg = distributedLock.failMsg();
            if (failMsg == null || failMsg.isBlank()) {
                throw new com.xytang.common.core.exception.BusinessException(
                    distributedLock.failCode());
            }
            throw new com.xytang.common.core.exception.BusinessException(
                distributedLock.failCode(), failMsg);
        }

        try {
            log.debug("[DistributedLock] acquired, key={}", lockKey);
            return pjp.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[DistributedLock] released, key={}", lockKey);
            }
        }
    }

    private String buildLockKey(ProceedingJoinPoint pjp, DistributedLock ann) {
        String type = ann.resourceType().isBlank() ? defaultType(pjp) : ann.resourceType();
        String idPart;

        if (ann.key().isBlank()) {
            idPart = String.valueOf(System.identityHashCode(pjp.getArgs()));
        } else if (ann.key().startsWith("#")) {
            idPart = parseSpel(ann.key(), pjp);
        } else {
            idPart = ann.key();
        }
        return CacheKeyConstants.LOCK + type + ":" + idPart;
    }

    private String defaultType(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getMethod().getDeclaringClass().getSimpleName() + "#" + sig.getMethod().getName();
    }

    private String parseSpel(String spel, ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();
        String[] paramNames = PARAM_NAME_DISCOVERER.getParameterNames(method);
        EvaluationContext ctx = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }
        Expression exp = PARSER.parseExpression(spel);
        Object value = exp.getValue(ctx);
        return value == null ? "null" : value.toString();
    }

    @SuppressWarnings("unused")
    private static void unusedReference() {
        // 引用 R 以避免 common-core 在仅编译期被遗漏
        R.ok();
    }
}
