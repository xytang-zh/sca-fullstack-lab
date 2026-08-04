package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Param;
import com.xytang.system.mapper.ParamMapper;
import com.xytang.system.service.ParamService;
import org.springframework.stereotype.Service;

/**
 * 系统参数服务实现：参数值查询与缓存刷新通知。
 */
@Service
public class ParamServiceImpl extends ServiceImpl<ParamMapper, Param> implements ParamService {

    /**
     * 按参数 key 查询参数值。
     *
     * @param key 参数键
     * @return 参数值；不存在返回 null
     */
    @Override
    public String getValue(String key) {
        // 按参数键精确查询；LIMIT 1 兜底，避免 param_key 出现重复数据时抛异常
        Param param = getOne(new LambdaQueryWrapper<Param>().eq(Param::getParamKey, key).last("LIMIT 1"));
        return param == null ? null : param.getParamValue();
    }

    /**
     * 参数变更后通知所有实例刷新本地缓存。
     *
     * @param key 发生变更的参数键
     */
    @Override
    public void refresh(String key) {
        // TODO(T018): 发 MQ 事件 sys.param.changed 通知所有实例清缓存
    }
}
