package com.xytang.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xytang.article.dto.ColumnCreateDTO;
import com.xytang.article.entity.Article;
import com.xytang.article.entity.Column;
import com.xytang.article.entity.ColumnSubscribe;
import com.xytang.article.mapper.ArticleMapper;
import com.xytang.article.mapper.ColumnMapper;
import com.xytang.article.mapper.ColumnSubscribeMapper;
import com.xytang.article.service.ColumnService;
import com.xytang.article.vo.ColumnVO;
import com.xytang.common.core.exception.BizException;
import com.xytang.common.core.exception.PermissionException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 博客专栏服务实现：专栏 CRUD、订阅/取消订阅（幂等）。
 *
 * <p>设计要点：越权防护统一走 {@code requireOwn}（作者校验）；订阅幂等依赖唯一索引 +
 * {@code DuplicateKeyException} 兜底；删除专栏仅解除文章关联，不删除文章。</p>
 */
@Service
@RequiredArgsConstructor
public class ColumnServiceImpl implements ColumnService {

    /** 专栏状态：正常 */
    private static final int STATUS_NORMAL = 1;

    private final ColumnMapper columnMapper;
    private final ColumnSubscribeMapper subscribeMapper;
    private final ArticleMapper articleMapper;

    /**
     * 分页查询正常状态专栏（游客可访问，可按作者过滤并标注当前用户订阅态）。
     *
     * @param userId        作者用户 ID（可空，为空查全部）
     * @param currentUserId 当前登录用户 ID（可空，用于标注是否已订阅）
     * @param page          页码，从 1 开始
     * @param size          每页条数
     * @return 专栏分页结果
     */
    @Override
    public PageResult<ColumnVO> page(Long userId, Long currentUserId, int page, int size) {
        Page<Column> columnPage = new Page<>(page, size);
        LambdaQueryWrapper<Column> wrapper = new LambdaQueryWrapper<Column>()
                .eq(Column::getStatus, STATUS_NORMAL)
                .orderByDesc(Column::getCreateTime);
        if (userId != null) {
            wrapper.eq(Column::getUserId, userId);
        }
        IPage<Column> result = columnMapper.selectPage(columnPage, wrapper);
        List<ColumnVO> list = result.getRecords().stream()
                .map(c -> toVO(c, currentUserId))
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 分页查询当前用户创建的专栏（按用户隔离，更新时间倒序）。
     *
     * @param userId 当前登录用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 我的专栏分页结果
     */
    @Override
    public PageResult<ColumnVO> listMyColumns(Long userId, int page, int size) {
        Page<Column> columnPage = new Page<>(page, size);
        LambdaQueryWrapper<Column> wrapper = new LambdaQueryWrapper<Column>()
                .eq(Column::getUserId, userId)
                .orderByDesc(Column::getUpdateTime);
        IPage<Column> result = columnMapper.selectPage(columnPage, wrapper);
        List<ColumnVO> list = result.getRecords().stream()
                .map(c -> toVO(c, userId))
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 分页查询当前用户订阅的专栏（先查订阅记录再批量回查专栏）。
     *
     * @param userId 当前登录用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 我订阅的专栏分页结果
     */
    @Override
    public PageResult<ColumnVO> listMySubscriptions(Long userId, int page, int size) {
        Page<ColumnSubscribe> subPage = new Page<>(page, size);
        LambdaQueryWrapper<ColumnSubscribe> subWrapper = new LambdaQueryWrapper<ColumnSubscribe>()
                .eq(ColumnSubscribe::getUserId, userId)
                .orderByDesc(ColumnSubscribe::getCreateTime);
        IPage<ColumnSubscribe> subResult = subscribeMapper.selectPage(subPage, subWrapper);
        List<Long> columnIds = subResult.getRecords().stream()
                .map(ColumnSubscribe::getColumnId)
                .collect(Collectors.toList());
        List<ColumnVO> list;
        if (columnIds.isEmpty()) {
            list = List.of();
        } else {
            list = columnMapper.selectList(new LambdaQueryWrapper<Column>()
                            .in(Column::getId, columnIds)
                            .eq(Column::getStatus, STATUS_NORMAL))
                    .stream()
                    .map(c -> toVO(c, userId))
                    .collect(Collectors.toList());
        }
        return PageResult.of(list, subResult.getTotal(), (int) subResult.getCurrent(), (int) subResult.getSize());
    }

    /**
     * 创建专栏（创建者为专栏作者，状态默认正常）。
     *
     * @param dto    专栏创建入参
     * @param userId 当前登录用户 ID
     * @return 创建后的专栏 VO
     */
    @Override
    public ColumnVO create(ColumnCreateDTO dto, Long userId) {
        Column column = new Column();
        column.setUserId(userId);
        column.setName(dto.getName());
        column.setDescription(dto.getDescription());
        column.setCoverImage(dto.getCoverImage());
        column.setStatus(STATUS_NORMAL);
        columnMapper.insert(column);
        return toVO(column, userId);
    }

    /**
     * 编辑专栏（仅作者本人，先校验归属再全量更新）。
     *
     * @param id     专栏 ID
     * @param dto    专栏更新入参
     * @param userId 当前登录用户 ID
     * @throws PermissionException 非作者越权编辑时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ColumnCreateDTO dto, Long userId) {
        Column column = requireOwn(id, userId);
        column.setName(dto.getName());
        column.setDescription(dto.getDescription());
        column.setCoverImage(dto.getCoverImage());
        columnMapper.updateById(column);
    }

    /**
     * 删除专栏（仅作者本人）：物理删除专栏并解除其中文章的专栏关联，不删除文章。
     *
     * @param id     专栏 ID
     * @param userId 当前登录用户 ID
     * @throws PermissionException 非作者越权删除时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        requireOwn(id, userId);
        columnMapper.deleteById(id);
        // 解除文章关联，不删除文章
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getColumnId, id)
                .set(Article::getColumnId, null));
    }

    /**
     * 订阅/取消订阅专栏（幂等）：未订阅则插入记录返回 true，已订阅则删除返回 false。
     *
     * @param columnId 专栏 ID
     * @param userId   当前登录用户 ID
     * @return true=本次已订阅 false=本次已取消
     * @throws BizException 专栏不存在或非正常状态时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleSubscribe(Long columnId, Long userId) {
        Column column = columnMapper.selectById(columnId);
        if (column == null || !Objects.equals(STATUS_NORMAL, column.getStatus())) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        ColumnSubscribe record = subscribeMapper.selectOne(new LambdaQueryWrapper<ColumnSubscribe>()
                .eq(ColumnSubscribe::getUserId, userId)
                .eq(ColumnSubscribe::getColumnId, columnId)
                .last("LIMIT 1"));
        if (record == null) {
            ColumnSubscribe insert = new ColumnSubscribe();
            insert.setUserId(userId);
            insert.setColumnId(columnId);
            try {
                subscribeMapper.insert(insert);
                return true;
            } catch (DuplicateKeyException e) {
                return false;
            }
        }
        subscribeMapper.deleteById(record.getId());
        return false;
    }

    private Column requireOwn(Long id, Long userId) {
        Column column = columnMapper.selectById(id);
        if (column == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        if (!Objects.equals(column.getUserId(), userId)) {
            throw new PermissionException(BizCode.DATA_SCOPE_DENIED);
        }
        return column;
    }

    private ColumnVO toVO(Column column, Long currentUserId) {
        ColumnVO vo = new ColumnVO();
        vo.setId(String.valueOf(column.getId()));
        vo.setUserId(String.valueOf(column.getUserId()));
        vo.setName(column.getName());
        vo.setDescription(column.getDescription());
        vo.setCoverImage(column.getCoverImage());
        vo.setCreateTime(column.getCreateTime());
        vo.setArticleCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getColumnId, column.getId())));
        vo.setSubscribeCount(subscribeMapper.selectCount(new LambdaQueryWrapper<ColumnSubscribe>()
                .eq(ColumnSubscribe::getColumnId, column.getId())));
        if (currentUserId != null) {
            vo.setSubscribed(subscribeMapper.selectCount(new LambdaQueryWrapper<ColumnSubscribe>()
                    .eq(ColumnSubscribe::getUserId, currentUserId)
                    .eq(ColumnSubscribe::getColumnId, column.getId())) > 0);
        } else {
            vo.setSubscribed(false);
        }
        return vo;
    }
}