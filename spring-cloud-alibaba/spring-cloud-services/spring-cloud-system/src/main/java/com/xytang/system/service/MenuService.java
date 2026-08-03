package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Menu;

import java.util.List;

/**
 * 菜单服务。
 */
public interface MenuService extends IService<Menu> {

    List<Menu> tree();
}
