<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute, RouterView, type RouteRecordRaw } from 'vue-router'
import {
  NLayout,
  NLayoutHeader,
  NLayoutSider,
  NLayoutContent,
  NMenu,
  NBreadcrumb,
  NBreadcrumbItem,
  NAvatar,
  NDropdown,
  useDialog,
  useMessage
} from 'naive-ui'
import { useUserStore } from '@/store/user'
import { routes } from '@/router'

const router = useRouter()
const route = useRoute()
const dialog = useDialog()
const message = useMessage()
const userStore = useUserStore()

const collapsed = ref(false)

const menus = computed(() => {
  return routes
    .filter((r: RouteRecordRaw) => r.path === '/system' || r.path === '/')
    .flatMap((r: RouteRecordRaw) => {
      if (r.path === '/') {
        return (r.children ?? []).map((c: RouteRecordRaw) => ({
          label: c.meta?.title as string,
          key: `/${c.path}`,
          icon: undefined
        }))
      }
      return [
        {
          label: r.meta?.title as string,
          key: r.path,
          icon: undefined,
          children: (r.children ?? []).map((c: RouteRecordRaw) => ({
            label: c.meta?.title as string,
            key: `${r.path}/${c.path}`,
            icon: undefined
          }))
        }
      ]
    })
})

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((m) => m.meta?.title)
  return matched.map((m) => m.meta?.title as string)
})

function handleSelect(key: string) {
  router.push(key)
}

async function handleUserAction(action: string) {
  if (action === 'logout') {
    dialog.warning({
      title: '确认登出',
      content: '确定要退出登录吗？',
      positiveText: '确认',
      negativeText: '取消',
      onPositiveClick: async () => {
        await userStore.logout()
        message.success('已退出登录')
        router.replace('/login')
      }
    })
  } else if (action === 'profile') {
    message.info('个人中心将在后续迭代中开放')
  }
}
</script>

<template>
  <n-layout has-sider position="absolute">
    <n-layout-sider
      bordered
      :collapsed="collapsed"
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="logo">
        <span v-if="!collapsed">SCA Admin</span>
        <span v-else>SA</span>
      </div>
      <n-menu
        :value="route.path"
        :options="menus"
        :collapsed="collapsed"
        :collapsed-width="64"
        @update:value="handleSelect"
      />
    </n-layout-sider>
    <n-layout>
      <n-layout-header bordered class="header">
        <div class="header-left">
          <n-breadcrumb>
            <n-breadcrumb-item v-for="b in breadcrumbs" :key="b">{{ b }}</n-breadcrumb-item>
          </n-breadcrumb>
        </div>
        <div class="header-right">
          <n-dropdown
            :options="[
              { label: '个人中心', key: 'profile' },
              { label: '退出登录', key: 'logout' }
            ]"
            trigger="click"
            @select="handleUserAction"
          >
            <div class="user">
              <n-avatar
                round
                size="small"
                :src="userStore.userInfo?.avatar"
              />
              <span>{{ userStore.userInfo?.nickname ?? userStore.userInfo?.username }}</span>
            </div>
          </n-dropdown>
        </div>
      </n-layout-header>
      <n-layout-content class="content">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<style scoped>
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  color: #667eea;
  border-bottom: 1px solid #e5e7eb;
}
.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
}
.user:hover {
  background: #f3f4f6;
}
.content {
  padding: 24px;
  background: #f5f5f5;
}
</style>
