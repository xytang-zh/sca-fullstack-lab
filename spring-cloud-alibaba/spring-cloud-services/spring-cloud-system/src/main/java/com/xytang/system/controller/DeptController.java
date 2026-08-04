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

    /**
     * 查询部门树（按父子层级组装）。
     *
     * @return 部门实体树
     */
    @Operation(summary = "部门树")
    @GetMapping("/tree")
    public R<List<Dept>> tree() {
        return R.ok(deptService.tree());
    }

    /**
     * 新增部门。
     *
     * @param dept 部门实体（名称/父级/排序）
     * @return 是否保存成功
     */
    @Operation(summary = "新增部门")
    @PostMapping
    public R<Boolean> create(@RequestBody Dept dept) {
        return R.ok(deptService.save(dept));
    }

    /**
     * 修改部门。
     *
     * @param id   部门 ID
     * @param dept 待更新的部门字段
     * @return 是否更新成功
     */
    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Dept dept) {
        dept.setId(id);
        return R.ok(deptService.updateById(dept));
    }

    /**
     * 删除部门。
     *
     * @param id 部门 ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(deptService.removeById(id));
    }
}
