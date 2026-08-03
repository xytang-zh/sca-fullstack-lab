package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Param;

/**
 * 系统参数服务。
 */
public interface ParamService extends IService<Param> {

    String getValue(String key);

    void refresh(String key);
}
