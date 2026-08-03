package com.xytang.article.service;

import com.xytang.article.dto.ColumnCreateDTO;
import com.xytang.article.vo.ColumnVO;
import com.xytang.common.core.response.PageVO;

/**
 * 博客专栏服务：列表（游客可读）/我的专栏/订阅/创建/编辑/删除。
 */
public interface ColumnService {

    /**
     * 专栏分页列表（游客可访问），可按作者过滤。
     *
     * @param userId        作者 ID（可空）
     * @param currentUserId 当前登录用户 ID（可空，用于标注是否已订阅）
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @return 分页结果
     */
    PageVO<ColumnVO> page(Long userId, Long currentUserId, int pageNum, int pageSize);

    /**
     * 我的专栏列表（登录用户）。
     *
     * @param userId   当前登录用户
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 专栏列表
     */
    PageVO<ColumnVO> listMyColumns(Long userId, int pageNum, int pageSize);

    /**
     * 我订阅的专栏列表（登录用户）。
     *
     * @param userId   当前登录用户
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 专栏列表
     */
    PageVO<ColumnVO> listMySubscriptions(Long userId, int pageNum, int pageSize);

    /**
     * 创建专栏（登录用户）。
     *
     * @param dto    专栏内容
     * @param userId 当前登录用户
     * @return 创建后的专栏
     */
    ColumnVO create(ColumnCreateDTO dto, Long userId);

    /**
     * 编辑专栏（仅作者）。
     *
     * @param id     专栏 ID
     * @param dto    新内容
     * @param userId 当前登录用户
     */
    void update(Long id, ColumnCreateDTO dto, Long userId);

    /**
     * 删除专栏（仅作者）：解除文章关联，不删除文章。
     *
     * @param id     专栏 ID
     * @param userId 当前登录用户
     */
    void delete(Long id, Long userId);

    /**
     * 订阅/取消订阅（幂等）：第一次订阅返回 true，再次执行取消返回 false。
     *
     * @param columnId 专栏 ID
     * @param userId   当前登录用户
     * @return true=本次已订阅，false=本次已取消
     */
    boolean toggleSubscribe(Long columnId, Long userId);
}