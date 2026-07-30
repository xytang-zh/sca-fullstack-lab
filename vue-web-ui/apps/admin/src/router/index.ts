import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

export const Layout = () => import('@/layouts/default/DefaultLayout.vue')

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', hideInMenu: true, noTagsView: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '工作台', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/users',
    meta: { title: '系统管理', icon: 'system' },
    children: [
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'user' }
      },
      {
        path: 'roles',
        name: 'RoleList',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理', icon: 'role' }
      },
      {
        path: 'menus',
        name: 'MenuTree',
        component: () => import('@/views/system/MenuTree.vue'),
        meta: { title: '菜单管理', icon: 'menu' }
      },
      {
        path: 'depts',
        name: 'DeptTree',
        component: () => import('@/views/system/DeptTree.vue'),
        meta: { title: '部门管理', icon: 'dept' }
      },
      {
        path: 'dicts',
        name: 'DictList',
        component: () => import('@/views/system/DictList.vue'),
        meta: { title: '字典管理', icon: 'dict' }
      },
      {
        path: 'params',
        name: 'ParamList',
        component: () => import('@/views/system/ParamList.vue'),
        meta: { title: '参数管理', icon: 'param' }
      },
      {
        path: 'notices',
        name: 'NoticeList',
        component: () => import('@/views/system/NoticeList.vue'),
        meta: { title: '通知公告', icon: 'notice' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '404', hideInMenu: true, noTagsView: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
