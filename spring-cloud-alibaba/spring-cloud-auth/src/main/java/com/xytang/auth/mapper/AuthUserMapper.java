package com.xytang.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xytang.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 认证用用户查询 Mapper（CRUD 在 system 服务维护，本模块仅认证场景读写）。
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {

    /**
     * 查询用户角色 code 列表。
     *
     * @param userId 用户 ID
     * @return 角色 code 列表（可能为空）
     */
    @Select("SELECT r.role_code FROM sys_role r "
            + "INNER JOIN sys_user_role ur ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询默认注册角色（USER）ID。
     *
     * @return 角色 ID；未配置返回 null
     */
    @Select("SELECT id FROM sys_role WHERE role_code = 'USER' AND deleted = 0 LIMIT 1")
    Long selectUserRoleId();

    /**
     * 插入用户-角色关联。
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_user_role (user_id, role_id, create_time) "
            + "VALUES (#{userId}, #{roleId}, CURRENT_TIMESTAMP)")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
