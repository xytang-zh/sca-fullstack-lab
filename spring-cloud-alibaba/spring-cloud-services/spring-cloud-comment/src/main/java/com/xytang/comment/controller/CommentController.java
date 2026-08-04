package com.xytang.comment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xytang.comment.dto.CommentAuditDTO;
import com.xytang.comment.dto.CommentCreateDTO;
import com.xytang.comment.dto.CommentReplyDTO;
import com.xytang.comment.service.CommentService;
import com.xytang.comment.vo.CommentMyVO;
import com.xytang.comment.vo.CommentVO;
import com.xytang.common.core.response.PageResult;
import com.xytang.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 博客评论控制器：列表游客可读，发表/回复/点赞需登录，审核需管理员。
 */
@Tag(name = "博客评论")
@RestController
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "文章评论分页（游客可访问，仅已审核）")
    @GetMapping("/articles/{articleId}")
    public R<PageResult<CommentVO>> page(@PathVariable Long articleId,
                                     @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                     @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return R.ok(commentService.pageByArticle(articleId, page, size, currentUserId));
    }

    @Operation(summary = "发表评论（需登录）")
    @PostMapping
    @SaCheckLogin
    public R<CommentVO> create(@RequestBody @Valid CommentCreateDTO dto, HttpServletRequest request) {
        return R.ok(commentService.create(dto, StpUtil.getLoginIdAsLong(),
                resolveIp(request), request.getHeader("User-Agent")));
    }

    @Operation(summary = "回复评论（需登录，二级嵌套）")
    @PostMapping("/{id}/reply")
    @SaCheckLogin
    public R<CommentVO> reply(@PathVariable Long id, @RequestBody @Valid CommentReplyDTO dto,
                              HttpServletRequest request) {
        return R.ok(commentService.reply(id, dto, StpUtil.getLoginIdAsLong(),
                resolveIp(request), request.getHeader("User-Agent")));
    }

    @Operation(summary = "评论点赞/取消（需登录，幂等）")
    @PostMapping("/{id}/like")
    @SaCheckLogin
    public R<Boolean> like(@PathVariable Long id) {
        return R.ok(commentService.toggleLike(id, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "我的评论（需登录，按用户隔离）")
    @GetMapping("/my")
    @SaCheckLogin
    public R<PageResult<CommentMyVO>> my(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                     @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(commentService.pageMyComments(StpUtil.getLoginIdAsLong(), page, size));
    }

    @Operation(summary = "待审核评论列表（管理员）")
    @GetMapping("/pending")
    @SaCheckLogin
    public R<PageResult<CommentVO>> pending(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                        @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(commentService.pagePending(page, size));
    }

    @Operation(summary = "审核评论（管理员，2=通过 3=驳回）")
    @PostMapping("/{id}/audit")
    @SaCheckLogin
    public R<Void> audit(@PathVariable Long id, @RequestBody @Valid CommentAuditDTO dto) {
        commentService.audit(id, dto);
        return R.ok();
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