package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Dict;
import com.xytang.system.mapper.DictMapper;
import com.xytang.system.service.DictService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Override
    public List<Dict> listByType(String dictType) {
        return list(new LambdaQueryWrapper<Dict>()
            .eq(Dict::getDictType, dictType)
            .orderByAsc(Dict::getSort));
    }
}
