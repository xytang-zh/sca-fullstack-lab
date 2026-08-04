package com.xytang.article.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xytang.article.dto.ArticleCreateDTO;
import com.xytang.article.dto.ArticlePageQuery;
import com.xytang.article.service.ArticleService;
import com.xytang.article.vo.ArticleDetailVO;
import com.xytang.article.vo.ArticleStatsVO;
import com.xytang.article.vo.ArticleVO;
import com.xytang.common.core.response.PageResult;
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

    /**
     * 分页查询文章列表（游客可访问，支持 sort=time|hot 排序、分类/关键字过滤）。
     *
     * @param query 分页与过滤条件
     * @return 文章分页结果
     */
    @Operation(summary = "文章分页列表（游客可访问，sort=time|hot）")
    @GetMapping
    public R<PageResult<ArticleVO>> page(@Valid ArticlePageQuery query) {
        return R.ok(articleService.page(query));
    }

    /**
     * 分页查询当前用户已发布的文章（按登录用户隔离，取登录 ID 而非前端传参）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 当前用户已发布文章分页结果
     */
    @Operation(summary = "我的已发布文章（需登录，按用户隔离）")
    @GetMapping("/my")
    @SaCheckLogin
    public R<PageResult<ArticleVO>> my(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                   @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(articleService.pageMyArticles(StpUtil.getLoginIdAsLong(), page, size));
    }

    /**
     * 分页查询当前用户的草稿文章。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 当前用户草稿分页结果
     */
    @Operation(summary = "我的草稿（需登录，按用户隔离）")
    @GetMapping("/my/drafts")
    @SaCheckLogin
    public R<PageResult<ArticleVO>> myDrafts(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                         @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(articleService.pageMyDrafts(StpUtil.getLoginIdAsLong(), page, size));
    }

    /**
     * 分页查询当前用户点赞过的文章。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 点赞文章分页结果
     */
    @Operation(summary = "我点赞的文章（需登录，按用户隔离）")
    @GetMapping("/my/likes")
    @SaCheckLogin
    public R<PageResult<ArticleVO>> myLikes(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                        @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(articleService.pageMyLikes(StpUtil.getLoginIdAsLong(), page, size));
    }

    /**
     * 分页查询当前用户收藏过的文章。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 收藏文章分页结果
     */
    @Operation(summary = "我收藏的文章（需登录，按用户隔离）")
    @GetMapping("/my/favorites")
    @SaCheckLogin
    public R<PageResult<ArticleVO>> myFavorites(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(articleService.pageMyFavorites(StpUtil.getLoginIdAsLong(), page, size));
    }

    /**
     * 获取文章数据用于编辑（仅作者本人，含草稿/待审核态）。
     *
     * @param id 文章 ID
     * @return 文章详情 VO（含 Markdown 原文）
     */
    @Operation(summary = "获取文章用于编辑（仅作者，含草稿/待审核）")
    @GetMapping("/my/{id}")
    @SaCheckLogin
    public R<ArticleDetailVO> getForEdit(@PathVariable Long id) {
        return R.ok(articleService.getForEdit(id, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 查询文章整体统计（管理员：总数/发布数/待审核数等）。
     *
     * @return 文章统计 VO
     */
    @Operation(summary = "文章统计（管理员）")
    @GetMapping("/stats")
    @SaCheckLogin
    public R<ArticleStatsVO> stats() {
        return R.ok(articleService.stats());
    }

    /**
     * 分页查询待审核文章（管理员）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 待审核文章分页结果
     */
    @Operation(summary = "待审核文章列表（管理员）")
    @GetMapping("/pending")
    @SaCheckLogin
    public R<PageResult<ArticleVO>> pending(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                        @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(articleService.pagePending(page, size));
    }

    /**
     * 审核文章（管理员操作，3=通过发布 4=驳回）。
     *
     * @param id     文章 ID
     * @param status 审核结果状态
     * @return 统一成功响应（无数据）
     */
    @Operation(summary = "审核文章（管理员，3=通过 4=驳回）")
    @PostMapping("/{id}/audit")
    @SaCheckLogin
    public R<Void> audit(@PathVariable Long id, @RequestParam Integer status) {
        articleService.audit(id, status);
        return R.ok();
    }

    /**
     * 查询文章详情（游客可访问，阅读量 +1，Markdown 原文 + 渲染 HTML）。
     *
     * @param id 文章 ID
     * @return 文章详情 VO
     */
    @Operation(summary = "文章详情（游客可访问，阅读量 +1）")
    @GetMapping("/{id}")
    public R<ArticleDetailVO> detail(@PathVariable Long id) {
        return R.ok(articleService.detail(id));
    }

    /**
     * 发布文章（需登录，status=1 草稿或 3 直接发布，按用户角色决定是否进审核）。
     *
     * @param dto 文章创建入参
     * @return 创建后的文章 VO
     */
    @Operation(summary = "发布文章（需登录，status=1草稿/3发布）")
    @PostMapping
    @SaCheckLogin
    public R<ArticleVO> create(@RequestBody @Valid ArticleCreateDTO dto) {
        return R.ok(articleService.create(dto, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 更新文章（仅作者本人可操作，全量更新）。
     *
     * @param id  文章 ID
     * @param dto 文章更新入参
     * @return 更新后的文章 VO
     */
    @Operation(summary = "更新文章（仅作者）")
    @PutMapping("/{id}")
    @SaCheckLogin
    public R<ArticleVO> update(@PathVariable Long id, @RequestBody @Valid ArticleCreateDTO dto) {
        return R.ok(articleService.update(dto, id, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 删除文章（软删除，仅作者本人或管理员）。
     *
     * @param id 文章 ID
     * @return 统一成功响应（无数据）
     */
    @Operation(summary = "删除文章（仅作者或管理员，软删除）")
    @DeleteMapping("/{id}")
    @SaCheckLogin
    public R<Void> delete(@PathVariable Long id) {
        articleService.delete(id, StpUtil.getLoginIdAsLong());
        return R.ok();
    }

    /**
     * 点赞/取消点赞（幂等：已点赞则取消）。
     *
     * @param id 文章 ID
     * @return true=已点赞 false=已取消
     */
    @Operation(summary = "点赞/取消点赞（需登录，幂等）")
    @PostMapping("/{id}/like")
    @SaCheckLogin
    public R<Boolean> like(@PathVariable Long id) {
        return R.ok(articleService.toggleLike(id, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 收藏/取消收藏（幂等：已收藏则取消）。
     *
     * @param id 文章 ID
     * @return true=已收藏 false=已取消
     */
    @Operation(summary = "收藏/取消收藏（需登录，幂等）")
    @PostMapping("/{id}/favorite")
    @SaCheckLogin
    public R<Boolean> favorite(@PathVariable Long id) {
        return R.ok(articleService.toggleFavorite(id, StpUtil.getLoginIdAsLong()));
    }
}
