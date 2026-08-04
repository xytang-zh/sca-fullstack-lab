package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Param;

/**
 * 系统参数服务。
 */
public interface ParamService extends IService<Param> {

    /**
     * 按参数 key 查询参数值。
     *
     * @param key 参数键
     * @return 参数值；不存在返回 null
     */
    String getValue(String key);

    /**
     * 参数变更后刷新所有实例的本地缓存。
     *
     * @param key 发生变更的参数键
     */
    void refresh(String key);
}
