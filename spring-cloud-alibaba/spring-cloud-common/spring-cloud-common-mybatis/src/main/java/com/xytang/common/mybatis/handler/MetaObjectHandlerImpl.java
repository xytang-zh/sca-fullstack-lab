package com.xytang.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.xytang.common.core.constant.CommonConstants;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充（对齐 data-model.md §7 关键约束）
 *
 * <p>自动填充：
 * <ul>
 *   <li>insert：creator / create_time / updater / update_time / deleted=0 / version=0</li>
 *   <li>update：updater / update_time</li>
 * </ul>
 *
 * <p>注意：creator/updater 当前从 Sa-Token 上下文读取；为避免 common-mybatis 直接依赖 common-satoken，
 * 此处只填充时间字段，creator/updater 留 TODO 注释，由业务侧在 Service 层显式填充。
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";
    private static final String CREATOR = "creator";
    private static final String UPDATER = "updater";
    private static final String DELETED = "deleted";
    private static final String VERSION = "version";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
        strictInsertFill(metaObject, DELETED, Integer.class, CommonConstants.DEL_FLAG_NOT_DELETED);
        strictInsertFill(metaObject, VERSION, Integer.class, 0);
        // TODO(T018): 从 Sa-Token 上下文获取当前登录用户 ID，填充 creator/updater
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        // TODO(T018): 从 Sa-Token 上下文获取当前登录用户 ID，填充 updater
    }
}
