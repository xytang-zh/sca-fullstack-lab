<script setup lang="ts">
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuOption } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import {
  PersonOutline,
  LockClosedOutline,
  DocumentTextOutline,
  CreateOutline,
  AlbumsOutline,
  StarOutline,
  ThumbsUpOutline,
  ChatbubbleOutline,
  PeopleOutline,
  AddCircleOutline,
  BarChartOutline,
  ReaderOutline,
  CheckmarkDoneOutline,
  PersonAddOutline,
  MenuOutline
} from '@vicons/ionicons5'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const iconMap: Record<string, ReturnType<typeof h>> = {
  person: h(PersonOutline),
  lock: h(LockClosedOutline),
  document: h(DocumentTextOutline),
  create: h(CreateOutline),
  albums: h(AlbumsOutline),
  star: h(StarOutline),
  'thumbs-up': h(ThumbsUpOutline),
  chatbubble: h(ChatbubbleOutline),
  people: h(PeopleOutline),
  add: h(AddCircleOutline),
  stats: h(BarChartOutline),
  audit: h(ReaderOutline),
  'comment-audit': h(CheckmarkDoneOutline),
  'user-manage': h(PersonAddOutline)
}

const menuOptions = computed<MenuOption[]>(() => {
  return permissionStore.menus.map((m) => {
    const icon = m.icon
    return {
      label: m.label,
      key: m.key,
      icon: icon ? () => iconMap[icon] : undefined
    }
  })
})

function handleSelect(key: string) {
  router.push(key)
}

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen flex bg-gray-50">
    <!-- 左侧菜单 -->
    <aside class="w-56 shrink-0 bg-white border-r border-gray-200 flex flex-col">
      <div class="h-14 flex items-center px-5 border-b border-gray-100">
        <n-button text class="text-lg font-bold text-indigo-600" @click="router.push('/')">
          Sca 博客
        </n-button>
      </div>
      <div class="flex-1 overflow-y-auto py-3">
        <n-menu
          :value="route.path"
          :options="menuOptions"
          :root-indent="16"
          @update:value="handleSelect"
        />
      </div>
      <div class="p-4 border-t border-gray-100">
        <n-text depth="3" class="text-xs block mb-2">
          {{ userStore.userInfo?.nickname ?? userStore.userInfo?.username }}
        </n-text>
        <n-button size="small" block @click="handleLogout">退出登录</n-button>
      </div>
    </aside>

    <!-- 右侧内容 -->
    <div class="flex-1 flex flex-col min-w-0">
      <header class="h-14 bg-white border-b border-gray-200 flex items-center px-6 gap-3">
        <n-button quaternary circle size="small" @click="router.push('/')">
          <template #icon><n-icon :component="MenuOutline" /></template>
        </n-button>
        <n-text class="text-sm text-gray-500">返回首页</n-text>
        <div class="flex-1" />
        <n-avatar round size="small" :src="userStore.userInfo?.avatar">
          {{ (userStore.userInfo?.nickname ?? userStore.userInfo?.username ?? 'U').charAt(0) }}
        </n-avatar>
      </header>
      <main class="flex-1 overflow-auto p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>