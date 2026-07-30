import type { Router } from 'vue-router'
import { useUserStore } from '@/store/user'

const WHITELIST = ['/login', '/404']

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to) => {
    const userStore = useUserStore()
    if (WHITELIST.includes(to.path)) {
      return true
    }
    if (!userStore.token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch (err) {
        console.error('[router-guard] fetch user info failed', err)
        userStore.reset()
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
    return true
  })
}
