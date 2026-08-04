package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Dict;

import java.util.List;

/**
 * 字典服务。
 */
public interface DictService extends IService<Dict> {

    /**
     * 按字典类型查询字典数据（按 sort 升序）。
     *
     * @param dictType 字典类型编码
     * @return 该类型下的字典项列表
     */
    List<Dict> listByType(String dictType);
}
