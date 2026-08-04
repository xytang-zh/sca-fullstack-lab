package com.xytang.system.controller;

import com.xytang.common.core.response.R;
import com.xytang.system.entity.Role;
import com.xytang.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理控制器。
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 查询全部角色列表（供用户分配角色下拉使用）。
     *
     * @return 角色实体列表
     */
    @Operation(summary = "角色列表")
    @GetMapping
    public R<List<Role>> list() {
        return R.ok(roleService.list());
    }

    /**
     * 查询角色详情。
     *
     * @param id 角色 ID
     * @return 角色实体
     */
    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public R<Role> get(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    /**
     * 新增角色。
     *
     * @param role 角色实体（角色编码/名称/权限标识）
     * @return 是否保存成功
     */
    @Operation(summary = "新增角色")
    @PostMapping
    public R<Boolean> create(@RequestBody Role role) {
        return R.ok(roleService.save(role));
    }

    /**
     * 修改角色。
     *
     * @param id   角色 ID
     * @param role 待更新的角色字段
     * @return 是否更新成功
     */
    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return R.ok(roleService.updateById(role));
    }

    /**
     * 删除角色。
     *
     * @param id 角色 ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(roleService.removeById(id));
    }
}
