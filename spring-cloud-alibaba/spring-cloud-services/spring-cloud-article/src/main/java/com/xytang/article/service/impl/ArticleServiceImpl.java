package com.xytang.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.xytang.article.config.HotWeightProperties;
import com.xytang.article.dto.ArticleCreateDTO;
import com.xytang.article.dto.ArticlePageQuery;
import com.xytang.article.entity.Article;
import com.xytang.article.entity.Favorite;
import com.xytang.article.entity.LikeRecord;
import com.xytang.article.mapper.ArticleMapper;
import com.xytang.article.mapper.FavoriteMapper;
import com.xytang.article.mapper.LikeRecordMapper;
import com.xytang.article.service.ArticleService;
import com.xytang.article.vo.ArticleDetailVO;
import com.xytang.article.vo.ArticleStatsVO;
import com.xytang.article.vo.ArticleVO;
import com.xytang.common.core.exception.BizException;
import com.xytang.common.core.exception.PermissionException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.PageResult;
import com.xytang.common.dubbo.CommentRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 博客文章服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    /** 文章状态：草稿（仅作者可见） */
    private static final int STATUS_DRAFT = 1;

    /** 文章状态：已发布（游客可见） */
    private static final int STATUS_PUBLISHED = 3;

    private final ArticleMapper articleMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;
    private final Cache<String, PageResult<ArticleVO>> articleListCache;
    private final HotWeightProperties hotWeight;

    /**
     * 评论数统计（Dubbo 远程调用 comment 服务）。
     * 注：Dubbo 3.3.6 的 @DubboReference 不支持构造器参数注入，采用字段注入（Dubbo 官方惯例）。
     */
    @DubboReference
    private CommentRpcService commentRpcService;

    @Override
    public PageResult<ArticleVO> page(ArticlePageQuery query) {
        String cacheKey = query.getSort() + ":" + query.getPage() + ":" + query.getSize()
                + ":" + (query.getAuthorIds() == null ? "" : query.getAuthorIds());
        PageResult<ArticleVO> cached = articleListCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        Page<ArticleVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<ArticleVO> result = articleMapper.selectPublishedPage(page, query.getSort(),
                hotWeight.getViews(), hotWeight.getLikes(),
                hotWeight.getFavorites(), hotWeight.getComments(),
                parseAuthorIds(query.getAuthorIds()));
        PageResult<ArticleVO> vo = PageResult.of(result.getRecords(), result.getTotal(),
                Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
        articleListCache.put(cacheKey, vo);
        return vo;
    }

    @Override
    public ArticleDetailVO detail(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null || !Objects.equals(STATUS_PUBLISHED, article.getStatus())) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        // 阅读量 +1（固定 SQL 片段，无外部输入）
        articleMapper.update(null, new UpdateWrapper<Article>()
                .setSql("views = views + 1")
                .eq("id", id));
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(String.valueOf(article.getId()));
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContentMd(article.getContentMd());
        vo.setCoverImage(article.getCoverImage());
        vo.setAuthorId(String.valueOf(article.getAuthorId()));
        vo.setViews(article.getViews() + 1);
        vo.setLikes(article.getLikes());
        vo.setFavorites(article.getFavorites());
        vo.setComments(resolveCommentCount(id));
        vo.setPublishTime(article.getPublishTime());
        return vo;
    }

    private long resolveCommentCount(Long articleId) {
        try {
            return commentRpcService.countByArticleId(articleId);
        } catch (RpcException e) {
            return 0L;
        }
    }

    @Override
    public ArticleVO create(ArticleCreateDTO dto, Long authorId) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContentMd(dto.getContentMd());
        article.setSlug(dto.getSlug());
        article.setCoverImage(dto.getCoverImage());
        article.setColumnId(dto.getColumnId());
        article.setAuthorId(authorId);
        boolean published = dto.getStatus() == null || Objects.equals(STATUS_PUBLISHED, dto.getStatus());
        article.setStatus(published ? STATUS_PUBLISHED : STATUS_DRAFT);
        if (published) {
            article.setPublishTime(LocalDateTime.now());
        }
        article.setViews(0L);
        article.setLikes(0L);
        article.setFavorites(0L);
        article.setComments(0L);
        articleMapper.insert(article);
        return toVO(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO update(ArticleCreateDTO dto, Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        if (!Objects.equals(article.getAuthorId(), userId)) {
            throw new PermissionException(BizCode.DATA_SCOPE_DENIED);
        }
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContentMd(dto.getContentMd());
        article.setSlug(dto.getSlug());
        article.setCoverImage(dto.getCoverImage());
        article.setColumnId(dto.getColumnId());
        boolean published = dto.getStatus() == null || Objects.equals(STATUS_PUBLISHED, dto.getStatus());
        if (published && !Objects.equals(STATUS_PUBLISHED, article.getStatus())) {
            article.setStatus(STATUS_PUBLISHED);
            article.setPublishTime(LocalDateTime.now());
        } else if (!published && Objects.equals(STATUS_PUBLISHED, article.getStatus())) {
            article.setStatus(STATUS_DRAFT);
            article.setPublishTime(null);
        }
        articleMapper.updateById(article);
        return toVO(article);
    }

    @Override
    public PageResult<ArticleVO> pageMyArticles(Long userId, int page, int size) {
        Page<Article> articlePage = new Page<>(page, size);
        IPage<Article> result = articleMapper.selectPage(articlePage, new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId)
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Article::getPublishTime));
        return toPageResult(result);
    }

    @Override
    public PageResult<ArticleVO> pageMyDrafts(Long userId, int page, int size) {
        Page<Article> articlePage = new Page<>(page, size);
        IPage<Article> result = articleMapper.selectPage(articlePage, new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId)
                .eq(Article::getStatus, STATUS_DRAFT)
                .orderByDesc(Article::getUpdateTime));
        return toPageResult(result);
    }

    @Override
    public PageResult<ArticleVO> pageMyLikes(Long userId, int page, int size) {
        Page<LikeRecord> likePage = new Page<>(page, size);
        IPage<LikeRecord> result = likeRecordMapper.selectPage(likePage, new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .orderByDesc(LikeRecord::getCreateTime));
        List<Long> articleIds = result.getRecords().stream()
                .map(LikeRecord::getArticleId)
                .distinct()
                .collect(Collectors.toList());
        List<ArticleVO> list = queryByIds(articleIds);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public PageResult<ArticleVO> pageMyFavorites(Long userId, int page, int size) {
        Page<Favorite> favoritePage = new Page<>(page, size);
        IPage<Favorite> result = favoriteMapper.selectPage(favoritePage, new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));
        List<Long> articleIds = result.getRecords().stream()
                .map(Favorite::getArticleId)
                .distinct()
                .collect(Collectors.toList());
        List<ArticleVO> list = queryByIds(articleIds);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public ArticleStatsVO stats() {
        ArticleStatsVO vo = new ArticleStatsVO();
        vo.setTotalArticles(articleMapper.selectCount(new LambdaQueryWrapper<Article>()));
        vo.setPublishedArticles(articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED)));
        vo.setPendingArticles(articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 2)));
        vo.setDraftArticles(articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_DRAFT)));
        vo.setTotalLikes(likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()));
        vo.setTotalFavorites(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long articleId, Integer status) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        if (!Objects.equals(2, article.getStatus())) {
            throw new BizException(BizCode.CONTENT_STATUS_INVALID);
        }
        article.setStatus(status);
        if (Objects.equals(STATUS_PUBLISHED, status)) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleMapper.updateById(article);
    }

    @Override
    public PageResult<ArticleVO> pagePending(int page, int size) {
        Page<Article> articlePage = new Page<>(page, size);
        IPage<Article> result = articleMapper.selectPage(articlePage, new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 2)
                .orderByAsc(Article::getCreateTime));
        return toPageResult(result);
    }

    @Override
    public ArticleDetailVO getForEdit(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        if (!Objects.equals(article.getAuthorId(), userId)) {
            throw new PermissionException(BizCode.DATA_SCOPE_DENIED);
        }
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(String.valueOf(article.getId()));
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContentMd(article.getContentMd());
        vo.setCoverImage(article.getCoverImage());
        vo.setColumnId(article.getColumnId() == null ? null : String.valueOf(article.getColumnId()));
        vo.setAuthorId(String.valueOf(article.getAuthorId()));
        vo.setViews(article.getViews());
        vo.setLikes(article.getLikes());
        vo.setFavorites(article.getFavorites());
        vo.setComments(article.getComments());
        vo.setPublishTime(article.getPublishTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        if (!Objects.equals(article.getAuthorId(), userId)) {
            throw new PermissionException(BizCode.DATA_SCOPE_DENIED);
        }
        articleMapper.deleteById(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long articleId, Long userId) {
        LikeRecord record = likeRecordMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getArticleId, articleId)
                .eq(LikeRecord::getUserId, userId)
                .last("LIMIT 1"));
        if (record == null) {
            LikeRecord insert = new LikeRecord();
            insert.setArticleId(articleId);
            insert.setUserId(userId);
            try {
                likeRecordMapper.insert(insert);
                articleMapper.incrCount(articleId, "likes");
                return true;
            } catch (DuplicateKeyException e) {
                return false;
            }
        }
        likeRecordMapper.deleteById(record.getId());
        articleMapper.decrCount(articleId, "likes");
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long articleId, Long userId) {
        Favorite record = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId)
                .last("LIMIT 1"));
        if (record == null) {
            Favorite insert = new Favorite();
            insert.setArticleId(articleId);
            insert.setUserId(userId);
            try {
                favoriteMapper.insert(insert);
                articleMapper.incrCount(articleId, "favorites");
                return true;
            } catch (DuplicateKeyException e) {
                return false;
            }
        }
        favoriteMapper.deleteById(record.getId());
        articleMapper.decrCount(articleId, "favorites");
        return false;
    }

    private List<Long> parseAuthorIds(String authorIds) {
        if (authorIds == null || authorIds.isBlank()) {
            return null;
        }
        return Arrays.stream(authorIds.split(","))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private List<ArticleVO> queryByIds(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return List.of();
        }
        return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .in(Article::getId, articleIds)
                        .eq(Article::getStatus, STATUS_PUBLISHED))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private PageResult<ArticleVO> toPageResult(IPage<Article> result) {
        List<ArticleVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    private ArticleVO toVO(Article article) {
        ArticleVO vo = new ArticleVO();
        vo.setId(String.valueOf(article.getId()));
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setCoverImage(article.getCoverImage());
        vo.setAuthorId(String.valueOf(article.getAuthorId()));
        vo.setColumnId(article.getColumnId() == null ? null : String.valueOf(article.getColumnId()));
        vo.setStatus(article.getStatus());
        vo.setViews(article.getViews());
        vo.setLikes(article.getLikes());
        vo.setFavorites(article.getFavorites());
        vo.setComments(article.getComments());
        vo.setPublishTime(article.getPublishTime());
        return vo;
    }
}
