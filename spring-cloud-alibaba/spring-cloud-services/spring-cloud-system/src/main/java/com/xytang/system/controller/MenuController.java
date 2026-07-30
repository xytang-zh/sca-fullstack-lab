package com.xytang.system.controller;

import com.xytang.common.core.response.R;
import com.xytang.system.entity.Menu;
import com.xytang.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "菜单树")
    @GetMapping("/tree")
    public R<List<Menu>> tree() {
        return R.ok(menuService.tree());
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    public R<Boolean> create(@RequestBody Menu menu) {
        return R.ok(menuService.save(menu));
    }

    @Operation(summary = "修改菜单")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        return R.ok(menuService.updateById(menu));
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(menuService.removeById(id));
    }
}
