package com.xytang.article.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xytang.article.dto.ArticleCreateDTO;
import com.xytang.article.dto.ArticlePageQuery;
import com.xytang.article.service.ArticleService;
import com.xytang.article.vo.ArticleDetailVO;
import com.xytang.article.vo.ArticleVO;
import com.xytang.common.core.response.PageVO;
import com.xytang.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 博客文章控制器：列表/详情游客可读，发布/点赞/收藏需登录。
 */
@Tag(name = "博客文章")
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "文章分页列表（游客可访问，sort=time|hot）")
    @GetMapping
    public R<PageVO<ArticleVO>> page(@Valid ArticlePageQuery query) {
        return R.ok(articleService.page(query));
    }

    @Operation(summary = "文章详情（游客可访问，阅读量 +1）")
    @GetMapping("/{id}")
    public R<ArticleDetailVO> detail(@PathVariable Long id) {
        return R.ok(articleService.detail(id));
    }

    @Operation(summary = "发布文章（需登录）")
    @PostMapping
    @SaCheckLogin
    public R<ArticleVO> create(@RequestBody @Valid ArticleCreateDTO dto) {
        return R.ok(articleService.create(dto, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "点赞/取消点赞（需登录，幂等）")
    @PostMapping("/{id}/like")
    @SaCheckLogin
    public R<Boolean> like(@PathVariable Long id) {
        return R.ok(articleService.toggleLike(id, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "收藏/取消收藏（需登录，幂等）")
    @PostMapping("/{id}/favorite")
    @SaCheckLogin
    public R<Boolean> favorite(@PathVariable Long id) {
        return R.ok(articleService.toggleFavorite(id, StpUtil.getLoginIdAsLong()));
    }
}
