<script setup lang="ts">
/**
 * 知乎风格顶部导航栏：品牌 logo、内容 Tab（关注/最新/热门/专栏）、搜索框与用户区。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 是否已登录（token 非空即视为登录态） */
const isLoggedIn = computed(() => userStore.token !== '')

/** 搜索关键字（回车触发搜索） */
const keyword = ref('')

/** 当前激活 Tab：从 URL query 读取，非法值回退到"最新" */
const activeTab = computed(() => {
  const tab = route.query.tab
  if (tab === 'hot' || tab === 'follow' || tab === 'columns') {
    return tab
  }
  return 'time'
})

/** 跳转博客首页 */
function goHome() {
  router.push('/')
}

/** 切换内容 Tab：以 query 参数驱动首页列表过滤 */
function switchTab(tab: string) {
  router.push({ path: '/', query: { tab } })
}

/** 触发搜索：关键字非空才跳转搜索页 */
function goSearch() {
  const q = keyword.value.trim()
  if (!q) {
    return
  }
  router.push({ path: '/search', query: { q } })
}

/** 跳转写文章：未登录先跳登录页并携带 redirect，登录成功回跳 */
function goWrite() {
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: '/dashboard/write' } })
    return
  }
  router.push('/dashboard/write')
}

/** 跳转个人主页 */
function goProfile() {
  router.push('/dashboard/profile')
}

/** 跳转设置（修改密码） */
function goSettings() {
  router.push('/dashboard/password')
}

/** 退出登录并返回首页 */
async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

/** 用户下拉菜单项 */
const dropdownOptions = [
  { label: '我的主页', key: 'profile' },
  { label: '设置', key: 'settings' },
  { label: '退出', key: 'logout' }
]

/** 处理用户下拉选择：按 key 分发到主页/设置/登出 */
function handleDropdown(key: string) {
  if (key === 'profile') {
    goProfile()
  } else if (key === 'settings') {
    goSettings()
  } else if (key === 'logout') {
    handleLogout()
  }
}

/** 首页内容 Tab 配置 */
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