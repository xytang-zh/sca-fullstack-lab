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

    @TableId
    private Long id;

    private Long articleId;
    private String articleTitle;
    private Long userId;
    private String nickname;
    private String avatar;
    private Long parentId;
    private Long replyToId;
    private String replyToNickname;
    private String content;
    private Integer status;
    private String ip;
    private String userAgent;
    private Long likes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}