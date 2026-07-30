package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.system.entity.Dept;

import java.util.List;

public interface DeptService extends IService<Dept> {

    List<Dept> tree();
}
