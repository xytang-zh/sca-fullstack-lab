package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Menu;
import com.xytang.system.mapper.MenuMapper;
import com.xytang.system.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务实现：菜单树查询、菜单 CRUD。
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    /**
     * 查询菜单列表（当前返回全量平铺数据，树形组装待实现）。
     *
     * @return 菜单列表
     */
    @Override
    public List<Menu> tree() {
        // TODO: 转换为树形结构返回，含 children
        return list();
    }
}
