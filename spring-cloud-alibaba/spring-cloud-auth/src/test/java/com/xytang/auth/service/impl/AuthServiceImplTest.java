package com.xytang.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.xytang.auth.constant.AuthConstants;
import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.RegisterDTO;
import com.xytang.auth.entity.AuthUser;
import com.xytang.auth.mapper.AuthUserMapper;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.service.LoginRiskService;
import com.xytang.auth.vo.LoginVO;
import com.xytang.common.core.constant.CommonConstants;
import com.xytang.common.core.exception.AuthException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.DevMessageHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务实现测试：验证文字验证码校验、账号密码登录与注册流程。
 *
 * <p>覆盖：
 * <ul>
 *   <li>登录成功：验证码通过、密码匹配、用户状态正常、返回 Token</li>
 *   <li>验证码错误 → AuthException(AUTH_CAPTCHA_ERROR)，不查账号、不计失败</li>
 *   <li>用户不存在 → AuthException(AUTH_PASSWORD_ERROR) + 失败计数</li>
 *   <li>密码错误 → AuthException(AUTH_PASSWORD_ERROR) + devMessage 含 Argon2id matches 返回 false</li>
 *   <li>用户被禁用 → AuthException(AUTH_USER_DISABLED)</li>
 *   <li>注册成功：插入用户 + USER 角色 + 自动登录</li>
 *   <li>注册重复账号 → AuthException(AUTH_USER_EXISTED)</li>
 *   <li>两次密码不一致 → AuthException(PARAM_ERROR)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("dev")
class AuthServiceImplTest {

    private static final String ACCOUNT = "testuser";
    private static final String CAPTCHA_KEY = "captcha-key-123";
    private static final String CAPTCHA_CODE = "abcd";
    private static final Long USER_ROLE_ID = 10L;

