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
import com.xytang.common.core.response.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 博客专栏服务实现。
 */
@Service
@RequiredArgsConstructor
public class ColumnServiceImpl implements ColumnService {

    private static final int STATUS_NORMAL = 1;

    private final ColumnMapper columnMapper;
    private final ColumnSubscribeMapper subscribeMapper;
    private final ArticleMapper articleMapper;

    @Override
    public PageVO<ColumnVO> page(Long userId, Long currentUserId, int pageNum, int pageSize) {
        Page<Column> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Column> wrapper = new LambdaQueryWrapper<Column>()
                .eq(Column::getStatus, STATUS_NORMAL)
                .orderByDesc(Column::getCreateTime);
        if (userId != null) {
            wrapper.eq(Column::getUserId, userId);
        }
        IPage<Column> result = columnMapper.selectPage(page, wrapper);
        List<ColumnVO> list = result.getRecords().stream()
                .map(c -> toVO(c, currentUserId))
                .collect(Collectors.toList());
        return PageVO.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public PageVO<ColumnVO> listMyColumns(Long userId, int pageNum, int pageSize) {
        Page<Column> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Column> wrapper = new LambdaQueryWrapper<Column>()
                .eq(Column::getUserId, userId)
                .orderByDesc(Column::getUpdateTime);
        IPage<Column> result = columnMapper.selectPage(page, wrapper);
        List<ColumnVO> list = result.getRecords().stream()
                .map(c -> toVO(c, userId))
                .collect(Collectors.toList());
        return PageVO.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public PageVO<ColumnVO> listMySubscriptions(Long userId, int pageNum, int pageSize) {
        Page<ColumnSubscribe> subPage = new Page<>(pageNum, pageSize);
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
        return PageVO.of(list, subResult.getTotal(), (int) subResult.getCurrent(), (int) subResult.getSize());
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ColumnCreateDTO dto, Long userId) {
        Column column = requireOwn(id, userId);
        column.setName(dto.getName());
        column.setDescription(dto.getDescription());
        column.setCoverImage(dto.getCoverImage());
        columnMapper.updateById(column);
    }

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