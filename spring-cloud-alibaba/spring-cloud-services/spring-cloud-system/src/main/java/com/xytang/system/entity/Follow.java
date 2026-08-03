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

    @TableId
    private Long id;

    private Long followerId;
    private Long followeeId;
    private LocalDateTime createTime;
}