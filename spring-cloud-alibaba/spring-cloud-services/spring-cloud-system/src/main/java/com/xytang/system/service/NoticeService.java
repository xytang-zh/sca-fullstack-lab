package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Notice;

public interface NoticeService extends IService<Notice> {

    void publish(Long id);

    void revoke(Long id);
}
