package com.xytang.common.satoken.stp;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色查询实现（对齐 spring-cloud-alibaba/CLAUDE.md §11.6 + data-model.md §6 缓存 Key）
 *
 * <p>本类为骨架实现：返回空列表。实际实现由各业务服务在自身工程内重写，
 * 通过 Dubbo 调用 spring-cloud-system 服务获取角色/权限点列表。
 *
 * <p>缓存策略：登录后写入 Redis Key {@code spring-cloud:auth:user:perms:{userId}}
 * 与 {@code spring-cloud:auth:user:roles:{userId}}，TTL 30 分钟 + ±10% 随机。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // TODO(T029): 通过 Dubbo 调用 system 服务的 UserRpcService.getPermissionList(Long)
        // 返回 List<String>，如 system:user:list、system:user:create 等。
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // TODO(T029): 通过 Dubbo 调用 system 服务的 UserRpcService.getRoleList(Long)
        // 返回 List<String>，如 super_admin、system_admin 等。
        return Collections.emptyList();
    }
}
