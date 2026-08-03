package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Param;
import com.xytang.system.mapper.ParamMapper;
import com.xytang.system.service.ParamService;
import org.springframework.stereotype.Service;

/**
 * 系统参数服务实现。
 */
@Service
public class ParamServiceImpl extends ServiceImpl<ParamMapper, Param> implements ParamService {

    @Override
    public String getValue(String key) {
        Param param = getOne(new LambdaQueryWrapper<Param>().eq(Param::getParamKey, key).last("LIMIT 1"));
        return param == null ? null : param.getParamValue();
    }

    @Override
    public void refresh(String key) {
        // TODO(T018): 发 MQ 事件 sys.param.changed 通知所有实例清缓存
    }
}
