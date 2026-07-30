package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Menu;
import com.xytang.system.mapper.MenuMapper;
import com.xytang.system.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<Menu> tree() {
        // TODO: 转换为树形结构返回，含 children
        return list();
    }
}
