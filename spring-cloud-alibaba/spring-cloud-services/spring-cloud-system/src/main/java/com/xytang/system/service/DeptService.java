package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Dept;

import java.util.List;

/**
 * 部门服务。
 */
public interface DeptService extends IService<Dept> {

    /**
     * 查询部门列表（当前返回全量平铺数据，树形组装待实现）。
     *
     * @return 部门列表
     */
    List<Dept> tree();
}
