package com.xytang.auth.service.impl;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xytang.auth.constant.AuthConstants;
import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.PasswordUpdateDTO;
import com.xytang.auth.dto.SmsLoginDTO;
import com.xytang.auth.entity.AuthUser;
import com.xytang.auth.enums.DeviceTypeEnum;
import com.xytang.auth.enums.LoginTypeEnum;
import com.xytang.auth.event.UserLoginEvent;
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
import com.xytang.common.core.response.DevMessageHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import cn.hutool.core.util.RandomUtil;

/**
 * 认证服务实现：登录/注销/当前用户/修改密码/踢人下线。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final int HASH_PREVIEW_LEN = 40;
    private static final int PHONE_MIN_LEN = 7;
    private static final int PHONE_HEAD_LEN = 3;
    private static final int PHONE_TAIL_LEN = 4;
    private static final int REFRESH_TOKEN_LENGTH = 64;
    private static final int RANDOM_PASSWORD_LENGTH = 32;

    private final AuthUserMapper authUserMapper;
    private final CaptchaService captchaService;
    private final LoginRiskService loginRiskService;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public LoginVO login(LoginDTO dto, String ip, String userAgent) {
        // 1. 按需校验滑块 checkToken（有值则校验，无值则跳过）
        if (StringUtils.hasText(dto.getCheckToken())) {
            if (!captchaService.verifyCheckToken(dto.getCheckToken())) {
                publishLoginEvent(null, dto.getUsername(), LoginTypeEnum.LOGIN, ip, userAgent,
                        Boolean.FALSE, "captcha invalid");
                DevMessageHolder.set("滑块验证码凭据无效或已过期，checkToken=" + dto.getCheckToken());
                throw new AuthException(BizCode.AUTH_CAPTCHA_ERROR);
            }
        }

        // 2. 检查账号锁定
        loginRiskService.assertNotLocked(dto.getUsername());

        // 3. 查询用户
        AuthUser user = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>()
                .eq(AuthUser::getUsername, dto.getUsername())
                .last("LIMIT 1"));
        if (user == null) {
            loginRiskService.recordFailure(dto.getUsername());
            publishLoginEvent(null, dto.getUsername(), LoginTypeEnum.LOGIN, ip, userAgent,
                    Boolean.FALSE, "user not found");
            DevMessageHolder.set("用户不存在，username=" + dto.getUsername());
            throw new AuthException(BizCode.AUTH_PASSWORD_ERROR);
        }

        // 4. 校验密码（统一返回"账号或密码错误"，不区分用户名与密码错误）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            loginRiskService.recordFailure(dto.getUsername());
            publishLoginEvent(user.getId(), dto.getUsername(), LoginTypeEnum.LOGIN, ip, userAgent,
                    Boolean.FALSE, "password mismatch");
            DevMessageHolder.set("Argon2id matches 返回 false，输入密码 '"
                    + dto.getPassword() + "' 与数据库哈希 '"
                    + user.getPassword().substring(0, Math.min(HASH_PREVIEW_LEN,
                        user.getPassword().length())) + "' 不匹配");
            throw new AuthException(BizCode.AUTH_PASSWORD_ERROR);
        }

        // 5. 校验状态（被禁用/删除）
        if (user.getStatus() == null
                || CommonConstants.STATUS_NORMAL != user.getStatus()) {
            publishLoginEvent(user.getId(), dto.getUsername(), LoginTypeEnum.LOGIN, ip, userAgent,
                    Boolean.FALSE, "account disabled");
            DevMessageHolder.set("用户已被禁用，status=" + user.getStatus());
            throw new AuthException(BizCode.AUTH_USER_DISABLED);
        }

        // 6. 登录成功：清失败计数 + 签发会话
        loginRiskService.clearFailure(dto.getUsername());
        long timeout = Boolean.TRUE.equals(dto.getRememberMe())
                ? AuthConstants.REMEMBER_ME_TIMEOUT_SECONDS
                : AuthConstants.LOGIN_TOKEN_TIMEOUT_SECONDS;
        return doLogin(user, timeout, LoginTypeEnum.LOGIN, ip, userAgent,
                List.of(CommonConstants.SUPER_ADMIN_ROLE_CODE), List.of(AuthConstants.SUPER_ADMIN_PERMS));
    }

    @Override
    public LoginVO smsLogin(SmsLoginDTO dto, String ip, String userAgent) {
        String phone = dto.getPhone();

        // 1. 校验并作废短信验证码（一次性消费）
        String codeKey = AuthConstants.SMS_CODE_PREFIX + phone;
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.equals(dto.getCode())) {
            publishLoginEvent(null, phone, LoginTypeEnum.SMS, ip, userAgent,
                    Boolean.FALSE, "sms code invalid");
            throw new AuthException(BizCode.AUTH_CAPTCHA_ERROR, "验证码错误或已过期");
        }
        stringRedisTemplate.delete(codeKey);

        // 2. 风控检查
        loginRiskService.assertNotLocked(phone);

        // 3. 按手机号查用户，不存在则自动注册（登录/注册一体）
        AuthUser user = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>()
                .eq(AuthUser::getPhone, phone)
                .last("LIMIT 1"));
        if (user == null) {
            user = registerByPhone(phone);
        }

        // 4. 状态校验（被禁用/删除）
        if (user.getStatus() == null || CommonConstants.STATUS_NORMAL != user.getStatus()) {
            publishLoginEvent(user.getId(), user.getUsername(), LoginTypeEnum.SMS, ip, userAgent,
                    Boolean.FALSE, "account disabled");
            throw new AuthException(BizCode.AUTH_USER_DISABLED);
        }

        // 5. 登录成功
        loginRiskService.clearFailure(phone);
        List<String> roleCodes = authUserMapper.selectRoleCodesByUserId(user.getId());
        return doLogin(user, AuthConstants.LOGIN_TOKEN_TIMEOUT_SECONDS, LoginTypeEnum.SMS, ip, userAgent,
                roleCodes, List.of());
    }

    /**
     * 手机号自动注册：username=手机号、随机不可登录密码、默认 USER 角色。
     *
     * @param phone 手机号
     * @return 注册后的用户
     */
    private AuthUser registerByPhone(String phone) {
        AuthUser user = new AuthUser();
        user.setUsername(phone);
        user.setPassword(passwordEncoder.encode(RandomUtil.randomString(RANDOM_PASSWORD_LENGTH)));
        user.setNickname(maskPhone(phone));
        user.setPhone(phone);
        user.setStatus(CommonConstants.STATUS_NORMAL);
        try {
            authUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时唯一索引兜底：重新查询已存在用户
            AuthUser existing = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>()
                    .eq(AuthUser::getPhone, phone)
                    .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
            throw e;
        }
        Long roleId = authUserMapper.selectUserRoleId();
        if (roleId != null) {
            authUserMapper.insertUserRole(user.getId(), roleId);
        }
        return user;
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

        // 异步发送登录日志事件
        publishLoginEvent(user.getId(), user.getUsername(), loginType, ip, userAgent,
                Boolean.TRUE, null);

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
        AuthUser user = authUserMapper.selectById(userId);
        StpUtil.logout();
        publishLoginEvent(userId,
                user == null ? null : user.getUsername(),
                LoginTypeEnum.LOGOUT, null, null,
                Boolean.TRUE, null);
    }

    @Override
    public UserInfoVO currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        AuthUser user = authUserMapper.selectById(userId);
        if (user == null) {
            DevMessageHolder.set("当前登录用户在 sys_user 表不存在，userId=" + userId);
            throw new UserNotFoundException();
        }
        return UserInfoVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(maskEmail(user.getEmail()))
            .phone(maskPhone(user.getPhone()))
            .avatar(user.getAvatar())
            .deptId(user.getDeptId())
            .deptName(null)
            .roles(List.of(UserInfoVO.RoleBriefVO.builder()
                .id(1L)
                .code(CommonConstants.SUPER_ADMIN_ROLE_CODE)
                .name("超级管理员")
                .dataScope(CommonConstants.DATA_SCOPE_ALL)
                .build()))
            .perms(List.of(AuthConstants.SUPER_ADMIN_PERMS))
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
            DevMessageHolder.set("原密码不匹配");
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
        AuthUser user = authUserMapper.selectById(userId);
        publishLoginEvent(userId,
                user == null ? null : user.getUsername(),
                LoginTypeEnum.KICKOUT, null, null,
                Boolean.TRUE, null);
        log.info("[Auth] kickout: targetUserId={} operator={}", userId, currentUserId);
    }

    /**
     * 异步发送登录日志事件到 log.login.create Exchange（由 log 服务消费写表）
     *
     * @param userId     登录用户 ID（未认证为 null）
     * @param username   用户名
     * @param type       事件类型
     * @param ip         客户端 IP
     * @param userAgent  客户端 UA
     * @param success    是否成功
     * @param failReason 失败原因（成功为 null）
     */
    private void publishLoginEvent(Long userId, String username, LoginTypeEnum type,
                                   String ip, String userAgent,
                                   Boolean success, String failReason) {
        try {
            UserLoginEvent event = new UserLoginEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType(type.msg());
            event.setUserId(userId);
            event.setUsername(username);
            event.setLoginType(type.code());
            event.setDeviceType(DeviceTypeEnum.PC.name());
            event.setIp(ip);
            event.setUserAgent(userAgent);
            event.setSuccess(success);
            event.setFailReason(failReason);
            event.setLoginTime(LocalDateTime.now());
            rabbitTemplate.convertAndSend(AuthConstants.EXCHANGE_LOG_LOGIN, "", event);
        } catch (AmqpException e) {
            log.error("[Auth] publish login event failed: userId={} type={}", userId, type, e);
        }
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
