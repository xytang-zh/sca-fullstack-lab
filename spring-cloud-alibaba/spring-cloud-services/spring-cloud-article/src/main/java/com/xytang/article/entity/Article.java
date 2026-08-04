package com.xytang.article.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客文章实体。
 *
 * <p>状态流转：1=草稿（仅作者可见）→ 2=待审核（提交后进入审核队列）
 * → 3=已发布（游客可见，触发阅读量/互动计数）或 4=已驳回（作者可见驳回原因）。
 * 互动计数（点赞/收藏/评论）为冗余字段，用于列表与热度排序，避免实时聚合。</p>
 */
@Data
@TableName("t_article")
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文章 ID（雪花 ID） */
    @TableId
    private Long id;

    /** 标题 */
    private String title;

    /** 摘要（列表页展示，最长 512 字） */
    private String summary;

    /** Markdown 正文 */
    private String contentMd;

    /** 状态：1=草稿 2=待审核 3=已发布 4=已驳回 */
    private Integer status;

    /** 作者 ID */
    private Long authorId;

    /** URL 友好标识（唯一，用于 SEO 可读链接） */
    private String slug;

    /** 封面图 URL */
    private String coverImage;

    /** 所属专栏 ID（可空） */
    private Long columnId;

    /** 阅读量 */
    private Long views;

    /** 点赞数 */
    private Long likes;

    /** 收藏数 */
    private Long favorites;

    /** 评论数（冗余计数，点赞/收藏更新时同步维护） */
    private Long comments;

    /** 发布时间（草稿为 null） */
    private LocalDateTime publishTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号（MyBatis-Plus @Version 防并发覆盖） */
    @Version
    private Integer version;

    /** 逻辑删除标记：0=未删 1=已删 */
    @TableLogic
    private Integer deleted;
}
