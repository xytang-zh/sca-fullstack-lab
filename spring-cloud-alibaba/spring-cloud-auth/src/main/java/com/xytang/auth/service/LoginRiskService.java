package com.xytang.auth.service;

/**
 * 登录风控：失败计数 + 锁定 15 分钟
 */
public interface LoginRiskService {

    /**
     * 检查账号是否锁定，锁定则抛 AccountLockedException
     */
    void assertNotLocked(String username);

    /**
     * 登录失败 +1，达到阈值后写入锁定标记
     */
    void recordFailure(String username);

    /**
     * 登录成功后清除失败计数
     */
    void clearFailure(String username);
}
