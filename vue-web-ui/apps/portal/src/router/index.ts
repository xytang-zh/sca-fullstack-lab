import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@sca/utils'

const routes: RouteRecordRaw[] = [
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
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
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
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - Sca 博客` : 'Sca 博客'

  if (to.meta.requiresAuth && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles) {
    const roles = to.meta.roles as string[]
    const { useUserStore } = await import('@/store/user')
    const userStore = useUserStore()
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
      return { path: '/dashboard/profile' }
    }
  }

  return true
})

export default router