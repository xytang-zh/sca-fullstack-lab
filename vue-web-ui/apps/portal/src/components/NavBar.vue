<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.token !== '')
const keyword = ref('')
const activeTab = computed(() => {
  const tab = route.query.tab
  if (tab === 'hot' || tab === 'follow' || tab === 'columns') {
    return tab
  }
  return 'time'
})

function goHome() {
  router.push('/')
}

function switchTab(tab: string) {
  router.push({ path: '/', query: { tab } })
}

function goSearch() {
  const q = keyword.value.trim()
  if (!q) {
    return
  }
  router.push({ path: '/search', query: { q } })
}

function goWrite() {
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: '/dashboard/write' } })
    return
  }
  router.push('/dashboard/write')
}

function goProfile() {
  router.push('/dashboard/profile')
}

function goSettings() {
  router.push('/dashboard/password')
}

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

const dropdownOptions = [
  { label: '我的主页', key: 'profile' },
  { label: '设置', key: 'settings' },
  { label: '退出', key: 'logout' }
]

function handleDropdown(key: string) {
  if (key === 'profile') {
    goProfile()
  } else if (key === 'settings') {
    goSettings()
  } else if (key === 'logout') {
    handleLogout()
  }
}

const navItems = [
  { key: 'follow', label: '关注' },
  { key: 'time', label: '最新' },
  { key: 'hot', label: '热门' },
  { key: 'columns', label: '专栏' }
]
</script>

<template>
  <header class="h-14 bg-white border-b border-gray-200 sticky top-0 z-20">
    <div class="max-w-[1440px] mx-auto h-full px-4 flex items-center gap-6">
      <!-- 文字 logo -->
      <button class="text-xl font-bold text-indigo-600 shrink-0" @click="goHome">
        Sca 博客
      </button>

      <!-- 导航 -->
      <nav class="flex items-center gap-5">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="text-sm transition-colors"
          :class="activeTab === item.key ? 'text-indigo-600 font-semibold' : 'text-gray-600 hover:text-gray-900'"
          @click="switchTab(item.key)"
        >
          {{ item.label }}
        </button>
      </nav>

      <!-- 搜索框 -->
      <div class="flex-1 max-w-md">
        <n-input
          v-model:value="keyword"
          placeholder="搜索你感兴趣的内容…"
          size="small"
          round
          clearable
          @keyup.enter="goSearch"
        />
      </div>

      <!-- 右侧操作区 -->
      <div class="flex items-center gap-3 shrink-0">
        <n-button quaternary circle size="small" title="写文章" @click="goWrite">
          <template #icon>
            <span class="text-xl leading-none">＋</span>
          </template>
        </n-button>
        <n-button quaternary circle size="small" title="消息" @click="goProfile">
          <template #icon>
            <span class="text-base leading-none">🔔</span>
          </template>
        </n-button>
        <n-button quaternary circle size="small" title="私信" @click="goProfile">
          <template #icon>
            <span class="text-base leading-none">✉️</span>
          </template>
        </n-button>

        <template v-if="isLoggedIn">
          <n-dropdown :options="dropdownOptions" trigger="click" @select="handleDropdown">
            <n-button text class="hover:opacity-80">
              <template #icon>
                <n-avatar round size="small" :src="userStore.userInfo?.avatar">
                  {{ (userStore.userInfo?.nickname ?? userStore.userInfo?.username ?? 'U').charAt(0) }}
                </n-avatar>
              </template>
            </n-button>
          </n-dropdown>
        </template>
        <template v-else>
          <n-button size="small" type="primary" ghost @click="router.push('/login')">登录</n-button>
        </template>
      </div>
    </div>
  </header>
</template>