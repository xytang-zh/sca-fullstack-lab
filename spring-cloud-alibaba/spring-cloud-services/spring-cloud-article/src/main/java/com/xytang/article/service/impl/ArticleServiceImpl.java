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
 * 博客文章服务实现：文章 CRUD、状态流转、点赞/收藏、统计与阅读量。
 *
 * <p>设计要点：列表使用 Caffeine 本地缓存抗热点；互动幂等依赖唯一索引 +
 * {@code DuplicateKeyException} 兜底；评论数经 Dubbo 聚合 comment 服务，
 * 远程不可用时降级为 0，保证文章详情不受影响。</p>
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

    /**
     * 分页查询已发布文章（按时间/热度排序），结果经 Caffeine 本地缓存抗热点。
     *
     * @param query 分页与过滤条件（排序方式/作者过滤）
     * @return 文章分页结果
     */
    @Override
    public PageResult<ArticleVO> page(ArticlePageQuery query) {
        // 1. 构造缓存 Key（排序 + 分页 + 作者过滤），命中本地缓存直接返回，避免热点列表反复打 DB
        String cacheKey = query.getSort() + ":" + query.getPage() + ":" + query.getSize()
                + ":" + (query.getAuthorIds() == null ? "" : query.getAuthorIds());
        PageResult<ArticleVO> cached = articleListCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. 未命中则查库：热度排序权重由 Nacos 动态注入，SQL 参数化防止注入
        Page<ArticleVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<ArticleVO> result = articleMapper.selectPublishedPage(page, query.getSort(),
                hotWeight.getViews(), hotWeight.getLikes(),
                hotWeight.getFavorites(), hotWeight.getComments(),
                parseAuthorIds(query.getAuthorIds()));
        // 3. 写回缓存后返回（TTL 5 分钟，见 CaffeineConfig）
        PageResult<ArticleVO> vo = PageResult.of(result.getRecords(), result.getTotal(),
                Math.toIntExact(result.getCurrent()), Math.toIntExact(result.getSize()));
        articleListCache.put(cacheKey, vo);
        return vo;
    }

    /**
     * 查询文章详情（仅已发布可见，阅读量 +1，评论数经 Dubbo 聚合）。
     *
     * @param id 文章 ID
     * @return 文章详情 VO
     * @throws BizException 文章不存在或未发布时抛出
     */
    @Override
    public ArticleDetailVO detail(Long id) {
        Article article = articleMapper.selectById(id);
        // 仅已发布文章对游客可见：草稿/待审核/已驳回一律视为不存在，避免泄露未公开内容
        if (article == null || !Objects.equals(STATUS_PUBLISHED, article.getStatus())) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        // 阅读量 +1（固定 SQL 片段，无外部输入）
        articleMapper.update(null, new UpdateWrapper<Article>()
                .setSql("views = views + 1")
                .eq("id", id));
        ArticleDetailVO vo = new ArticleDetailVO();
        // 雪花 ID 转 String 返回，避免前端 JS Number 精度丢失
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

    // 评论数经 Dubbo 聚合 comment 服务；comment 服务不可用时降级为 0，不影响文章详情展示
    private long resolveCommentCount(Long articleId) {
        try {
            return commentRpcService.countByArticleId(articleId);
        } catch (RpcException e) {
            return 0L;
        }
    }

    /**
     * 创建文章：状态缺省视为发布，发布时记录发布时间并初始化互动计数为 0。
     *
     * @param dto      文章创建入参
     * @param authorId 作者用户 ID（取自登录态）
     * @return 创建后的文章 VO
     */
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
        // 状态缺省视为发布；发布时记发布时间，草稿留空（重新发布时再补）
        boolean published = dto.getStatus() == null || Objects.equals(STATUS_PUBLISHED, dto.getStatus());
        article.setStatus(published ? STATUS_PUBLISHED : STATUS_DRAFT);
        if (published) {
            article.setPublishTime(LocalDateTime.now());
        }
        // 初始化互动计数为 0，保证列表/详情 VO 字段始终非空
        article.setViews(0L);
        article.setLikes(0L);
        article.setFavorites(0L);
        article.setComments(0L);
        articleMapper.insert(article);
        return toVO(article);
    }

    /**
     * 更新文章（仅作者本人）：同步处理草稿/发布状态流转与发布时间。
     *
     * @param dto       文章更新入参
     * @param articleId 文章 ID
     * @param userId    当前登录用户 ID
     * @return 更新后的文章 VO
     * @throws BizException       文章不存在时抛出
     * @throws PermissionException 非作者越权编辑时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO update(ArticleCreateDTO dto, Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BizException(BizCode.CONTENT_NOT_FOUND);
        }
        // 越权防护：仅作者本人可编辑，防止通过 URL 篡改他人文章
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
        // 状态流转：草稿→发布需补发布时间；发布→草稿需清空发布时间（再次发布时重新补）
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

    /**
     * 分页查询当前用户已发布的文章（按作者隔离，发布时间倒序）。
     *
     * @param userId 用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 文章分页结果
     */
    @Override
    public PageResult<ArticleVO> pageMyArticles(Long userId, int page, int size) {
        // 按作者隔离 + 仅已发布状态，按发布时间倒序
        Page<Article> articlePage = new Page<>(page, size);
        IPage<Article> result = articleMapper.selectPage(articlePage, new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId)
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Article::getPublishTime));
        return toPageResult(result);
    }

    /**
     * 分页查询当前用户的草稿文章（仅作者个人可见，按更新时间倒序）。
     *
     * @param userId 用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 草稿分页结果
     */
    @Override
    public PageResult<ArticleVO> pageMyDrafts(Long userId, int page, int size) {
        // 仅作者个人可见的草稿，按更新时间倒序（最近编辑的排前面）
        Page<Article> articlePage = new Page<>(page, size);
        IPage<Article> result = articleMapper.selectPage(articlePage, new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId)
                .eq(Article::getStatus, STATUS_DRAFT)
                .orderByDesc(Article::getUpdateTime));
        return toPageResult(result);
    }

    /**
     * 分页查询当前用户点赞过的文章（先查点赞记录再批量回查已发布文章）。
     *
     * @param userId 用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 点赞文章分页结果
     */
    @Override
    public PageResult<ArticleVO> pageMyLikes(Long userId, int page, int size) {
        // 1. 先按用户分页查询点赞记录（按点赞时间倒序）
        Page<LikeRecord> likePage = new Page<>(page, size);
        IPage<LikeRecord> result = likeRecordMapper.selectPage(likePage, new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .orderByDesc(LikeRecord::getCreateTime));
        // 2. 收集去重后的文章 ID，批量回查已发布文章（过滤已删除/下线的）
        List<Long> articleIds = result.getRecords().stream()
                .map(LikeRecord::getArticleId)
                .distinct()
                .collect(Collectors.toList());
        List<ArticleVO> list = queryByIds(articleIds);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 分页查询当前用户收藏过的文章（先查收藏记录再批量回查已发布文章）。
     *
     * @param userId 用户 ID
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 收藏文章分页结果
     */
    @Override
    public PageResult<ArticleVO> pageMyFavorites(Long userId, int page, int size) {
        // 1. 先按用户分页查询收藏记录（按收藏时间倒序）
        Page<Favorite> favoritePage = new Page<>(page, size);
        IPage<Favorite> result = favoriteMapper.selectPage(favoritePage, new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));
        // 2. 收集去重后的文章 ID，批量回查已发布文章（过滤已删除/下线的）
        List<Long> articleIds = result.getRecords().stream()
                .map(Favorite::getArticleId)
                .distinct()
                .collect(Collectors.toList());
        List<ArticleVO> list = queryByIds(articleIds);
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 统计文章整体数据（总数/已发布/待审核/草稿/点赞/收藏）。
     *
     * @return 文章统计 VO
     */
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

    /**
     * 审核文章（仅待审核状态可操作）：通过则补发布时间，驳回则保留原状态。
     *
     * @param articleId 文章 ID
     * @param status    审核结果状态（3=通过发布 4=驳回）
     * @throws BizException 文章不存在或非待审核状态时抛出
     */
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

    /**
     * 分页查询待审核文章（管理员审核队列，按创建时间升序先来先审）。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 待审核文章分页结果
     */
    @Override
    public PageResult<ArticleVO> pagePending(int page, int size) {
        Page<Article> articlePage = new Page<>(page, size);
        IPage<Article> result = articleMapper.selectPage(articlePage, new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 2)
                .orderByAsc(Article::getCreateTime));
        return toPageResult(result);
    }

    /**
     * 获取文章用于编辑（仅作者本人，含草稿/待审核态，非法作者拒绝）。
     *
     * @param articleId 文章 ID
     * @param userId    当前登录用户 ID
     * @return 文章详情 VO（含 Markdown 原文）
     * @throws BizException       文章不存在时抛出
     * @throws PermissionException 非作者越权访问时抛出
     */
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

    /**
     * 删除文章（物理删除，仅作者本人可操作）。
     *
     * @param articleId 文章 ID
     * @param userId    当前登录用户 ID
     * @throws BizException       文章不存在时抛出
     * @throws PermissionException 非作者越权删除时抛出
     */
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

    /**
     * 点赞/取消点赞（幂等）：未点赞则插入记录并 +1，已点赞则删除记录并 -1。
     *
     * <p>并发重复插入由唯一索引兜底，捕获 DuplicateKeyException 视为已点赞保持幂等。</p>
     *
     * @param articleId 文章 ID
     * @param userId    当前登录用户 ID
     * @return true=本次已点赞 false=本次已取消
     */
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

    /**
     * 收藏/取消收藏（幂等）：未收藏则插入记录并 +1，已收藏则删除记录并 -1。
     *
     * @param articleId 文章 ID
     * @param userId    当前登录用户 ID
     * @return true=本次已收藏 false=本次已取消
     */
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
