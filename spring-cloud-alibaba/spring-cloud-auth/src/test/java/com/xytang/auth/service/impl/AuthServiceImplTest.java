package com.xytang.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.entity.AuthUser;
import com.xytang.auth.enums.LoginTypeEnum;
import com.xytang.auth.mapper.AuthUserMapper;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.service.LoginRiskService;
import com.xytang.auth.vo.LoginVO;
import com.xytang.auth.constant.AuthConstants;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务实现测试：验证 Argon2id 密码哈希与登录成功/失败场景。
 *
 * <p>覆盖：
 * <ul>
 *   <li>登录成功：密码匹配、用户状态正常、返回 Token</li>
 *   <li>验证码错误 → AuthException(AUTH_CAPTCHA_ERROR)</li>
 *   <li>用户不存在 → AuthException(AUTH_PASSWORD_ERROR) + 失败计数</li>
 *   <li>密码错误 → AuthException(AUTH_PASSWORD_ERROR) + devMessage 含 Argon2id matches 返回 false</li>
 *   <li>用户被禁用 → AuthException(AUTH_USER_DISABLED)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("dev")
class AuthServiceImplTest {

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
        LoginDTO dto = buildDto();
        AuthUser user = buildUser(1L, CommonConstants.STATUS_NORMAL,
            "$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc");

        when(captchaService.verify(dto.getCaptchaKey(), dto.getCaptcha())).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword())).thenReturn(true);

        LoginVO vo = authService.login(dto, "127.0.0.1", "Mozilla/5.0");

        assertNotNull(vo);
        assertEquals("Authorization", vo.getTokenName());
        assertEquals("Bearer test-token", vo.getTokenValue());
        assertEquals("admin", vo.getUsername());
        verify(loginRiskService).clearFailure(dto.getUsername());
        verify(rabbitTemplate).convertAndSend(eq(AuthConstants.EXCHANGE_LOG_LOGIN), eq(""), any(Object.class));
    }

    @Test
    void loginCaptchaErrorShouldThrowAuthException() {
        LoginDTO dto = buildDto();
        when(captchaService.verify(dto.getCaptchaKey(), dto.getCaptcha())).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class,
            () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_CAPTCHA_ERROR, ex.getBizCode());
        assertTrue(DevMessageHolder.get().contains("验证码不匹配"));
    }

    @Test
    void loginUserNotFoundShouldThrowAuthExceptionAndRecordFailure() {
        LoginDTO dto = buildDto();
        when(captchaService.verify(dto.getCaptchaKey(), dto.getCaptcha())).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class,
            () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_PASSWORD_ERROR, ex.getBizCode());
        assertEquals("用户名或密码错误", ex.getMessage());
        verify(loginRiskService).recordFailure(dto.getUsername());
        verify(rabbitTemplate).convertAndSend(eq(AuthConstants.EXCHANGE_LOG_LOGIN), eq(""), any(Object.class));
    }

    @Test
    void loginPasswordMismatchShouldThrowAuthExceptionWithDevMessage() {
        LoginDTO dto = buildDto();
        AuthUser user = buildUser(1L, CommonConstants.STATUS_NORMAL,
            "$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc");

        when(captchaService.verify(dto.getCaptchaKey(), dto.getCaptcha())).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword())).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class,
            () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_PASSWORD_ERROR, ex.getBizCode());
        String devMessage = DevMessageHolder.get();
        assertNotNull(devMessage);
        assertTrue(devMessage.contains("Argon2id matches 返回 false"));
        verify(loginRiskService).recordFailure(dto.getUsername());
    }

    @Test
    void loginDisabledUserShouldThrowAuthException() {
        LoginDTO dto = buildDto();
        AuthUser user = buildUser(1L, CommonConstants.STATUS_DISABLED,
            "$argon2id$v=19$m=16384,t=3,p=2$+27WTLFAqxSTRl5oyRAIjw$FDm+vGxZbK72A/m7fGobGmU6Kgg6RsyuHLHJnwyfXzc");

        when(captchaService.verify(dto.getCaptchaKey(), dto.getCaptcha())).thenReturn(true);
        when(authUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword())).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class,
            () -> authService.login(dto, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(BizCode.AUTH_USER_DISABLED, ex.getBizCode());
        verify(loginRiskService, never()).clearFailure(anyString());
    }

    private LoginDTO buildDto() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");
        dto.setCaptcha("a1b2");
        dto.setCaptchaKey("key-123");
        dto.setRememberMe(false);
        return dto;
    }

    private AuthUser buildUser(Long id, Integer status, String passwordHash) {
        AuthUser user = new AuthUser();
        user.setId(id);
        user.setUsername("admin");
        user.setPassword(passwordHash);
        user.setNickname("管理员");
        user.setStatus(status);
        return user;
    }
}
