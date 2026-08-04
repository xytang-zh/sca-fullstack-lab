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
 * 用户关注服务实现。
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFollow(Long targetUserId, Long userId) {
        if (Objects.equals(targetUserId, userId)) {
            throw new BizException(BizCode.PARAM_ERROR, "禁止关注自己");
        }
        requireUser(targetUserId);
        Follow record = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .eq(Follow::getFolloweeId, targetUserId)
                .last("LIMIT 1"));
        if (record == null) {
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
        followMapper.deleteById(record.getId());
        return false;
    }

    @Override
    public PageResult<UserVO> pageFollowers(Long userId, int page, int size) {
        requireUser(userId);
        Page<Follow> followPage = new Page<>(page, size);
        IPage<Follow> result = followMapper.selectPage(followPage, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId)
                .orderByDesc(Follow::getCreateTime));
        List<Long> ids = result.getRecords().stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
        List<UserVO> list = resolveUsers(ids);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public PageResult<UserVO> pageFollowing(Long userId, int page, int size) {
        requireUser(userId);
        Page<Follow> followPage = new Page<>(page, size);
        IPage<Follow> result = followMapper.selectPage(followPage, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreateTime));
        List<Long> ids = result.getRecords().stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());
        List<UserVO> list = resolveUsers(ids);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = requireUser(userId);
        return toVO(user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }

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