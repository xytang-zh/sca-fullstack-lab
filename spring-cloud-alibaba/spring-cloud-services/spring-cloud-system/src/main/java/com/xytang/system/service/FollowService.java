package com.xytang.system.service;

import com.xytang.common.core.response.PageVO;
import com.xytang.system.vo.UserVO;

/**
 * 用户关注服务：关注/取关、粉丝列表、关注列表、我的资料。
 */
public interface FollowService {

    /**
     * 关注/取消关注（幂等，禁止关注自己）。
     *
     * @param targetUserId 被关注用户 ID
     * @param userId       当前登录用户
     * @return true=本次已关注，false=本次已取消
     */
    boolean toggleFollow(Long targetUserId, Long userId);

    /**
     * 粉丝列表（关注该用户的人）。
     *
     * @param userId   目标用户 ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 用户列表
     */
    PageVO<UserVO> pageFollowers(Long userId, int pageNum, int pageSize);

    /**
     * 关注列表（该用户关注的人）。
     *
     * @param userId   目标用户 ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 用户列表
     */
    PageVO<UserVO> pageFollowing(Long userId, int pageNum, int pageSize);

    /**
     * 我的完整资料（含 bio、关注数、粉丝数）。
     *
     * @param userId 用户 ID
     * @return 用户资料
     */
    UserVO getProfile(Long userId);
}