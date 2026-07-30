package com.xytang.common.core.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    public static final String SUPER_ADMIN_ROLE_CODE = "super_admin";

    public static final int STATUS_DRAFT = 1;
    public static final int STATUS_NORMAL = 2;
    public static final int STATUS_DISABLED = 3;
    public static final int STATUS_LOCKED = 4;
    public static final int STATUS_DELETED = 5;

    public static final int DATA_SCOPE_ALL = 1;
    public static final int DATA_SCOPE_DEPT_AND_BELOW = 2;
    public static final int DATA_SCOPE_DEPT_ONLY = 3;
    public static final int DATA_SCOPE_SELF = 4;
    public static final int DATA_SCOPE_CUSTOM = 5;

    public static final int DEL_FLAG_NOT_DELETED = 0;
    public static final int DEL_FLAG_DELETED = 1;
}
