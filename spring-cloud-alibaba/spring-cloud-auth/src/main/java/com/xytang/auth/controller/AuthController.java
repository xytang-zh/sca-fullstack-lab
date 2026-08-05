package com.xytang.auth.controller;

import com.xytang.auth.dto.KickoutDTO;
import com.xytang.auth.dto.LoginDTO;
import com.xytang.auth.dto.PasswordUpdateDTO;
import com.xytang.auth.dto.RegisterDTO;
import com.xytang.auth.service.AuthService;
import com.xytang.auth.service.CaptchaService;
import com.xytang.auth.vo.CaptchaVO;
import com.xytang.auth.vo.LoginVO;
import com.xytang.auth.vo.UserInfoVO;
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

/**
 * 认证中心控制器：验证码/登录/注册/登出/用户信息。
 */
@Tag(name = "认证中心")
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取文字图形验证码")
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "获取文字图形验证码（POST）")
    @PostMapping("/captcha")
    public R<CaptchaVO> captchaByPost() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "账号密码登录（需文字验证码）")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        String ip = resolveIp(request);
        String ua = request.getHeader("User-Agent");
        return R.ok(authService.login(dto, ip, ua));
    }

    @Operation(summary = "账号注册（注册成功自动登录）")
    @PostMapping("/register")
    public R<LoginVO> register(@RequestBody @Valid RegisterDTO dto, HttpServletRequest request) {
        String ip = resolveIp(request);
        String ua = request.getHeader("User-Agent");
        return R.ok(authService.register(dto, ip, ua));
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

    // 解析客户端真实 IP：优先 X-Forwarded-For（反向代理协商的最左 IP），其次 X-Real-IP，最后 RemoteAddr
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
