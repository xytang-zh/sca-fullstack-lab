package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Menu;

import java.util.List;

public interface MenuService extends IService<Menu> {

    List<Menu> tree();
}