    @Mock
    private AuthUserMapper authUserMapper;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private LoginRiskService loginRiskService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        DevMessageHolder.clear();
        stpUtilMock = mockStatic(StpUtil.class);
        stpUtilMock.when(() -> StpUtil.login(any(), any(SaLoginParameter.class))).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getTokenName).thenReturn("Authorization");
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("Bearer test-token");
    }

    @AfterEach
    void tearDown() {
        if (stpUtilMock != null) {
            stpUtilMock.close();
        }
        DevMessageHolder.clear();
    }

    @Test
    void loginSuccessShouldReturnLoginVo() {
        LoginDTO dto = buildLoginDto();
        AuthUser user = buildUser(1L, CommonConstants.STATUS_NORMAL,
                "$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc");

        when(captchaService.verify(CAPTCHA_KEY, CAPTCHA_CODE)).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword())).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));

        LoginVO vo = authService.login(dto, "127.0.0.1", "Mozilla/5.0");

        assertNotNull(vo);
        assertEquals("Authorization", vo.getTokenName());
        assertEquals("Bearer test-token", vo.getTokenValue());
        assertEquals("testuser", vo.getUsername());
        verify(loginRiskService).clearFailure(dto.getAccount());
        verify(rabbitTemplate).convertAndSend(eq(AuthConstants.EXCHANGE_LOG_LOGIN), eq(""), any(Object.class));
    }

    @Test
    void loginCaptchaErrorShouldThrowAuthException() {
        LoginDTO dto = buildLoginDto();
        when(captchaService.verify(CAPTCHA_KEY, CAPTCHA_CODE)).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_CAPTCHA_ERROR, ex.getBizCode());
        assertTrue(DevMessageHolder.get().contains("文字验证码无效或已过期"));
        // 验证码失败不查账号、不计入登录失败锁定计数
        verify(authUserMapper, never()).selectOne(any());
        verify(loginRiskService, never()).recordFailure(anyString());
    }

    @Test
    void loginUserNotFoundShouldThrowAuthExceptionAndRecordFailure() {
        LoginDTO dto = buildLoginDto();
        when(captchaService.verify(CAPTCHA_KEY, CAPTCHA_CODE)).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_PASSWORD_ERROR, ex.getBizCode());
        assertEquals("账号或密码错误", ex.getMessage());
        verify(loginRiskService).recordFailure(dto.getAccount());
        verify(rabbitTemplate).convertAndSend(eq(AuthConstants.EXCHANGE_LOG_LOGIN), eq(""), any(Object.class));
    }

    @Test
    void loginPasswordMismatchShouldThrowAuthExceptionWithDevMessage() {
        LoginDTO dto = buildLoginDto();
        AuthUser user = buildUser(1L, CommonConstants.STATUS_NORMAL,
                "$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc");

        when(captchaService.verify(CAPTCHA_KEY, CAPTCHA_CODE)).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword())).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_PASSWORD_ERROR, ex.getBizCode());
        assertEquals("账号或密码错误", ex.getMessage());
        String devMessage = DevMessageHolder.get();
        assertNotNull(devMessage);
        assertTrue(devMessage.contains("Argon2id matches 返回 false"));
        verify(loginRiskService).recordFailure(dto.getAccount());
    }

    @Test
    void loginDisabledUserShouldThrowAuthException() {
        LoginDTO dto = buildLoginDto();
        AuthUser user = buildUser(1L, CommonConstants.STATUS_DISABLED,
                "$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc");

        when(captchaService.verify(CAPTCHA_KEY, CAPTCHA_CODE)).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword())).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_USER_DISABLED, ex.getBizCode());
        verify(loginRiskService, never()).clearFailure(anyString());
    }

    @Test
    void registerSuccessShouldInsertUserWithRoleAndAutoLogin() {
        RegisterDTO dto = buildRegisterDto();

        when(authUserMapper.selectOne(any())).thenReturn(null);
        when(authUserMapper.selectUserRoleId()).thenReturn(USER_ROLE_ID);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed");
        when(stringRedisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));
        // 模拟 MyBatis-Plus 插入后回填雪花 ID
        when(authUserMapper.insert(any(AuthUser.class))).thenAnswer(invocation -> {
            AuthUser inserted = invocation.getArgument(0);
            inserted.setId(1L);
            return 1;
        });

        LoginVO vo = authService.register(dto, "127.0.0.1", "Mozilla/5.0");

        assertNotNull(vo);
        assertEquals("testuser", vo.getUsername());
        verify(authUserMapper).insert(any(AuthUser.class));
        verify(authUserMapper).insertUserRole(eq(1L), eq(USER_ROLE_ID));
        verify(rabbitTemplate).convertAndSend(eq(AuthConstants.EXCHANGE_LOG_LOGIN), eq(""), any(Object.class));
    }

    @Test
    void registerDuplicateAccountShouldThrowAuthException() {
        RegisterDTO dto = buildRegisterDto();
        when(authUserMapper.selectOne(any())).thenReturn(buildUser(2L, CommonConstants.STATUS_NORMAL, "hashed"));

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.register(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_USER_EXISTED, ex.getBizCode());
        assertEquals("账号已存在", ex.getMessage());
        verify(authUserMapper, never()).insert(any(AuthUser.class));
    }

    @Test
    void registerDuplicateKeyShouldThrowAuthException() {
        RegisterDTO dto = buildRegisterDto();
        when(authUserMapper.selectOne(any())).thenReturn(null);
        when(authUserMapper.insert(any(AuthUser.class))).thenThrow(new DuplicateKeyException("dup"));

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.register(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_USER_EXISTED, ex.getBizCode());
        assertEquals("账号已存在", ex.getMessage());
    }

    @Test
    void registerPasswordMismatchShouldThrowAuthException() {
        RegisterDTO dto = buildRegisterDto();
        dto.setConfirmPassword("different");

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.register(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.PARAM_ERROR, ex.getBizCode());
        assertEquals("两次输入的密码不一致", ex.getMessage());
        verify(authUserMapper, never()).selectOne(any());
    }

    private LoginDTO buildLoginDto() {
        LoginDTO dto = new LoginDTO();
        dto.setAccount(ACCOUNT);
        dto.setPassword("admin123");
        dto.setCaptchaKey(CAPTCHA_KEY);
        dto.setCaptchaCode(CAPTCHA_CODE);
        dto.setRememberMe(false);
        return dto;
    }

    private RegisterDTO buildRegisterDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setAccount(ACCOUNT);
        dto.setPassword("admin123");
        dto.setConfirmPassword("admin123");
        return dto;
    }

    private AuthUser buildUser(Long id, Integer status, String passwordHash) {
        AuthUser user = new AuthUser();
        user.setId(id);
        user.setUsername(ACCOUNT);
        user.setPassword(passwordHash);
        user.setNickname("测试用户");
        user.setStatus(status);
        return user;
    }
}
