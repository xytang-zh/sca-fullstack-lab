package com.xytang.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户关注关系（follower_id + followee_id 唯一，保证幂等）。
 */
@Data
@TableName("t_follow")
public class Follow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关注关系 ID */
    @TableId
    private Long id;

    /** 关注者（粉丝）用户 ID */
    private Long followerId;
    /** 被关注者用户 ID */
    private Long followeeId;
    /** 关注时间 */
    private LocalDateTime createTime;
}