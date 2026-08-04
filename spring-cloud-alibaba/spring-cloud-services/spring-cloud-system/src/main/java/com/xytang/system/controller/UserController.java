package com.xytang.system.controller;

import com.xytang.common.core.response.PageResult;
import com.xytang.common.core.response.R;
import com.xytang.system.dto.UserCreateDTO;
import com.xytang.system.dto.UserPageQuery;
import com.xytang.system.dto.UserUpdateDTO;
import com.xytang.system.service.UserService;
import com.xytang.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器。
 */
@Tag(name = "用户管理", description = "用户 CRUD + 重置密码 + 状态变更")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表。
     *
     * @param query 分页查询条件（关键字/状态/部门）
     * @return 用户分页结果（手机号/邮箱已脱敏）
     */
    @Operation(summary = "用户分页查询")
    @GetMapping
    public R<PageResult<UserVO>> page(@Validated UserPageQuery query) {
        return R.ok(userService.page(query));
    }

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户详情 VO
     * @throws UserNotFoundException 用户不存在时抛出
     */
    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public R<UserVO> get(@PathVariable Long id) {
        return R.ok(userService.getDetail(id));
    }

    /**
     * 新增用户。
     *
     * @param dto 新增用户入参（账号/密码/昵称/邮箱/手机号/部门）
     * @return 新用户 ID
     * @throws BusinessException 用户名已存在时抛出
     */
    @Operation(summary = "新增用户")
    @PostMapping
    public R<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userService.create(dto));
    }

    /**
     * 修改用户（部分字段更新）。
     *
     * @param id  用户 ID
     * @param dto 待更新字段（昵称/邮箱/手机号/状态等）
     * @return 统一成功响应（无数据）
     * @throws UserNotFoundException 用户不存在时抛出
     */
    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        dto.setId(id);
        userService.update(dto);
        return R.ok();
    }

    /**
     * 重置用户密码（管理员操作，密码使用 Argon2id 加密后落库）。
     *
     * @param id          用户 ID
     * @param newPassword 新明文密码
     * @return 统一成功响应（无数据）
     * @throws UserNotFoundException 用户不存在时抛出
     */
    @Operation(summary = "重置密码")
    @PatchMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return R.ok();
    }

    /**
     * 启用/禁用用户。
     *
     * @param id     用户 ID
     * @param status 目标状态：1=正常 0=禁用
     * @return 统一成功响应（无数据）
     * @throws LastSuperAdminException 禁用最后一个超管时抛出
     */
    @Operation(summary = "启用/禁用用户")
    @PatchMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return R.ok();
    }

    /**
     * 删除用户（软删除：置 status 为已删，不物理删除）。
     *
     * @param id 用户 ID
     * @return 统一成功响应（无数据）
     * @throws LastSuperAdminException 删除最后一个超管时抛出
     */
    @Operation(summary = "删除用户（软删除）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }
}
