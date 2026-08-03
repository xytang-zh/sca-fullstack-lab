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

    @Operation(summary = "角色列表")
    @GetMapping
    public R<List<Role>> list() {
        return R.ok(roleService.list());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public R<Role> get(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Boolean> create(@RequestBody Role role) {
        return R.ok(roleService.save(role));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return R.ok(roleService.updateById(role));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(roleService.removeById(id));
    }
}
