package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Notice;

/**
 * 通知公告服务。
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 发布通知（状态置为已发布并记录发布时间）。
     *
     * @param id 通知 ID
     */
    void publish(Long id);

    /**
     * 撤回已发布的通知（状态置为已撤回）。
     *
     * @param id 通知 ID
     */
    void revoke(Long id);
}
