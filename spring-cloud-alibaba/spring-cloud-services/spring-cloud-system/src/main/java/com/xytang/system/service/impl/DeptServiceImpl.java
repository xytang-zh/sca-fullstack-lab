package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Dept;
import com.xytang.system.mapper.DeptMapper;
import com.xytang.system.service.DeptService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门服务实现。
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    @Override
    public List<Dept> tree() {
        return list();
    }
}
