package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Notice;
import com.xytang.system.mapper.NoticeMapper;
import com.xytang.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public void publish(Long id) {
        Notice n = getById(id);
        if (n == null) return;
        n.setStatus(2);
        n.setPublishTime(LocalDateTime.now());
        updateById(n);
    }

    @Override
    public void revoke(Long id) {
        Notice n = getById(id);
        if (n == null) return;
        n.setStatus(3);
        updateById(n);
    }
}
