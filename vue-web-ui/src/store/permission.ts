import { computed } from 'vue'
import { defineStore } from 'pinia'
import { useUserStore } from './user'

/**
 * 侧边栏菜单项。
 * @param roles 可见角色列表（缺省表示所有登录角色可见）
 */
export interface MenuItem {
  /** 路由路径（作为菜单选中态与跳转目标） */
  key: string
  /** 菜单显示名称 */
  label: string
  /** 图标名称（映射到 DashboardLayout 的 iconMap） */
  icon?: string
  /** 可见角色列表（如 ADMIN/super_admin） */
  roles?: string[]
}

/** 用户中心菜单（所有登录角色可见） */
const USER_MENUS: MenuItem[] = [
  { key: '/dashboard/profile', label: '个人主页', icon: 'person' },
  { key: '/dashboard/password', label: '修改密码', icon: 'lock' },
  { key: '/dashboard/articles', label: '文章', icon: 'document' },
  { key: '/dashboard/drafts', label: '草稿', icon: 'create' },
  { key: '/dashboard/columns', label: '专栏', icon: 'albums' },
  { key: '/dashboard/favorites', label: '收藏', icon: 'star' },
  { key: '/dashboard/likes', label: '点赞', icon: 'thumbs-up' },
  { key: '/dashboard/answers', label: '回答', icon: 'chatbubble' },
  { key: '/dashboard/follows', label: '关注订阅', icon: 'people' },
  { key: '/dashboard/write', label: '撰写文章', icon: 'add' }
]

/** 管理菜单（仅 ADMIN / super_admin 可见） */
const ADMIN_MENUS: MenuItem[] = [
  { key: '/dashboard/stats', label: '统计', icon: 'stats', roles: ['ADMIN', 'super_admin'] },
  { key: '/dashboard/audit/articles', label: '文章审核', icon: 'audit', roles: ['ADMIN', 'super_admin'] },
  { key: '/dashboard/audit/comments', label: '评论审核', icon: 'comment-audit', roles: ['ADMIN', 'super_admin'] },
  { key: '/dashboard/users', label: '用户管理', icon: 'user-manage', roles: ['ADMIN', 'super_admin'] }
]

export const usePermissionStore = defineStore('permission', () => {
  const userStore = useUserStore()

  const isAdmin = computed(() => {
    const roles = userStore.roles
    return roles.includes('ADMIN') || roles.includes('super_admin')
  })

  const menus = computed<MenuItem[]>(() => {
    const base = [...USER_MENUS]
    if (isAdmin.value) {
      base.push(...ADMIN_MENUS)
    }
    return base
  })

  /** 判断当前路由是否允许访问（按角色过滤） */
  function canAccess(path: string): boolean {
    const item = [...USER_MENUS, ...ADMIN_MENUS].find((m) => m.key === path)
    if (!item) {
      return true
    }
    if (!item.roles) {
      return true
    }
    return item.roles.some((r) => userStore.roles.includes(r))
  }

  return { menus, isAdmin, canAccess }
})