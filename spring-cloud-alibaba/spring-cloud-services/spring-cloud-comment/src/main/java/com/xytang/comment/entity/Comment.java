package com.xytang.comment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 博客评论（一二级嵌套回复）。
 */
@Data
@TableName("t_comment")
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评论 ID（雪花 ID，返回前端时序列化为 String 避免精度丢失） */
    @TableId
    private Long id;

    /** 所属文章 ID */
    private Long articleId;
    /** 所属文章标题（冗余存储，"我的评论"页免二次查询文章服务） */
    private String articleTitle;
    /** 评论人用户 ID */
    private Long userId;
    /** 评论人昵称 */
    private String nickname;
    /** 评论人头像 */
    private String avatar;
    /** 父评论 ID（0 表示一级评论，回复指向一级评论形成二级嵌套） */
    private Long parentId;
    /** 被回复的评论 ID（供 @ 通知联动定位被回复人） */
    private Long replyToId;
    /** 被回复的评论者昵称（冗余展示） */
    private String replyToNickname;
    /** 评论内容（纯文本，规划经敏感词/XSS 过滤后存储） */
    private String content;
    /** 状态：1=待审核 2=已审核（对外可见）3=已驳回 */
    private Integer status;
    /** 发表 IP（反垃圾溯源，代理透传时取 X-Forwarded-For 首个） */
    private String ip;
    /** 发表时 User-Agent（反垃圾溯源） */
    private String userAgent;
    /** 点赞数 */
    private Long likes;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号（MyBatis-Plus @Version 更新时自动 +1，防止并发覆盖） */
    @Version
    private Integer version;

    /** 逻辑删除标记（0=未删 1=已删，MyBatis-Plus 查询自动过滤） */
    @TableLogic
    private Integer deleted;
}