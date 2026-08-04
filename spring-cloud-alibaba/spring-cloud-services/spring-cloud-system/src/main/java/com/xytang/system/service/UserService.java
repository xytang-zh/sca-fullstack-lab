package com.xytang.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xytang.common.core.response.PageResult;
import com.xytang.system.dto.UserCreateDTO;
import com.xytang.system.dto.UserPageQuery;
import com.xytang.system.dto.UserUpdateDTO;
import com.xytang.system.entity.User;
import com.xytang.system.vo.UserVO;

/**
 * 用户服务。
 */
public interface UserService extends IService<User> {

    PageResult<UserVO> page(UserPageQuery query);

    UserVO getDetail(Long id);

    Long create(UserCreateDTO dto);

    void update(UserUpdateDTO dto);

    void delete(Long id);

    void resetPassword(Long id, String newPwd);

    void changeStatus(Long id, Integer status);

    User loadUserByUsername(String username);
}
