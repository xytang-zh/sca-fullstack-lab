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

    /**
     * 账号密码登录，成功返回访问令牌与会话信息。
     *
     * <p>安全约束：账号不存在与密码错误统一返回"账号或密码错误"，
     * 防止账号枚举（Account Enumeration）攻击；登录失败会累计失败次数直至锁定。
     *
     * @param dto       登录入参（账号、密码、验证码、记住我）
     * @param ip        客户端真实 IP（用于风控与异地登录告警）
     * @param userAgent 客户端 UA（用于设备识别）
     * @return 登录结果（token + 用户信息 + 角色）
     * @throws com.xytang.common.core.exception.AuthException 验证码错误 / 账号或密码错误 / 账号被禁用
     */
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

    /**
     * 登出当前会话：销毁 Sa-Token 会话（未登录时静默返回）。
     */
    void logout();

    /**
     * 查询当前登录用户信息（手机号/邮箱脱敏后返回）。
     *
     * @return 当前用户信息 VO
     * @throws com.xytang.common.core.exception.UserNotFoundException 登录用户在表不存在
     */
    UserInfoVO currentUser();

    /**
     * 修改当前登录用户密码，成功后销毁会话要求重新登录。
     *
     * @param dto 原密码 / 新密码 / 确认密码
     * @throws com.xytang.common.core.exception.AuthException 原密码错误 / 新密码与原密码相同
     */
    void updatePassword(PasswordUpdateDTO dto);

    /**
     * 预埋踢人下线接口（具体 SSO Pub/Sub 通知在 US2 完成）
     *
     * @param userId 被踢用户 ID
     */
    void kickout(Long userId);
}
