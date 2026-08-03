<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import type { UserInfoVO } from '@sca/types'

const router = useRouter()
const userStore = useUserStore()
const user = ref<UserInfoVO | null>(null)

onMounted(async () => {
  user.value = await userStore.fetchUserInfo()
})

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-10">
    <template v-if="user">
      <n-card class="!rounded-2xl" content-style="padding: 32px">
        <div class="flex items-center gap-6">
          <n-avatar round :size="80" :src="user.avatar">
            {{ (user.nickname ?? user.username).charAt(0) }}
          </n-avatar>
          <div>
            <h1 class="text-2xl font-bold text-gray-900">{{ user.nickname || user.username }}</h1>
            <n-text depth="3" class="text-sm mt-1 block">@{{ user.username }}</n-text>
            <n-space class="mt-3 text-sm text-gray-500" :size="24">
              <n-text v-if="user.phone" depth="2">手机号：{{ user.phone }}</n-text>
              <n-text v-if="user.email" depth="2">邮箱：{{ user.email }}</n-text>
            </n-space>
          </div>
        </div>
      </n-card>

      <n-grid class="mt-6" :cols="2" :x-gap="16" responsive="screen" item-responsive>
        <n-grid-item span="2 m:1">
          <n-card hoverable content-style="padding: 24px; text-align: center">
            <n-text class="text-3xl font-bold text-gray-800 mb-1 block">我的文章</n-text>
            <n-text depth="3" class="text-sm block">管理你发布的博客文章</n-text>
          </n-card>
        </n-grid-item>
        <n-grid-item span="2 m:1">
          <n-card hoverable content-style="padding: 24px; text-align: center">
            <n-text class="text-3xl font-bold text-gray-800 mb-1 block">我的收藏</n-text>
            <n-text depth="3" class="text-sm block">查看收藏的文章</n-text>
          </n-card>
        </n-grid-item>
      </n-grid>

      <n-text depth="3" class="text-xs text-center mt-8 block">
        我的文章 / 我的收藏功能将在后续迭代开放
      </n-text>
    </template>

    <n-button v-else class="mt-10" @click="goHome">返回首页</n-button>
  </div>
</template>
