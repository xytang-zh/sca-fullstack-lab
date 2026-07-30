package com.xytang.auth.service.impl;

import com.xytang.auth.constant.AuthConstants;
import com.xytang.auth.service.LoginRiskService;
import com.xytang.common.core.exception.AccountLockedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginRiskServiceImpl implements LoginRiskService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void assertNotLocked(String username) {
        String lockKey = AuthConstants.LOGIN_LOCK_PREFIX + username;
        String locked = stringRedisTemplate.opsForValue().get(lockKey);
        if (locked != null) {
            Long ttl = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            log.warn("[LoginRisk] account locked: username={} ttlSec={}", username, ttl);
            throw new AccountLockedException("账号已锁定，请 " + (ttl == null ? 0 : (ttl + 60) / 60) + " 分钟后重试");
        }
    }

    @Override
    public void recordFailure(String username) {
        String failKey = AuthConstants.LOGIN_FAIL_PREFIX + username;
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofMinutes(AuthConstants.LOGIN_LOCK_MINUTES));
        }
        if (count != null && count >= AuthConstants.LOGIN_MAX_FAIL_COUNT) {
            String lockKey = AuthConstants.LOGIN_LOCK_PREFIX + username;
            stringRedisTemplate.opsForValue().set(
                lockKey, "1",
                Duration.ofMinutes(AuthConstants.LOGIN_LOCK_MINUTES));
            log.warn("[LoginRisk] account locked by failures: username={} count={}", username, count);
        }
    }

    @Override
    public void clearFailure(String username) {
        stringRedisTemplate.delete(AuthConstants.LOGIN_FAIL_PREFIX + username);
        stringRedisTemplate.delete(AuthConstants.LOGIN_LOCK_PREFIX + username);
    }
}
