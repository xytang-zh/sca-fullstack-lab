package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Dict;

import java.util.List;

/**
 * 字典服务。
 */
public interface DictService extends IService<Dict> {

    List<Dict> listByType(String dictType);
}
