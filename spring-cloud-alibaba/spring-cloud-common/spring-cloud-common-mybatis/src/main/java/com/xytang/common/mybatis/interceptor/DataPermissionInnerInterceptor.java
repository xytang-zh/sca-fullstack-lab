package com.xytang.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.xytang.common.mybatis.annotation.DataScope;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 数据权限拦截器（自研，对齐 data-model.md §4.2 + §2.2 五级数据范围）
 *
 * <p>基于 JSqlParser 拼接 IN 子查询或 = 条件，实现 5 级数据范围控制：
 * <ol>
 *   <li>全部：不拼接</li>
 *   <li>本部门及以下：{@code dept_id IN (SELECT id FROM sys_dept WHERE id=? OR FIND_IN_SET(?, ancestors))}</li>
 *   <li>仅本部门：{@code dept_id = ?}</li>
 *   <li>仅本人：{@code creator = ?}</li>
 *   <li>自定义：通过 sys_role_dept 关联表</li>
 * </ol>
 *
 * <p>使用方式：在 Mapper 方法或 Service 方法上标注 {@link DataScope}。
 *
 * <p>注意：MVP 阶段实现 SQL 改写骨架，实际拼接逻辑在 T018 RBAC Starter 中完善。
 */
@Slf4j
@Component
public class DataPermissionInnerInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, org.apache.ibatis.mapping.BoundSql boundSql)
        throws SQLException {
        DataScope dataScope = resolveDataScope(ms);
        if (dataScope == null) {
            return;
        }
        String sql = boundSql.getSql();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                return;
            }
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
            String rewritten = rewriteSelect(select, dataScope, parameter);
            if (rewritten != null) {
                PluginUtils.mpBoundSql(boundSql).sql(rewritten);
            }
        } catch (JSQLParserException e) {
            log.error("[DataPermission] SQL parse failed: {}", sql, e);
        }
    }

    private DataScope resolveDataScope(MappedStatement ms) {
        String id = ms.getId();
        int lastDot = id.lastIndexOf('.');
        if (lastDot < 0) {
            return null;
        }
        String className = id.substring(0, lastDot);
        String methodName = id.substring(lastDot + 1);
        try {
            Class<?> clazz = Class.forName(className);
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataScope.class)) {
                    return method.getAnnotation(DataScope.class);
                }
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
        return null;
    }

    /**
     * SQL 改写：基于 @DataScope 配置与入参中的 dataScope 值拼接 WHERE 条件。
     *
     * <p>MVP 实现骨架：仅打 DEBUG 日志，实际改写逻辑在 T018 RBAC Starter 中完善。
     */
    private String rewriteSelect(PlainSelect select, DataScope scope, Object parameter) {
        int effectiveScope = scope.defaultScope();
        log.debug("[DataPermission] scope={} alias.dept={} alias.user={} on SQL: {}",
            effectiveScope, scope.deptAlias(), scope.userAlias(), select);
        // TODO(T018): 根据 effectiveScope + 当前登录用户的 deptId / userId / 角色，拼接 IN 子查询或 = 条件
        return null;
    }

    static {
        // 静态初始化：确保 JSqlParser 类可被加载
        PluginUtils.class.getName();
        Arrays.toString(new Class<?>[]{});
    }
}
