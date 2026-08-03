package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Role;
import com.xytang.system.mapper.RoleMapper;
import com.xytang.system.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现。
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
