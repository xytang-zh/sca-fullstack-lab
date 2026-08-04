package com.xytang.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xytang.common.core.exception.BizException;
import com.xytang.common.core.exception.UserNotFoundException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.PageResult;
import com.xytang.system.entity.Follow;
import com.xytang.system.entity.User;
import com.xytang.system.mapper.FollowMapper;
import com.xytang.system.mapper.UserMapper;
import com.xytang.system.service.FollowService;
import com.xytang.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户关注服务实现：关注/取关（幂等）、粉丝/关注列表、我的资料。
 *
 * <p>设计要点：关注关系用唯一索引保证幂等，并发重复插入由
 * {@code DuplicateKeyException} 兜底；列表查询批量取用户信息避免 N+1。</p>
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    /**
     * 关注/取消关注（幂等）：已关注则取关并返回 false，未关注则关注并返回 true。
     *
     * @param targetUserId 被关注用户 ID
     * @param userId       当前登录用户 ID
     * @return true=本次已关注，false=本次已取消（含并发兜底）
     * @throws BizException           关注自己时抛出
     * @throws UserNotFoundException 目标用户不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFollow(Long targetUserId, Long userId) {
        // 1. 禁止关注自己，避免产生自环关注关系
        if (Objects.equals(targetUserId, userId)) {
            throw new BizException(BizCode.PARAM_ERROR, "禁止关注自己");
        }
        // 2. 校验目标用户存在，防止关注不存在的账号
        requireUser(targetUserId);
        // 3. 查询当前用户与该目标是否已存在关注关系（LIMIT 1 兜底唯一约束）
        Follow record = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .eq(Follow::getFolloweeId, targetUserId)
                .last("LIMIT 1"));
        if (record == null) {
            // 4. 未关注：插入关注关系；并发重复插入由唯一索引兜底，DuplicateKeyException 视为已关注保持幂等
            Follow insert = new Follow();
            insert.setFollowerId(userId);
            insert.setFolloweeId(targetUserId);
            try {
                followMapper.insert(insert);
                return true;
            } catch (DuplicateKeyException e) {
                return false;
            }
        }
        // 5. 已关注：删除既有关系，实现取关
        followMapper.deleteById(record.getId());
        return false;
    }

    /**
     * 分页查询粉丝列表（关注了该用户的人）。
     *
     * @param userId 目标用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 粉丝用户分页结果
     * @throws UserNotFoundException 目标用户不存在时抛出
     */
    @Override
    public PageResult<UserVO> pageFollowers(Long userId, int page, int size) {
        requireUser(userId);
        // 1. 分页查询 followeeId = userId 的关注记录，即"谁关注了该用户"
        Page<Follow> followPage = new Page<>(page, size);
        IPage<Follow> result = followMapper.selectPage(followPage, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId)
                .orderByDesc(Follow::getCreateTime));
        // 2. 提取粉丝 ID 批量查询用户信息，避免逐条查询造成 N+1
        List<Long> ids = result.getRecords().stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
        List<UserVO> list = resolveUsers(ids);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 分页查询关注列表（该用户关注的人）。
     *
     * @param userId 目标用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 关注用户分页结果
     * @throws UserNotFoundException 目标用户不存在时抛出
     */
    @Override
    public PageResult<UserVO> pageFollowing(Long userId, int page, int size) {
        requireUser(userId);
        // 1. 分页查询 followerId = userId 的关注记录，即"该用户关注了谁"
        Page<Follow> followPage = new Page<>(page, size);
        IPage<Follow> result = followMapper.selectPage(followPage, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreateTime));
        // 2. 提取被关注者 ID 批量查询用户信息，避免逐条查询造成 N+1
        List<Long> ids = result.getRecords().stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());
        List<UserVO> list = resolveUsers(ids);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 查询用户完整资料（含关注数/粉丝数统计）。
     *
     * @param userId 用户 ID
     * @return 用户资料 VO
     * @throws UserNotFoundException 用户不存在时抛出
     */
    @Override
    public UserVO getProfile(Long userId) {
        User user = requireUser(userId);
        return toVO(user);
    }

    // 强制目标用户存在：不存在直接抛 UserNotFoundException，避免后续关联数据悬挂
    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }

    // 批量查询用户并转 VO；空集合直接返回空列表，避免生成 IN () 的非法 SQL
    private List<UserVO> resolveUsers(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, ids))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    // 实体转 VO：顺带统计关注数/粉丝数（当前逐用户计数，用户量大时可改为聚合查询优化）
    private UserVO toVO(User user) {
        return UserVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .bio(user.getBio())
            .followCount(followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, user.getId())))
            .followerCount(followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFolloweeId, user.getId())))
            .build();
    }
}