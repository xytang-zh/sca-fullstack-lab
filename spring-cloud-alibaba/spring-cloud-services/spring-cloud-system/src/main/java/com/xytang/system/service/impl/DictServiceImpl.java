package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Dict;
import com.xytang.system.mapper.DictMapper;
import com.xytang.system.service.DictService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务实现：按类型查询字典数据与字典 CRUD。
 */
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    /**
     * 按字典类型查询字典数据，按 sort 升序返回。
     *
     * @param dictType 字典类型编码
     * @return 该类型下的字典项列表
     */
    @Override
    public List<Dict> listByType(String dictType) {
        // 按字典类型过滤并按 sort 升序返回，保证前端下拉选项顺序稳定
        return list(new LambdaQueryWrapper<Dict>()
            .eq(Dict::getDictType, dictType)
            .orderByAsc(Dict::getSort));
    }
}
