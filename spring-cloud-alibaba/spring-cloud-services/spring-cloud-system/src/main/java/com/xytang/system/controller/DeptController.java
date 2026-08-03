package com.xytang.system.controller;

import com.xytang.common.core.response.R;
import com.xytang.system.entity.Dept;
import com.xytang.system.service.DeptService;
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
 * 部门管理控制器。
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部门树")
    @GetMapping("/tree")
    public R<List<Dept>> tree() {
        return R.ok(deptService.tree());
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public R<Boolean> create(@RequestBody Dept dept) {
        return R.ok(deptService.save(dept));
    }

    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Dept dept) {
        dept.setId(id);
        return R.ok(deptService.updateById(dept));
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(deptService.removeById(id));
    }
}
