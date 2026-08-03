package com.xytang.auth.service;

import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.PasswordUpdateDTO;
import com.xytang.auth.dto.RegisterDTO;
import com.xytang.auth.vo.LoginVO;
import com.xytang.auth.vo.UserInfoVO;

/**
 * 认证服务：登录 / 注册 / 登出 / 当前用户 / 修改密码 / 踢人下线
 */
public interface AuthService {

    LoginVO login(LoginDTO dto, String ip, String userAgent);

    /**
     * 账号注册：账号 6-18 位（仅字母与数字、字母开头）、不可重复；注册成功自动登录。
     *
     * @param dto       账号与密码
     * @param ip        客户端 IP
     * @param userAgent 客户端 UA
     * @return 登录返回 VO
     */
    LoginVO register(RegisterDTO dto, String ip, String userAgent);

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
