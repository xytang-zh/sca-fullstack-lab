package com.xytang.common.mybatis.rbac;

import com.xytang.common.core.constant.CommonConstants;

/**
 * RBAC 数据权限上下文（对齐 data-model.md §2.2 + §4.2 + §6）
 *
 * <p>由各业务服务在 Service 层显式设置当前用户 ID / 角色 / 部门 ID，
 * 供 {@link com.xytang.common.mybatis.interceptor.DataPermissionInnerInterceptor}
 * 在 SQL 改写时读取。
 *
 * <p>MVP 实现骨架：ThreadLocal 持有上下文，T018 完善后由 Sa-Token 监听器自动填充。
 */
public final class RbacContext {

    private static final ThreadLocal<RbacUser> HOLDER = new ThreadLocal<>();

    private RbacContext() {
    }

    /**
     * 写入当前线程的 RBAC 快照（请求开始/登录态变更时调用）。
     *
     * @param user 当前登录用户的 RBAC 快照
     */
    public static void set(RbacUser user) {
        HOLDER.set(user);
    }

    /**
     * 读取当前线程的 RBAC 快照；未设置时返回匿名用户（避免 NPE）。
     *
     * @return 当前登录用户的 RBAC 快照
     */
    public static RbacUser get() {
        RbacUser u = HOLDER.get();
        return u == null ? RbacUser.anonymous() : u;
    }

    /**
     * 判断当前用户是否为超级管理员（角色编码含 SUPER_ADMIN）。
     *
     * @return true 表示超级管理员
     */
    public static boolean isSuperAdmin() {
        RbacUser u = get();
        return u.roles().contains(CommonConstants.SUPER_ADMIN_ROLE_CODE);
    }

    /**
     * 清理当前线程上下文（请求结束必须调用，防止线程池复用导致越权）。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前登录用户的 RBAC 快照。
     *
     * @param userId 用户 ID（未登录为 null）
     * @param deptId 部门 ID（未登录为 null）
     * @param roles  角色编码集合
     */
    public record RbacUser(Long userId, Long deptId, java.util.Set<String> roles) {

        public static RbacUser anonymous() {
            return new RbacUser(null, null, java.util.Set.of());
        }

        public boolean isAuthenticated() {
            return userId != null;
        }
    }
}
