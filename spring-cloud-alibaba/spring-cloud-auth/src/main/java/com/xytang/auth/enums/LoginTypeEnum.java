package com.xytang.auth.enums;

/**
 * 登录类型（对齐 data-model.md §2.10 sys_login_log.login_type）
 *
 * <ul>
 *   <li>1 登录</li>
 *   <li>2 登出</li>
 *   <li>3 踢人下线</li>
 *   <li>4 SSO 单点登录</li>
 *   <li>5 OAuth2 授权</li>
 *   <li>6 短信验证码登录</li>
 * </ul>
 */
public enum LoginTypeEnum {

    /** 普通登录（账号密码） */
    LOGIN(1, "login"),
    /** 登出 */
    LOGOUT(2, "logout"),
    /** 踢人下线 */
    KICKOUT(3, "kickout"),
    /** SSO 单点登录 */
    SSO(4, "sso"),
    /** OAuth2 授权登录 */
    OAUTH2(5, "oauth2"),
    /** 短信验证码登录 */
    SMS(6, "sms");

    private final int code;
    private final String msg;

    LoginTypeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int code() {
        return code;
    }

    public String msg() {
        return msg;
    }
}
