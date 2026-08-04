package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Dept;
import com.xytang.system.mapper.DeptMapper;
import com.xytang.system.service.DeptService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门服务实现：部门树查询与部门 CRUD。
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    /**
     * 查询部门列表（当前为占位实现，返回全量平铺数据，树形组装待后续补充）。
     *
     * @return 部门列表
     */
    @Override
    public List<Dept> tree() {
        // 占位实现：直接返回全量部门平铺数据，树形组装待后续补充
        return list();
    }
}
