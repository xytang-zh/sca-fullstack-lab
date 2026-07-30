package com.xytang.auth.service;

import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.PasswordUpdateDTO;
import com.xytang.auth.vo.LoginVO;
import com.xytang.auth.vo.UserInfoVO;

/**
 * 认证服务：登录 / 登出 / 当前用户 / 修改密码 / 踢人下线
 */
public interface AuthService {

    LoginVO login(LoginDTO dto, String ip, String userAgent);

    void logout();

    UserInfoVO currentUser();

    void updatePassword(PasswordUpdateDTO dto);

    /**
     * 预埋踢人下线接口（具体 SSO Pub/Sub 通知在 US2 完成）
     */
    void kickout(Long userId);
}
