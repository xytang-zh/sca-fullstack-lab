package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.system.entity.Role;
import com.xytang.system.mapper.RoleMapper;
import com.xytang.system.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现：角色 CRUD 与菜单分配（继承 MyBatis-Plus IService 默认能力）。
 *
 * <p>博客三角色 USER/AUTHOR/ADMIN 由角色编码区分，权限标识供 Sa-Token StpInterface 读取。</p>
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
