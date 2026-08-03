package com.xytang.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xytang.common.core.response.PageVO;
import com.xytang.common.core.response.R;
import com.xytang.system.service.FollowService;
import com.xytang.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户关注控制器：关注/取关、粉丝/关注列表、我的资料。
 */
@Tag(name = "用户关注")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "关注/取消关注（需登录，幂等）")
    @PostMapping("/{id}/follow")
    @SaCheckLogin
    public R<Boolean> follow(@PathVariable Long id) {
        return R.ok(followService.toggleFollow(id, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "粉丝列表（关注该用户的人）")
    @GetMapping("/{id}/followers")
    @SaCheckLogin
    public R<PageVO<UserVO>> followers(@PathVariable Long id,
                                       @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                       @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return R.ok(followService.pageFollowers(id, pageNum, pageSize));
    }

    @Operation(summary = "关注列表（该用户关注的人）")
    @GetMapping("/{id}/following")
    @SaCheckLogin
    public R<PageVO<UserVO>> following(@PathVariable Long id,
                                       @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
                                       @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return R.ok(followService.pageFollowing(id, pageNum, pageSize));
    }

    @Operation(summary = "我的完整资料（需登录，含 bio/关注数/粉丝数）")
    @GetMapping("/me/mine")
    @SaCheckLogin
    public R<UserVO> mine() {
        return R.ok(followService.getProfile(StpUtil.getLoginIdAsLong()));
    }
}