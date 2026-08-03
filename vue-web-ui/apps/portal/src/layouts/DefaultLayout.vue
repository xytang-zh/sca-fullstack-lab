<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.token !== '')

onMounted(() => {
  if (isLoggedIn.value) {
    userStore.fetchUserInfo().catch(() => {
      // Token 失效时拦截器已处理跳转，此处静默
    })
  }
})

function goHome() {
  router.push('/')
}

function goLogin() {
  // 导航栏登录不携带 redirect：登录成功后进入个人主页（spec 约定）
  router.push('/login')
}

function goProfile() {
  router.push('/profile')
}

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

const dropdownOptions = [
  { label: '个人主页', key: 'profile' },
  { label: '退出登录', key: 'logout' }
]

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
    <header class="h-14 bg-white border-b border-gray-200 sticky top-0 z-10">
      <div class="max-w-5xl mx-auto h-full px-4 flex items-center justify-between">
        <button class="flex items-center gap-2 text-lg font-bold text-gray-800" @click="goHome">
          Sca 博客
        </button>
        <div class="flex items-center gap-3">
          <template v-if="isLoggedIn">
            <n-dropdown :options="dropdownOptions" trigger="click" @select="handleDropdown">
              <button class="flex items-center gap-2 hover:opacity-80">
                <n-avatar round size="small" :src="userStore.userInfo?.avatar">
                  {{ (userStore.userInfo?.nickname ?? userStore.userInfo?.username ?? 'U').charAt(0) }}
                </n-avatar>
                <span class="text-sm text-gray-700">
                  {{ userStore.userInfo?.nickname ?? userStore.userInfo?.username }}
                </span>
              </button>
            </n-dropdown>
          </template>
          <template v-else>
            <n-button type="primary" size="small" @click="goLogin">登录</n-button>
          </template>
        </div>
      </div>
    </header>

    <main class="flex-1">
      <router-view />
    </main>

    <footer class="py-6 text-center text-xs text-gray-400">
      Sca 博客 · 企业级一体化智能管理平台示例
    </footer>
  </div>
</template>
