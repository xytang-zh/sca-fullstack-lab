package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.common.core.response.PageResult;
import com.xytang.system.dto.UserCreateDTO;
import com.xytang.system.dto.UserPageQuery;
import com.xytang.system.dto.UserUpdateDTO;
import com.xytang.system.entity.User;
import com.xytang.system.vo.UserVO;

/**
 * 用户服务：用户 CRUD、重置密码、启用禁用、按用户名查询。
 */
public interface UserService extends IService<User> {

    /**
     * 分页查询用户（支持关键字/状态/部门过滤）。
     *
     * @param query 分页与过滤条件
     * @return 分页结果（手机号/邮箱已脱敏）
     */
    PageResult<UserVO> page(UserPageQuery query);

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户详情 VO
     * @throws com.xytang.common.core.exception.UserNotFoundException 用户不存在
     */
    UserVO getDetail(Long id);

    /**
     * 新增用户（密码 Argon2id 加密）。
     *
     * @param dto 用户信息
     * @return 新用户 ID
     * @throws com.xytang.common.core.exception.BusinessException 用户名已存在
     */
    Long create(UserCreateDTO dto);

    /**
     * 更新用户（仅更新传入的非空字段）。
     *
     * @param dto 用户信息（含 id）
     * @throws com.xytang.common.core.exception.UserNotFoundException 用户不存在
     */
    void update(UserUpdateDTO dto);

    /**
     * 软删除用户（置状态为已删除），禁止删除最后一个超管。
     *
     * @param id 用户 ID
     * @throws com.xytang.common.core.exception.LastSuperAdminException 删除最后一个超管
     */
    void delete(Long id);

    /**
     * 重置用户密码并清零失败计数与锁定时间。
     *
     * @param id     用户 ID
     * @param newPwd 新密码（明文，内部 Argon2id 加密）
     */
    void resetPassword(Long id, String newPwd);

    /**
     * 启用/禁用用户（禁用操作为软删除语义，同样保护超管）。
     *
     * @param id     用户 ID
     * @param status 目标状态（1=正常 0=禁用）
     */
    void changeStatus(Long id, Integer status);

    /**
     * 按用户名查询用户（登录/认证场景用）。
     *
     * @param username 登录账号
     * @return 用户实体；不存在返回 null
     */
    User loadUserByUsername(String username);
}
