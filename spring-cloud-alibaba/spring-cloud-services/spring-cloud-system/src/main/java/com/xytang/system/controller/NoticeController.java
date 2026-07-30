package com.xytang.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xytang.common.core.response.PageVO;
import com.xytang.common.core.response.R;
import com.xytang.system.entity.Notice;
import com.xytang.system.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知公告管理")
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "通知分页")
    @GetMapping
    public R<PageVO<Notice>> page(@RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "10") int pageSize) {
        Page<Notice> page = noticeService.page(new Page<>(pageNum, pageSize));
        return R.ok(PageVO.of(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    @Operation(summary = "新增通知")
    @PostMapping
    public R<Boolean> create(@RequestBody Notice notice) {
        return R.ok(noticeService.save(notice));
    }

    @Operation(summary = "修改通知")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Notice notice) {
        notice.setId(id);
        return R.ok(noticeService.updateById(notice));
    }

    @Operation(summary = "发布通知")
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return R.ok();
    }

    @Operation(summary = "撤回通知")
    @PostMapping("/{id}/revoke")
    public R<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return R.ok();
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(noticeService.removeById(id));
    }
}
