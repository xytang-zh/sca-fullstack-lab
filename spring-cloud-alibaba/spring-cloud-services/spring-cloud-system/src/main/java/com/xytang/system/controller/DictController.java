package com.xytang.system.controller;

import com.xytang.common.core.response.R;
import com.xytang.system.entity.Dict;
import com.xytang.system.service.DictService;
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
 * 字典管理控制器。
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /**
     * 按字典类型查询字典数据（常用于前端下拉选项，如性别/状态）。
     *
     * @param type 字典类型编码
     * @return 该类型下的字典项列表
     */
    @Operation(summary = "按类型查字典数据")
    @GetMapping("/data/{type}")
    public R<List<Dict>> listByType(@PathVariable String type) {
        return R.ok(dictService.listByType(type));
    }

    /**
     * 新增字典项。
     *
     * @param dict 字典实体（类型/键/值/排序）
     * @return 是否保存成功
     */
    @Operation(summary = "新增字典")
    @PostMapping
    public R<Boolean> create(@RequestBody Dict dict) {
        return R.ok(dictService.save(dict));
    }

    /**
     * 修改字典项。
     *
     * @param id   字典 ID
     * @param dict 待更新的字典字段
     * @return 是否更新成功
     */
    @Operation(summary = "修改字典")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Dict dict) {
        dict.setId(id);
        return R.ok(dictService.updateById(dict));
    }

    /**
     * 删除字典项。
     *
     * @param id 字典 ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除字典")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(dictService.removeById(id));
    }
}
