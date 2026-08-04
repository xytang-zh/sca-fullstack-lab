<script setup lang="ts">
/**
 * 默认布局：顶部导航栏 + 内容区 + 页脚。
 * - 登录态展示用户头像下拉（个人主页/退出登录），登出后回首页
 * - 未登录展示登录按钮
 */
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

/** 是否已登录（token 非空即视为登录态） */
const isLoggedIn = computed(() => userStore.token !== '')

onMounted(() => {
  // 进入页面时若已登录则预拉取用户信息，供导航栏展示昵称/头像
  if (isLoggedIn.value) {
    userStore.fetchUserInfo().catch(() => {
      // Token 失效时拦截器已处理跳转，此处静默
    })
  }
})

/** 跳转博客首页 */
function goHome() {
  router.push('/')
}

/** 跳转登录页（不携带 redirect，登录成功进入个人主页） */
function goLogin() {
  // 导航栏登录不携带 redirect：登录成功后进入个人主页（spec 约定）
  router.push('/login')
}

/** 跳转个人主页 */
function goProfile() {
  router.push('/profile')
}

/** 登出并回到首页 */
async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

/** 导航栏用户下拉菜单项 */
const dropdownOptions = [
  { label: '个人主页', key: 'profile' },
  { label: '退出登录', key: 'logout' }
]

/** 处理用户下拉选择：按 key 分发到个人主页或登出 */
function handleDropdown(key: string) {
  if (key === 'profile') {
    goProfile()
  } else if (key === 'logout') {
    handleLogout()
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 顶栏：左侧品牌名（点击回首页），右侧用户区 -->
    <header class="h-14 bg-white border-b border-gray-200 sticky top-0 z-10">
      <div class="max-w-[1440px] mx-auto h-full px-4 flex items-center justify-between">
        <n-button text class="text-lg font-bold text-gray-800" @click="goHome">
          Sca 博客
        </n-button>
        <div class="flex items-center gap-3">
          <!-- 已登录：头像 + 昵称下拉（个人主页/退出登录） -->
          <template v-if="isLoggedIn">
            <n-dropdown :options="dropdownOptions" trigger="click" @select="handleDropdown">
              <n-button text class="hover:opacity-80">
                <template #icon>
                  <n-avatar round size="small" :src="userStore.userInfo?.avatar">
                    {{ (userStore.userInfo?.nickname ?? userStore.userInfo?.username ?? 'U').charAt(0) }}
                  </n-avatar>
                </template>
                <n-text class="text-sm text-gray-700">
                  {{ userStore.userInfo?.nickname ?? userStore.userInfo?.username }}
                </n-text>
              </n-button>
            </n-dropdown>
          </template>
          <!-- 未登录：登录按钮 -->
          <template v-else>
            <n-button type="primary" size="small" @click="goLogin">登录</n-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容区：路由视图出口 -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- 页脚：版权信息 -->
    <footer class="py-6 text-center">
      <n-text depth="3" class="text-xs">Sca 博客 · 企业级一体化智能管理平台示例</n-text>
    </footer>
  </div>
</template>
