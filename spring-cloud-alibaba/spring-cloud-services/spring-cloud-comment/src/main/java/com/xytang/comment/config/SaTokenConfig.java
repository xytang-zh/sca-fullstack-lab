package com.xytang.comment.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 主配置（comment 服务侧）。
 *
 * <p>游客浏览契约：GET 文章评论列表匿名可访问；发表/回复/点赞/审核等写操作要求登录。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** 免登录白名单：监控端点、Swagger 文档、静态资源与错误页 */
    private static final String[] WHITELIST = new String[] {
        "/actuator/**",
        "/doc.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/favicon.ico",
        "/error"
    };

    /** 评论列表路径前缀：GET 请求匿名放行，供游客浏览已审核评论 */
    private static final String ARTICLES_PREFIX = "/articles";

    /**
     * 注册 Sa-Token 拦截器：除白名单外全部要求登录，仅"GET /articles/**"对游客放行。
     *
     * <p>评论服务侧只做登录态校验，角色/权限校验由接口上的 Sa-Token 注解控制；
     * 网关负责剥离 /api/comments 前缀，因此此处路径直接以 /articles 开头。</p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/**")
                .notMatch(WHITELIST)
                .check(r -> {
                    String method = SaHolder.getRequest().getMethod();
                    String path = SaHolder.getRequest().getRequestPath();
                    // 游客浏览：GET 评论列表匿名放行（网关已剥离 /api/comments 前缀）
                    boolean publicRead = "GET".equals(method)
                            && path.startsWith(ARTICLES_PREFIX + "/");
                    if (!publicRead) {
                        StpUtil.checkLogin();
                    }
                }))).addPathPatterns("/**");
    }
}