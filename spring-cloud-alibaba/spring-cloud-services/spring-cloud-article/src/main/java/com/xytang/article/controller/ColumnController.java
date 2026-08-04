package com.xytang.article.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xytang.article.dto.ColumnCreateDTO;
import com.xytang.article.service.ColumnService;
import com.xytang.article.vo.ColumnVO;
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
 * 博客专栏控制器：列表游客可读，创建/编辑/删除/订阅需登录。
 */
@Tag(name = "博客专栏")
@RestController
@RequestMapping("/columns")
@RequiredArgsConstructor
@Validated
public class ColumnController {

    private final ColumnService columnService;

    @Operation(summary = "专栏分页列表（游客可访问，?userId= 按作者过滤）")
    @GetMapping
    public R<PageResult<ColumnVO>> page(@RequestParam(required = false) Long userId,
                                    @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                    @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return R.ok(columnService.page(userId, currentUserId, page, size));
    }

    @Operation(summary = "我的专栏（需登录）")
    @GetMapping("/my")
    @SaCheckLogin
    public R<PageResult<ColumnVO>> my(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                  @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(columnService.listMyColumns(StpUtil.getLoginIdAsLong(), page, size));
    }

    @Operation(summary = "我订阅的专栏（需登录）")
    @GetMapping("/my/subscriptions")
    @SaCheckLogin
    public R<PageResult<ColumnVO>> mySubscriptions(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                               @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(columnService.listMySubscriptions(StpUtil.getLoginIdAsLong(), page, size));
    }

    @Operation(summary = "创建专栏（需登录）")
    @PostMapping
    @SaCheckLogin
    public R<ColumnVO> create(@RequestBody @Valid ColumnCreateDTO dto) {
        return R.ok(columnService.create(dto, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "编辑专栏（仅作者）")
    @PutMapping("/{id}")
    @SaCheckLogin
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid ColumnCreateDTO dto) {
        columnService.update(id, dto, StpUtil.getLoginIdAsLong());
        return R.ok();
    }

    @Operation(summary = "删除专栏（仅作者）")
    @DeleteMapping("/{id}")
    @SaCheckLogin
    public R<Void> delete(@PathVariable Long id) {
        columnService.delete(id, StpUtil.getLoginIdAsLong());
        return R.ok();
    }

    @Operation(summary = "订阅/取消订阅专栏（需登录，幂等）")
    @PostMapping("/{id}/subscribe")
    @SaCheckLogin
    public R<Boolean> subscribe(@PathVariable Long id) {
        return R.ok(columnService.toggleSubscribe(id, StpUtil.getLoginIdAsLong()));
    }
}