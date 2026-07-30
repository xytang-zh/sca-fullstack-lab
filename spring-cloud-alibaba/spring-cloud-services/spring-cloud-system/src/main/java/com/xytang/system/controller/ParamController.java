package com.xytang.system.controller;

import com.xytang.common.core.response.R;
import com.xytang.system.entity.Param;
import com.xytang.system.service.ParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "参数管理")
@RestController
@RequestMapping("/params")
@RequiredArgsConstructor
public class ParamController {

    private final ParamService paramService;

    @Operation(summary = "按 key 查参数")
    @GetMapping("/key/{key}")
    public R<String> get(@PathVariable String key) {
        return R.ok(paramService.getValue(key));
    }

    @Operation(summary = "新增参数")
    @PostMapping
    public R<Boolean> create(@RequestBody Param param) {
        return R.ok(paramService.save(param));
    }

    @Operation(summary = "修改参数")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Param param) {
        param.setId(id);
        return R.ok(paramService.updateById(param));
    }

    @Operation(summary = "删除参数")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(paramService.removeById(id));
    }
}
