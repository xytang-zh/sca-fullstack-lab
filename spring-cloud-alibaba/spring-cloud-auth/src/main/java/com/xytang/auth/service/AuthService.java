package com.xytang.auth.service;

import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.PasswordUpdateDTO;
import com.xytang.auth.dto.SmsLoginDTO;
import com.xytang.auth.vo.LoginVO;
import com.xytang.auth.vo.UserInfoVO;

/**
 * 认证服务：登录 / 登出 / 当前用户 / 修改密码 / 踢人下线
 */
public interface AuthService {

    LoginVO login(LoginDTO dto, String ip, String userAgent);

    /**
     * 手机验证码登录/注册一体：新用户自动注册（默认 USER 角色）并登录，老用户直接登录。
     *
     * @param dto       手机号与验证码
     * @param ip        客户端 IP
     * @param userAgent 客户端 UA
     * @return 登录返回 VO
     */
    LoginVO smsLogin(SmsLoginDTO dto, String ip, String userAgent);

    void logout();

    UserInfoVO currentUser();

    void updatePassword(PasswordUpdateDTO dto);

    /**
     * 预埋踢人下线接口（具体 SSO Pub/Sub 通知在 US2 完成）
     *
     * @param userId 被踢用户 ID
     */
    void kickout(Long userId);
}
