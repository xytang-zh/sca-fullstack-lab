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

    /**
     * 分页查询文章已审核评论（游客可访问；登录则额外标注当前用户点赞态）。
     *
     * @param articleId 文章 ID
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @return 评论分页结果
     */
    @Operation(summary = "文章评论分页（游客可访问，仅已审核）")
    @GetMapping("/articles/{articleId}")
    public R<PageResult<CommentVO>> page(@PathVariable Long articleId,
                                     @RequestParam(defaultValue = "1") @Min(1) Integer page,
                                     @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        // 游客可访问，未登录时 currentUserId 传 null，服务层据此跳过点赞态标注
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return R.ok(commentService.pageByArticle(articleId, page, size, currentUserId));
    }

    /**
     * 发表一级评论（需登录，默认进待审核；记录 IP/UA 用于反垃圾溯源）。
     *
     * @param dto     评论入参（文章 ID/内容/昵称/头像）
     * @param request 请求对象（解析客户端真实 IP 与 UA）
     * @return 评论 VO
     */
    @Operation(summary = "发表评论（需登录）")
    @PostMapping
    @SaCheckLogin
    public R<CommentVO> create(@RequestBody @Valid CommentCreateDTO dto, HttpServletRequest request) {
        return R.ok(commentService.create(dto, StpUtil.getLoginIdAsLong(),
                resolveIp(request), request.getHeader("User-Agent")));
    }

    /**
     * 回复评论（需登录，二级嵌套，默认进待审核）。
     *
     * @param id      被回复的评论 ID（指向一级评论）
     * @param dto     回复入参（文章 ID/父评论/被回复者昵称/内容）
     * @param request 请求对象（解析客户端真实 IP 与 UA）
     * @return 评论 VO
     */
    @Operation(summary = "回复评论（需登录，二级嵌套）")
    @PostMapping("/{id}/reply")
    @SaCheckLogin
    public R<CommentVO> reply(@PathVariable Long id, @RequestBody @Valid CommentReplyDTO dto,
                              HttpServletRequest request) {
        return R.ok(commentService.reply(id, dto, StpUtil.getLoginIdAsLong(),
                resolveIp(request), request.getHeader("User-Agent")));
    }

    /**
     * 评论点赞/取消（需登录，幂等）。
     *
     * @param id 评论 ID
     * @return true=已点赞 false=已取消
     */
    @Operation(summary = "评论点赞/取消（需登录，幂等）")
    @PostMapping("/{id}/like")
    @SaCheckLogin
    public R<Boolean> like(@PathVariable Long id) {
        return R.ok(commentService.toggleLike(id, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 分页查询当前用户的评论（按用户隔离，含所属文章标题）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 我的评论分页结果
     */
    @Operation(summary = "我的评论（需登录，按用户隔离）")
    @GetMapping("/my")
    @SaCheckLogin
    public R<PageResult<CommentMyVO>> my(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                     @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(commentService.pageMyComments(StpUtil.getLoginIdAsLong(), page, size));
    }

    /**
     * 分页查询待审核评论（管理员审核队列）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 待审核评论分页结果
     */
    @Operation(summary = "待审核评论列表（管理员）")
    @GetMapping("/pending")
    @SaCheckLogin
    public R<PageResult<CommentVO>> pending(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                        @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return R.ok(commentService.pagePending(page, size));
    }

    /**
     * 审核评论（管理员操作，2=通过 3=驳回）。
     *
     * @param id  评论 ID
     * @param dto 审核入参（目标状态）
     * @return 统一成功响应（无数据）
     */
    @Operation(summary = "审核评论（管理员，2=通过 3=驳回）")
    @PostMapping("/{id}/audit")
    @SaCheckLogin
    public R<Void> audit(@PathVariable Long id, @RequestBody @Valid CommentAuditDTO dto) {
        // 安全约束：审核决定评论是否对外可见，属敏感操作；骨架阶段仅校验登录态，ADMIN 角色校验待接入
        commentService.audit(id, dto);
        return R.ok();
    }

    // 解析客户端真实 IP：优先取代理透传的 X-Forwarded-For，其次 X-Real-IP，最后回退到请求的远程地址
    // （评论服务记录 IP 用于反垃圾溯源，网关层已剥离 /api/comments 前缀）
    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            // 多级代理时 X-Forwarded-For 形如 "client, proxy1, proxy2"，取最左侧的原始客户端 IP
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}