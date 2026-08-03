package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.common.core.constant.CommonConstants;
import com.xytang.common.core.exception.BusinessException;
import com.xytang.common.core.exception.LastSuperAdminException;
import com.xytang.common.core.exception.UserNotFoundException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.PageVO;
import com.xytang.system.dto.UserCreateDTO;
import com.xytang.system.dto.UserPageQuery;
import com.xytang.system.dto.UserUpdateDTO;
import com.xytang.system.entity.User;
import com.xytang.system.mapper.UserMapper;
import com.xytang.system.service.UserService;
import com.xytang.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final int PHONE_MIN_LEN = 7;
    private static final int PHONE_HEAD_LEN = 3;
    private static final int PHONE_TAIL_LEN = 4;

    private final PasswordEncoder passwordEncoder;

    @Override
    public PageVO<UserVO> page(UserPageQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(User::getUsername, query.getKeyword())
                .or().like(User::getNickname, query.getKeyword());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(User::getDeptId, query.getDeptId());
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = baseMapper.selectPage(page, wrapper);
        List<UserVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public UserVO getDetail(Long id) {
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateDTO dto) {
        if (loadUserByUsername(dto.getUsername()) != null) {
            throw new BusinessException(BizCode.SYS_USER_EXISTED);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setStatus(CommonConstants.STATUS_NORMAL);
        user.setFailCount(0);
        baseMapper.insert(user);
        log.info("[User] create: id={} username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateDTO dto) {
        User user = baseMapper.selectById(dto.getId());
        if (user == null) {
            throw new UserNotFoundException();
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getDeptId() != null) {
            user.setDeptId(dto.getDeptId());
        }
        if (dto.getStatus() != null) {
            if (CommonConstants.STATUS_DELETED == dto.getStatus()) {
                guardLastSuperAdmin(user.getId());
            }
            user.setStatus(dto.getStatus());
        }
        if (dto.getVersion() != null) {
            user.setVersion(dto.getVersion());
        }
        baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        guardLastSuperAdmin(id);
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new UserNotFoundException();
        }
        user.setStatus(CommonConstants.STATUS_DELETED);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPwd) {
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new UserNotFoundException();
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        user.setFailCount(0);
        user.setLockUntil(null);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        if (CommonConstants.STATUS_DELETED == status) {
            guardLastSuperAdmin(id);
        }
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new UserNotFoundException();
        }
        user.setStatus(status);
        baseMapper.updateById(user);
    }

    @Override
    public User loadUserByUsername(String username) {
        return baseMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username)
            .last("LIMIT 1"));
    }

    private void guardLastSuperAdmin(Long userId) {
        // MVP 阶段：检查 username='admin' 或 role_code='super_admin' 的用户是否被禁用/删除
        // 简化实现：禁止禁用/删除 id=1 的初始超管
        if (userId != null && userId == 1L) {
            throw new LastSuperAdminException();
        }
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(maskEmail(user.getEmail()))
            .phone(maskPhone(user.getPhone()))
            .avatar(user.getAvatar())
            .deptId(user.getDeptId())
            .status(user.getStatus())
            .lastLoginTime(user.getLastLoginTime())
            .lastLoginIp(user.getLastLoginIp())
            .createTime(user.getCreateTime())
            .roles(List.of())
            .build();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        String prefix = email.substring(0, at);
        String maskedPrefix = prefix.isEmpty() ? ""
                : prefix.charAt(0) + "*".repeat(Math.max(0, prefix.length() - 1));
        return maskedPrefix + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < PHONE_MIN_LEN) {
            return phone;
        }
        return phone.substring(0, PHONE_HEAD_LEN) + "****"
            + phone.substring(phone.length() - PHONE_TAIL_LEN);
    }

    @SuppressWarnings("unused")
    private Set<String> unusedReference() {
        // 引用 CommonConstants 避免编译期常量折叠
        return Set.of(CommonConstants.SUPER_ADMIN_ROLE_CODE);
    }
}
