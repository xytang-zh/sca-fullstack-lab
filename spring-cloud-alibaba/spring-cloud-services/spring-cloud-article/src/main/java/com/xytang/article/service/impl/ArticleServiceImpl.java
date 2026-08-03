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
import com.xytang.article.vo.ArticleVO;
import com.xytang.common.core.exception.BizException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 博客文章服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    /** 文章状态：已发布（游客可见） */
    private static final int STATUS_PUBLISHED = 3;

    private final ArticleMapper articleMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;
    private final Cache<String, PageVO<ArticleVO>> articleListCache;
    private final HotWeightProperties hotWeight;

    @Override
    public PageVO<ArticleVO> page(ArticlePageQuery query) {
        String cacheKey = query.getSort() + ":" + query.getPageNum() + ":" + query.getPageSize();
        PageVO<ArticleVO> cached = articleListCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        Page<ArticleVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<ArticleVO> result = articleMapper.selectPublishedPage(page, query.getSort(),
                hotWeight.getViews(), hotWeight.getLikes(),
                hotWeight.getFavorites(), hotWeight.getComments());
        PageVO<ArticleVO> vo = PageVO.of(result.getRecords(), result.getTotal(),
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
        vo.setComments(article.getComments());
        vo.setPublishTime(article.getPublishTime());
        return vo;
    }

    @Override
    public ArticleVO create(ArticleCreateDTO dto, Long authorId) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContentMd(dto.getContentMd());
        article.setSlug(dto.getSlug());
        article.setCoverImage(dto.getCoverImage());
        article.setAuthorId(authorId);
        article.setStatus(STATUS_PUBLISHED);
        article.setPublishTime(LocalDateTime.now());
        article.setViews(0L);
        article.setLikes(0L);
        article.setFavorites(0L);
        article.setComments(0L);
        articleMapper.insert(article);
        return toVO(article);
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

    private ArticleVO toVO(Article article) {
        ArticleVO vo = new ArticleVO();
        vo.setId(String.valueOf(article.getId()));
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setCoverImage(article.getCoverImage());
        vo.setAuthorId(String.valueOf(article.getAuthorId()));
        vo.setViews(article.getViews());
        vo.setLikes(article.getLikes());
        vo.setFavorites(article.getFavorites());
        vo.setComments(article.getComments());
        vo.setPublishTime(article.getPublishTime());
        return vo;
    }
}
