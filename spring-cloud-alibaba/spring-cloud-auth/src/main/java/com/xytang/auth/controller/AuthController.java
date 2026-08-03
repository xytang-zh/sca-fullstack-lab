package com.xytang.auth.controller;

import com.xytang.auth.dto.CaptchaCheckDTO;
import com.xytang.auth.dto.KickoutDTO;
import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.PasswordUpdateDTO;
import com.xytang.auth.service.AuthService;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.vo.CaptchaVO;
import com.xytang.auth.vo.LoginVO;
import com.xytang.auth.vo.UserInfoVO;
import com.xytang.common.core.exception.AuthException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证中心控制器：验证码/登录/登出/用户信息。
 */
@Tag(name = "认证中心")
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "获取验证码（tianai SDK 用 POST 请求）")
    @PostMapping("/captcha")
    public R<CaptchaVO> captchaByPost() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "校验滑块并签发一次性 checkToken（tianai SDK 回调接口）")
    @PostMapping("/captcha/check")
    public R<Map<String, String>> captchaCheck(@RequestBody @Valid CaptchaCheckDTO dto) {
        String checkToken = captchaService.check(dto.getId(), dto.getData());
        if (checkToken == null) {
            throw new AuthException(BizCode.AUTH_CAPTCHA_ERROR);
        }
        return R.ok(Map.of("checkToken", checkToken));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        String ip = resolveIp(request);
        String ua = request.getHeader("User-Agent");
        return R.ok(authService.login(dto, ip, ua));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.ok(authService.currentUser());
    }

    @Operation(summary = "修改密码")
    @PatchMapping("/me/password")
    public R<Void> updatePassword(@RequestBody @Valid PasswordUpdateDTO dto) {
        authService.updatePassword(dto);
        return R.ok();
    }

    @Operation(summary = "踢人下线（预埋：US2 完善 SLO 通知）")
    @PostMapping("/kickout")
    public R<Void> kickout(@RequestBody @Valid KickoutDTO dto) {
        authService.kickout(dto.getUserId());
        return R.ok("已将用户踢下线", null);
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
