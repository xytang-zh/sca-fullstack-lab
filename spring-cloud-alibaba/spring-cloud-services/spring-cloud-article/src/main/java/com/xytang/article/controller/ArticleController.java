package com.xytang.article.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xytang.article.dto.ArticleCreateDTO;
import com.xytang.article.dto.ArticlePageQuery;
import com.xytang.article.service.ArticleService;
import com.xytang.article.vo.ArticleDetailVO;
import com.xytang.article.vo.ArticleStatsVO;
import com.xytang.article.vo.ArticleVO;
import com.xytang.common.core.response.PageVO;
import com.xytang.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 博客文章控制器：列表/详情游客可读，发布/点赞/收藏需登录。
 */
@Tag(name = "博客文章")
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@Validated
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "文章分页列表（游客可访问，sort=time|hot）")
    @GetMapping
    public R<PageVO<ArticleVO>> page(@Valid ArticlePageQuery query) {
        return R.ok(articleService.page(query));
    }

    @Operation(summary = "我的已发布文章（需登录，按用户隔离）")
    @GetMapping("/my")
    @SaCheckLogin
    public R<PageVO<ArticleVO>> my(@RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                   @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return R.ok(articleService.pageMyArticles(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    @Operation(summary = "我的草稿（需登录，按用户隔离）")
    @GetMapping("/my/drafts")
    @SaCheckLogin
    public R<PageVO<ArticleVO>> myDrafts(@RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                         @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return R.ok(articleService.pageMyDrafts(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    @Operation(summary = "我点赞的文章（需登录，按用户隔离）")
    @GetMapping("/my/likes")
    @SaCheckLogin
    public R<PageVO<ArticleVO>> myLikes(@RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                        @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return R.ok(articleService.pageMyLikes(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    @Operation(summary = "我收藏的文章（需登录，按用户隔离）")
    @GetMapping("/my/favorites")
    @SaCheckLogin
    public R<PageVO<ArticleVO>> myFavorites(@RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return R.ok(articleService.pageMyFavorites(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    @Operation(summary = "获取文章用于编辑（仅作者，含草稿/待审核）")
    @GetMapping("/my/{id}")
    @SaCheckLogin
    public R<ArticleDetailVO> getForEdit(@PathVariable Long id) {
        return R.ok(articleService.getForEdit(id, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "文章统计（管理员）")
    @GetMapping("/stats")
    @SaCheckLogin
    public R<ArticleStatsVO> stats() {
        return R.ok(articleService.stats());
    }

    @Operation(summary = "待审核文章列表（管理员）")
    @GetMapping("/pending")
    @SaCheckLogin
    public R<PageVO<ArticleVO>> pending(@RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                        @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return R.ok(articleService.pagePending(pageNum, pageSize));
    }

    @Operation(summary = "审核文章（管理员，3=通过 4=驳回）")
    @PostMapping("/{id}/audit")
    @SaCheckLogin
    public R<Void> audit(@PathVariable Long id, @RequestParam Integer status) {
        articleService.audit(id, status);
        return R.ok();
    }

    @Operation(summary = "文章详情（游客可访问，阅读量 +1）")
    @GetMapping("/{id}")
    public R<ArticleDetailVO> detail(@PathVariable Long id) {
        return R.ok(articleService.detail(id));
    }

    @Operation(summary = "发布文章（需登录，status=1草稿/3发布）")
    @PostMapping
    @SaCheckLogin
    public R<ArticleVO> create(@RequestBody @Valid ArticleCreateDTO dto) {
        return R.ok(articleService.create(dto, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "更新文章（仅作者）")
    @PutMapping("/{id}")
    @SaCheckLogin
    public R<ArticleVO> update(@PathVariable Long id, @RequestBody @Valid ArticleCreateDTO dto) {
        return R.ok(articleService.update(dto, id, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "删除文章（仅作者或管理员，软删除）")
    @DeleteMapping("/{id}")
    @SaCheckLogin
    public R<Void> delete(@PathVariable Long id) {
        articleService.delete(id, StpUtil.getLoginIdAsLong());
        return R.ok();
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
