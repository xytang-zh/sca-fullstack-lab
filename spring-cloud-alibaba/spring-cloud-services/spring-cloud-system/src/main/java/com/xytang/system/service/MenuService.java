package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Menu;

import java.util.List;

/**
 * 菜单服务。
 */
public interface MenuService extends IService<Menu> {

    /**
     * 查询菜单列表（当前返回全量平铺数据，树形组装待实现）。
     *
     * @return 菜单列表
     */
    List<Menu> tree();
}
