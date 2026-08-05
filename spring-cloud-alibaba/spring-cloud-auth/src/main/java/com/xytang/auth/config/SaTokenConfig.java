package com.xytang.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 主配置（auth 服务侧）
 *
 * <p>MVP 阶段仅做基础路由拦截：登录/验证码/健康检查允许匿名，其余接口要求已登录。
 * SSO 模式二、OAuth2 Server、踢人下线 Pub/Sub 在 US2 完善。
 *
 * <p>白名单路径：/sso/** 覆盖 SSO 全端点（auth/code/code2session/logout/refresh/online/kickout）。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    // 匿名白名单：登录/注册/验证码/SSO/OAuth2/监控/文档等无需登录即可访问
    private static final String[] WHITELIST = new String[] {
        "/login",
        "/register",
        "/captcha",
        "/sso/**",
        "/oauth2/**",
        "/actuator/**",
        "/doc.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/favicon.ico",
        "/error"
    };

    /**
     * 注册 Sa-Token 拦截器：除白名单外的所有路径要求已登录。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/**")
                .notMatch(WHITELIST)
                .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/**");
    }
}
