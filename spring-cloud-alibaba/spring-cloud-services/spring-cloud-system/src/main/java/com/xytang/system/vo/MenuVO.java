package com.xytang.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 菜单出参。
 */
@Data
@Builder
@Schema(description = "菜单视图对象（含子节点树）")
public class MenuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单 ID */
    private Long id;
    /** 父菜单 ID */
    private Long parentId;
    /** 菜单名称 */
    private String menuName;
    /** 菜单类型（1=目录 2=菜单 3=按钮） */
    private Integer menuType;
    /** 路由地址 */
    private String path;
    /** 前端组件路径 */
    private String component;
    /** 权限标识 */
    private String perms;
    /** 菜单图标 */
    private String icon;
    /** 排序号 */
    private Integer sort;
    /** 是否可见 */
    private Integer visible;
    /** 状态 */
    private Integer status;
    /** 子菜单列表（树形结构） */
    private List<MenuVO> children;
}
