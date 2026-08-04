package com.xytang.auth.service.impl;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xytang.auth.constant.AuthConstants;
import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.PasswordUpdateDTO;
import com.xytang.auth.dto.RegisterDTO;
import com.xytang.auth.entity.AuthUser;
import com.xytang.auth.enums.DeviceTypeEnum;
import com.xytang.auth.enums.LoginTypeEnum;
import com.xytang.auth.mapper.AuthUserMapper;
import com.xytang.auth.service.AuthService;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.service.LoginRiskService;
import com.xytang.auth.vo.LoginVO;
import com.xytang.auth.vo.UserInfoVO;
import com.xytang.common.core.constant.CommonConstants;
import com.xytang.common.core.exception.AuthException;
import com.xytang.common.core.exception.UserNotFoundException;
import com.xytang.common.core.response.BizCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import cn.hutool.core.util.RandomUtil;

/**
 * 认证服务实现：登录/注销/当前用户/修改密码/踢人下线。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final int PHONE_MIN_LEN = 7;
    private static final int PHONE_HEAD_LEN = 3;
    private static final int PHONE_TAIL_LEN = 4;
    private static final int REFRESH_TOKEN_LENGTH = 64;

    private final AuthUserMapper authUserMapper;
    private final CaptchaService captchaService;
    private final LoginRiskService loginRiskService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public LoginVO login(LoginDTO dto, String ip, String userAgent) {
        // 1. 校验文字验证码（忽略大小写、一次性消费；失败不查账号，防枚举）
        if (!captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            log.warn("文字验证码无效或已过期，captchaKey={}", dto.getCaptchaKey());
            throw new AuthException(BizCode.AUTH_CAPTCHA_ERROR);
        }

        // 2. 检查账号锁定
        loginRiskService.assertNotLocked(dto.getAccount());

        // 3. 查询用户
        AuthUser user = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>()
                .eq(AuthUser::getUsername, dto.getAccount())
                .last("LIMIT 1"));
        if (user == null) {
            loginRiskService.recordFailure(dto.getAccount());
            log.warn("用户不存在，account={}", dto.getAccount());
            throw new AuthException(BizCode.AUTH_PASSWORD_ERROR, "账号或密码错误");
        }

        // 4. 校验密码（统一返回"账号或密码错误"，不区分账号与密码错误）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            loginRiskService.recordFailure(dto.getAccount());
            log.warn("Argon2id 密码校验不匹配，username={}", dto.getAccount());
            throw new AuthException(BizCode.AUTH_PASSWORD_ERROR, "账号或密码错误");
        }

        // 5. 校验状态（被禁用/删除）
        if (user.getStatus() == null
                || CommonConstants.STATUS_NORMAL != user.getStatus()) {
            log.warn("用户已被禁用，username={}, status={}", dto.getAccount(), user.getStatus());
            throw new AuthException(BizCode.AUTH_USER_DISABLED);
        }

        // 6. 登录成功：清失败计数 + 签发会话
        loginRiskService.clearFailure(dto.getAccount());
        long timeout = Boolean.TRUE.equals(dto.getRememberMe())
                ? AuthConstants.REMEMBER_ME_TIMEOUT_SECONDS
                : AuthConstants.LOGIN_TOKEN_TIMEOUT_SECONDS;
        List<String> roleCodes = authUserMapper.selectRoleCodesByUserId(user.getId());
        return doLogin(user, timeout, LoginTypeEnum.LOGIN, ip, userAgent, roleCodes, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto, String ip, String userAgent) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AuthException(BizCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 1. 账号查重（并发场景由唯一索引 + DuplicateKeyException 兜底）
        AuthUser exists = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>()
                .eq(AuthUser::getUsername, dto.getAccount())
                .last("LIMIT 1"));
        if (exists != null) {
            throw new AuthException(BizCode.AUTH_USER_EXISTED, "账号已存在");
        }

        // 2. 插入用户（Argon2id 加密，默认 USER 角色）
        AuthUser user = new AuthUser();
        user.setUsername(dto.getAccount());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getAccount());
        user.setStatus(CommonConstants.STATUS_NORMAL);
        try {
            authUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册唯一索引兜底：同样视为账号已存在
            throw new AuthException(BizCode.AUTH_USER_EXISTED, "账号已存在");
        }
        Long roleId = authUserMapper.selectUserRoleId();
        if (roleId != null) {
            authUserMapper.insertUserRole(user.getId(), roleId);
        }

        // 3. 注册成功自动登录
        List<String> roleCodes = authUserMapper.selectRoleCodesByUserId(user.getId());
        return doLogin(user, AuthConstants.LOGIN_TOKEN_TIMEOUT_SECONDS, LoginTypeEnum.LOGIN,
                ip, userAgent, roleCodes, List.of());
    }

    /**
     * 登录成功公共流程：签发会话、更新登录信息、发 Refresh Token、记日志事件。
     *
     * @param user      已通过校验的用户
     * @param timeout   Token 有效期（秒）
     * @param loginType 登录类型
     * @param ip        客户端 IP
     * @param userAgent 客户端 UA
     * @param roles     角色 code 列表
     * @param perms     权限点列表
     * @return 登录返回 VO
     */
    private LoginVO doLogin(AuthUser user, long timeout, LoginTypeEnum loginType,
                            String ip, String userAgent, List<String> roles, List<String> perms) {
        SaLoginParameter param = new SaLoginParameter()
                .setDeviceType(DeviceTypeEnum.PC.name())
                .setTimeout(timeout)
                .setIsLastingCookie(Boolean.TRUE);
        StpUtil.login(user.getId(), param);

        // 更新登录信息（last_login_time / last_login_ip）
        AuthUser update = new AuthUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        update.setLastLoginIp(ip);
        authUserMapper.updateById(update);

        // 生成 Refresh Token（7d 有效，一次性消费）
        String refreshToken = RandomUtil.randomString(REFRESH_TOKEN_LENGTH);
        stringRedisTemplate.opsForValue().set(
                AuthConstants.REFRESH_TOKEN_PREFIX + refreshToken,
                String.valueOf(user.getId()),
                AuthConstants.REMEMBER_ME_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);

        return LoginVO.builder()
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .expiresIn(timeout)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roles)
                .perms(perms)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout() {
        if (!StpUtil.isLogin()) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
    }

    @Override
    public UserInfoVO currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        AuthUser user = authUserMapper.selectById(userId);
        if (user == null) {
            log.warn("当前登录用户在 sys_user 表不存在，userId={}", userId);
            throw new UserNotFoundException();
        }
        List<String> roleCodes = authUserMapper.selectRoleCodesByUserId(userId);
        List<UserInfoVO.RoleBriefVO> roles = roleCodes.stream()
                .map(code -> UserInfoVO.RoleBriefVO.builder()
                        .code(code)
                        .name(code)
                        .build())
                .collect(Collectors.toList());
        return UserInfoVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(maskEmail(user.getEmail()))
            .phone(maskPhone(user.getPhone()))
            .avatar(user.getAvatar())
            .deptId(user.getDeptId())
            .deptName(null)
            .roles(roles)
            .perms(List.of())
            .lastLoginTime(user.getLastLoginTime())
            .lastLoginIp(user.getLastLoginIp())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordUpdateDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new AuthException(BizCode.PARAM_ERROR, "两次密码不一致");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        AuthUser user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            log.warn("原密码不匹配，userId={}", userId);
            throw new AuthException(BizCode.AUTH_PASSWORD_ERROR, "原密码错误");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new AuthException(BizCode.PARAM_ERROR, "新密码不能与原密码相同");
        }
        AuthUser update = new AuthUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        authUserMapper.updateById(update);
        // 修改成功后销毁当前会话，要求重新登录
        StpUtil.logout();
    }

    @Override
    public void kickout(Long userId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId.equals(userId)) {
            throw new AuthException(BizCode.PARAM_ERROR, "禁止踢自己下线");
        }
        StpUtil.kickout(userId);
        log.info("[Auth] kickout: targetUserId={} operator={}", userId, currentUserId);
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        String prefix = email.substring(0, at);
        String maskedPrefix = prefix.isEmpty() ? ""
                : prefix.charAt(0) + "*".repeat(Math.max(0, prefix.length() - 1));
        return maskedPrefix + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < PHONE_MIN_LEN) {
            return phone;
        }
        return phone.substring(0, PHONE_HEAD_LEN) + "****"
            + phone.substring(phone.length() - PHONE_TAIL_LEN);
    }
}
