package com.xytang.common.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解（对齐 data-model.md §4.1 + §2.2 五级数据范围）
 *
 * <p>用法：在 Mapper 方法或 Service 方法上标注，DataPermissionInnerInterceptor
 * 通过 JSqlParser 拼接 IN 子查询或 = 条件。
 *
 * <p>数据范围（{@link #dataScope()}）：
 * <ul>
 *   <li>1 全部：不拼接</li>
 *   <li>2 本部门及以下：{@code dept_id IN (SELECT id FROM sys_dept WHERE id=? OR FIND_IN_SET(?, ancestors))}</li>
 *   <li>3 仅本部门：{@code dept_id = ?}</li>
 *   <li>4 仅本人：{@code creator = ?}</li>
 *   <li>5 自定义：通过 sys_role_dept 关联表</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 部门表别名，默认 d */
    String deptAlias() default "d";

    /** 用户表别名，默认 u */
    String userAlias() default "u";

    /** 部门 ID 字段名，默认 dept_id */
    String deptIdField() default "dept_id";

    /** 创建人字段名，默认 creator */
    String creatorField() default "creator";

    /** 数据范围参数名，从入参中读取；默认 dataScope */
    String dataScopeParam() default "dataScope";

    /** 默认数据范围（1=全部），用于未传参时的兜底 */
    int defaultScope() default 1;
}
