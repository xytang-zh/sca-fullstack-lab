package com.xytang.system.controller;

import com.xytang.common.core.response.R;
import com.xytang.system.entity.Menu;
import com.xytang.system.service.MenuService;
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
 * 菜单管理控制器。
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 查询菜单树（按父子层级组装，供前端渲染导航/权限配置）。
     *
     * @return 菜单实体树
     */
    @Operation(summary = "菜单树")
    @GetMapping("/tree")
    public R<List<Menu>> tree() {
        return R.ok(menuService.tree());
    }

    /**
     * 新增菜单。
     *
     * @param menu 菜单实体（父级/路由/权限标识）
     * @return 是否保存成功
     */
    @Operation(summary = "新增菜单")
    @PostMapping
    public R<Boolean> create(@RequestBody Menu menu) {
        return R.ok(menuService.save(menu));
    }

    /**
     * 修改菜单。
     *
     * @param id   菜单 ID
     * @param menu 待更新的菜单字段
     * @return 是否更新成功
     */
    @Operation(summary = "修改菜单")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        return R.ok(menuService.updateById(menu));
    }

    /**
     * 删除菜单。
     *
     * @param id 菜单 ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(menuService.removeById(id));
    }
}
