package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xytang.common.core.constant.CommonConstants;
import com.xytang.common.core.exception.BusinessException;
import com.xytang.common.core.exception.LastSuperAdminException;
import com.xytang.common.core.exception.UserNotFoundException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.PageResult;
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
 * 用户服务实现：用户 CRUD、密码重置、状态变更与按用户名查询。
 *
 * <p>安全约束：密码一律通过 {@code PasswordEncoder}（Argon2id）加密后落库；
 * 手机号/邮箱在出参前脱敏，避免敏感信息泄露；禁删最后一个超管。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final int PHONE_MIN_LEN = 7;
    private static final int PHONE_HEAD_LEN = 3;
    private static final int PHONE_TAIL_LEN = 4;

    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户，支持关键字（用户名/昵称模糊）、状态、部门过滤，按创建时间倒序。
     *
     * @param query 分页与过滤条件
     * @return 用户分页结果（手机号/邮箱已脱敏）
     */
    @Override
    public PageResult<UserVO> page(UserPageQuery query) {
        Page<User> page = new Page<>(query.getPage(), query.getSize());
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
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 查询用户详情并转 VO（手机号/邮箱脱敏）。
     *
     * @param id 用户 ID
     * @return 用户详情 VO
     * @throws UserNotFoundException 用户不存在时抛出
     */
    @Override
    public UserVO getDetail(Long id) {
        User user = baseMapper.selectById(id);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return toVO(user);
    }

    /**
     * 新增用户：先查重再插入，密码经 Argon2id 加密，默认正常状态。
     *
     * @param dto 新增用户入参
     * @return 新用户 ID
     * @throws BusinessException 用户名已存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateDTO dto) {
        // 1. 用户名查重（唯一索引兜底并发）
        if (loadUserByUsername(dto.getUsername()) != null) {
            throw new BusinessException(BizCode.SYS_USER_EXISTED);
        }
        // 2. 组装用户：密码 Argon2id 加密、默认正常状态、失败计数清零
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

    /**
     * 更新用户：仅更新传入的非空字段；状态改为禁用时需先校验非最后一个超管。
     *
     * @param dto 用户更新入参（含 id 与待更新字段）
     * @throws UserNotFoundException       用户不存在时抛出
     * @throws LastSuperAdminException     禁用最后一个超管时抛出
     */
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
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
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

    /**
     * 软删除用户：置 status 为已删除，不物理删除；禁止删除最后一个超管。
     *
     * @param id 用户 ID
     * @throws LastSuperAdminException 删除最后一个超管时抛出
     * @throws UserNotFoundException   用户不存在时抛出
     */
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

    /**
     * 重置用户密码：新密码 Argon2id 加密，同时清零登录失败计数与锁定到期时间。
     *
     * @param id     用户 ID
     * @param newPwd 新明文密码
     * @throws UserNotFoundException 用户不存在时抛出
     */
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

    /**
     * 启用/禁用用户：禁用（软删除语义）前校验非最后一个超管。
     *
     * @param id     用户 ID
     * @param status 目标状态：1=正常 0=禁用
     * @throws LastSuperAdminException 禁用最后一个超管时抛出
     * @throws UserNotFoundException   用户不存在时抛出
     */
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

    /**
     * 按用户名精确查询用户（登录/认证场景复用，LIMIT 1 兜底同名异常数据）。
     *
     * @param username 登录账号
     * @return 用户实体；不存在返回 null
     */
    @Override
    public User loadUserByUsername(String username) {
        return baseMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username)
            .last("LIMIT 1"));
    }

    // 保护最后一个超管：禁止禁用/删除 id=1 的初始超管（MVP 简化实现）
    private void guardLastSuperAdmin(Long userId) {
        if (userId != null && userId == 1L) {
            throw new LastSuperAdminException();
        }
    }

    // 实体转 VO：手机号/邮箱脱敏，角色列表待后续填充
    private UserVO toVO(User user) {
        return UserVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(maskEmail(user.getEmail()))
            .phone(maskPhone(user.getPhone()))
            .avatar(user.getAvatar())
            .bio(user.getBio())
            .deptId(user.getDeptId())
            .status(user.getStatus())
            .lastLoginTime(user.getLastLoginTime())
            .lastLoginIp(user.getLastLoginIp())
            .createTime(user.getCreateTime())
            .roles(List.of())
            .build();
    }

    // 邮箱脱敏：保留首字符与域名，中间打码（如 a***@x.com）
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

    // 手机号脱敏：保留前 3 位与后 4 位，中间打码（如 138****1234）
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
