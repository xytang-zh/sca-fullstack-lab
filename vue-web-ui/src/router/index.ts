import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@sca/utils'

/**
 * 路由表：公开路由（博客/文章详情/搜索）+ 登录页 + 需鉴权的 dashboard（用户中心与管理页）。
 * 页面组件一律懒加载（() => import），保证首屏只加载所需 chunk。
 */
const routes: RouteRecordRaw[] = [
  // ============ 公开路由：博客前台，游客可访问 ============
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '博客' }
      },
      {
        path: 'articles/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/ArticleDetail.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/Search.vue'),
        meta: { title: '搜索' }
      }
    ]
  },
  // ============ 登录/注册页 ============
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  // ============ 用户中心 + 管理页（requiresAuth，未登录跳登录页） ============
  {
    path: '/dashboard',
    component: () => import('@/layouts/DashboardLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard/profile',
    children: [
      {
        path: 'profile',
        name: 'DashboardProfile',
        component: () => import('@/views/dashboard/Profile.vue'),
        meta: { title: '个人主页', requiresAuth: true }
      },
      {
        path: 'password',
        name: 'DashboardPassword',
        component: () => import('@/views/dashboard/Password.vue'),
        meta: { title: '修改密码', requiresAuth: true }
      },
      {
        path: 'articles',
        name: 'DashboardArticles',
        component: () => import('@/views/dashboard/MyArticles.vue'),
        meta: { title: '我的文章', requiresAuth: true }
      },
      {
        path: 'drafts',
        name: 'DashboardDrafts',
        component: () => import('@/views/dashboard/Drafts.vue'),
        meta: { title: '草稿', requiresAuth: true }
      },
      {
        path: 'write',
        name: 'DashboardWrite',
        component: () => import('@/views/dashboard/Write.vue'),
        meta: { title: '撰写文章', requiresAuth: true }
      },
      {
        path: 'columns',
        name: 'DashboardColumns',
        component: () => import('@/views/dashboard/Columns.vue'),
        meta: { title: '专栏', requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'DashboardFavorites',
        component: () => import('@/views/dashboard/Favorites.vue'),
        meta: { title: '收藏', requiresAuth: true }
      },
      {
        path: 'likes',
        name: 'DashboardLikes',
        component: () => import('@/views/dashboard/Likes.vue'),
        meta: { title: '点赞', requiresAuth: true }
      },
      {
        path: 'answers',
        name: 'DashboardAnswers',
        component: () => import('@/views/dashboard/Answers.vue'),
        meta: { title: '回答', requiresAuth: true }
      },
      {
        path: 'follows',
        name: 'DashboardFollows',
        component: () => import('@/views/dashboard/Follows.vue'),
        meta: { title: '关注订阅', requiresAuth: true }
      },
      {
        path: 'stats',
        name: 'DashboardStats',
        component: () => import('@/views/dashboard/Stats.vue'),
        meta: { title: '统计', requiresAuth: true, roles: ['ADMIN', 'super_admin'] }
      },
      {
        path: 'audit/articles',
        name: 'DashboardArticleAudit',
        component: () => import('@/views/dashboard/ArticleAudit.vue'),
        meta: { title: '文章审核', requiresAuth: true, roles: ['ADMIN', 'super_admin'] }
      },
      {
        path: 'audit/comments',
        name: 'DashboardCommentAudit',
        component: () => import('@/views/dashboard/CommentAudit.vue'),
        meta: { title: '评论审核', requiresAuth: true, roles: ['ADMIN', 'super_admin'] }
      },
      {
        path: 'users',
        name: 'DashboardUsers',
        component: () => import('@/views/dashboard/UserList.vue'),
        meta: { title: '用户管理', requiresAuth: true, roles: ['ADMIN', 'super_admin'] }
      }
    ]
  },
  // ============ 兜底路由：未匹配路径重定向首页 ============
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

/** 路由实例：HTML5 History 模式，路由切换时滚动回顶部 */
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 全局前置守卫：按"登录态 → 角色"两级拦截。
 * - 进受保护页（requiresAuth）但无 Token → 跳登录页并携带 redirect 便于登录后回跳
 * - 带 meta.roles 的页面（管理页）→ 懒加载用户信息校验角色，防止非管理员直接改 URL 越权访问
 */
router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - Sca 博客` : 'Sca 博客'

  // 1. 登录态拦截：未登录访问受保护页一律重定向登录页
  if (to.meta.requiresAuth && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 2. 角色拦截：管理页需 ADMIN/super_admin 角色，无角色信息时先拉取再判断
  if (to.meta.roles) {
    const roles = to.meta.roles as string[]
    const { useUserStore } = await import('@/store/user')
    const userStore = useUserStore()
    // 用户信息未加载过则拉取；拉取失败视为登录态失效，清空并回登录页
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        userStore.reset()
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
    const hasRole = userStore.roles.some((r) => roles.includes(r))
    if (!hasRole) {
      // 无权限：回退到个人主页，避免停留在无权限页面
      return { path: '/dashboard/profile' }
    }
  }

  return true
})

export default router