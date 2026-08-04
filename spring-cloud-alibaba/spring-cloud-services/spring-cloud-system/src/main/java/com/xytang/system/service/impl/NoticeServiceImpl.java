package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Notice;
import com.xytang.system.mapper.NoticeMapper;
import com.xytang.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知公告服务实现：通知 CRUD、发布/撤回状态流转。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    /** 通知已发布状态 */
    private static final int PUBLISHED = 2;
    /** 通知已撤回状态 */
    private static final int REVOKED = 3;

    /**
     * 发布通知：状态置为已发布并记录发布时间；通知不存在时静默返回保持幂等。
     *
     * @param id 通知 ID
     */
    @Override
    public void publish(Long id) {
        Notice n = getById(id);
        if (n == null) {
            // 通知不存在时静默返回，保持发布操作的幂等语义
            return;
        }
        n.setStatus(PUBLISHED);
        n.setPublishTime(LocalDateTime.now());
        updateById(n);
    }

    /**
     * 撤回通知：状态置为已撤回；通知不存在时静默返回保持幂等。
     *
     * @param id 通知 ID
     */
    @Override
    public void revoke(Long id) {
        Notice n = getById(id);
        if (n == null) {
            // 通知不存在时静默返回，保持撤回操作的幂等语义
            return;
        }
        n.setStatus(REVOKED);
        updateById(n);
    }
}
