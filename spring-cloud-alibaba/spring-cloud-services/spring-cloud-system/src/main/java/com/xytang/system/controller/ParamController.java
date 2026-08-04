package com.xytang.system.controller;

import com.xytang.common.core.response.R;
import com.xytang.system.entity.Param;
import com.xytang.system.service.ParamService;
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

/**
 * 系统参数控制器。
 */
@Tag(name = "参数管理")
@RestController
@RequestMapping("/params")
@RequiredArgsConstructor
public class ParamController {

    private final ParamService paramService;

    /**
     * 按参数 key 查询参数值。
     *
     * @param key 参数键
     * @return 参数值
     */
    @Operation(summary = "按 key 查参数")
    @GetMapping("/key/{key}")
    public R<String> get(@PathVariable String key) {
        return R.ok(paramService.getValue(key));
    }

    /**
     * 新增系统参数。
     *
     * @param param 参数实体（key/value/备注）
     * @return 是否保存成功
     */
    @Operation(summary = "新增参数")
    @PostMapping
    public R<Boolean> create(@RequestBody Param param) {
        return R.ok(paramService.save(param));
    }

    /**
     * 修改系统参数。
     *
     * @param id    参数 ID
     * @param param 待更新的参数字段
     * @return 是否更新成功
     */
    @Operation(summary = "修改参数")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Param param) {
        param.setId(id);
        return R.ok(paramService.updateById(param));
    }

    /**
     * 删除系统参数。
     *
     * @param id 参数 ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除参数")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(paramService.removeById(id));
    }
}
