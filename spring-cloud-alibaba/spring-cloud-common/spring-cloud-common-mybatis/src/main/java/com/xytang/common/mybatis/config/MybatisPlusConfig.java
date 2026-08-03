package com.xytang.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.xytang.common.mybatis.interceptor.DataPermissionInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 拦截器链配置（对齐 data-model.md §4.2）
 *
 * <p>注册顺序（重要，影响 SQL 改写顺序）：
 * <ol>
 *   <li>{@link DataPermissionInnerInterceptor}（自研，数据权限；data-model.md §4.2 列为 1）
 *   <li>{@link PaginationInnerInterceptor}（分页；DbType.MYSQL）
 *   <li>{@link OptimisticLockerInnerInterceptor}（乐观锁；配合 @Version）
 * </ol>
 *
 * <p>本配置同时注册自研 {@link DataPermissionInnerInterceptor} 与 MyBatis-Plus 自带
 * {@link DataPermissionInterceptor}（备用，未启用 processor）。
 */
@Configuration
public class MybatisPlusConfig {

    private static final long MAX_PAGE_LIMIT = 500L;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 数据权限（自研，基于 @DataScope 注解 + JSqlParser）
        interceptor.addInnerInterceptor(new DataPermissionInnerInterceptor());
        // 2. 分页（MySQL 方言；通过 ShardingSphere 适配层兼容 KingbaseES / DM8）
        PaginationInnerInterceptor page = new PaginationInnerInterceptor(DbType.MYSQL);
        page.setMaxLimit(MAX_PAGE_LIMIT);
        page.setOverflow(false);
        interceptor.addInnerInterceptor(page);
        // 3. 乐观锁（配合 @Version + version 字段；冲突抛 OptimisticLockException）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
