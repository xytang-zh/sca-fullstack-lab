<script setup lang="ts">
/**
 * Dashboard 布局：左侧角色菜单 + 右侧内容区。
 * - 菜单由 permissionStore 按角色动态生成（用户中心 + 管理员菜单）
 * - 顶栏含返回首页入口与当前用户头像
 */
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

/** 菜单图标映射：permissionStore 的 icon 名称 → Naive UI 图标 VNode */
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

/** 侧边栏菜单项：由 permissionStore 按角色过滤后的菜单渲染为 Naive UI 菜单配置 */
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

/** 菜单选中：跳转到对应路由路径 */
function handleSelect(key: string) {
  router.push(key)
}

/** 退出登录并返回首页 */
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