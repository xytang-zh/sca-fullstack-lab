package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Dict;

import java.util.List;

public interface DictService extends IService<Dict> {

    List<Dict> listByType(String dictType);
}
