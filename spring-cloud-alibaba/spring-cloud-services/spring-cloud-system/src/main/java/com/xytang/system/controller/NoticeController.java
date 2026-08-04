package com.xytang.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xytang.common.core.response.PageResult;
import com.xytang.common.core.response.R;
import com.xytang.system.entity.Notice;
import com.xytang.system.service.NoticeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知公告控制器。
 */
@Tag(name = "通知公告管理")
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 分页查询通知公告。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 通知分页结果
     */
    @Operation(summary = "通知分页")
    @GetMapping
    public R<PageResult<Notice>> page(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        Page<Notice> noticePage = noticeService.page(new Page<>(page, size));
        return R.ok(PageResult.of(noticePage.getRecords(), noticePage.getTotal(),
                (int) noticePage.getCurrent(), (int) noticePage.getSize()));
    }

    /**
     * 新增通知（草稿状态，需发布后可见）。
     *
     * @param notice 通知实体（标题/内容/类型）
     * @return 是否保存成功
     */
    @Operation(summary = "新增通知")
    @PostMapping
    public R<Boolean> create(@RequestBody Notice notice) {
        return R.ok(noticeService.save(notice));
    }

    /**
     * 修改通知。
     *
     * @param id     通知 ID
     * @param notice 待更新的通知字段
     * @return 是否更新成功
     */
    @Operation(summary = "修改通知")
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Notice notice) {
        notice.setId(id);
        return R.ok(noticeService.updateById(notice));
    }

    /**
     * 发布通知（状态置为已发布，用户可见）。
     *
     * @param id 通知 ID
     * @return 统一成功响应（无数据）
     */
    @Operation(summary = "发布通知")
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return R.ok();
    }

    /**
     * 撤回通知（已发布的恢复为草稿）。
     *
     * @param id 通知 ID
     * @return 统一成功响应（无数据）
     */
    @Operation(summary = "撤回通知")
    @PostMapping("/{id}/revoke")
    public R<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return R.ok();
    }

    /**
     * 删除通知。
     *
     * @param id 通知 ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(noticeService.removeById(id));
    }
}
