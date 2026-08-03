package com.xytang.article.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 主配置（article 服务侧）。
 *
 * <p>游客浏览契约：GET 文章列表/详情匿名可访问；发布/点赞/收藏等写操作要求登录，
 * 未登录统一抛 NotLoginException，由全局异常处理器返回业务码 401。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final String[] WHITELIST = new String[] {
        "/actuator/**",
        "/doc.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/favicon.ico",
        "/error"
    };

    private static final String ARTICLES_PREFIX = "/articles";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/**")
                .notMatch(WHITELIST)
                .check(r -> {
                    String method = SaHolder.getRequest().getMethod();
                    String path = SaHolder.getRequest().getRequestPath();
                    // 游客浏览：GET 文章列表/详情匿名放行
                    boolean publicRead = "GET".equals(method)
                            && (ARTICLES_PREFIX.equals(path) || path.startsWith(ARTICLES_PREFIX + "/"));
                    if (!publicRead) {
                        StpUtil.checkLogin();
                    }
                }))).addPathPatterns("/**");
    }
}
