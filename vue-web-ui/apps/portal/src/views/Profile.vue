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
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 flex items-center gap-6">
        <n-avatar round :size="80" :src="user.avatar">
          {{ (user.nickname ?? user.username).charAt(0) }}
        </n-avatar>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">{{ user.nickname || user.username }}</h1>
          <p class="text-sm text-gray-400 mt-1">@{{ user.username }}</p>
          <div class="flex gap-6 mt-3 text-sm text-gray-500">
            <span v-if="user.phone">手机号：{{ user.phone }}</span>
            <span v-if="user.email">邮箱：{{ user.email }}</span>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4 mt-6">
        <div class="bg-white rounded-xl border border-gray-100 p-6 text-center hover:shadow-md transition-shadow">
          <p class="text-3xl font-bold text-gray-800 mb-1">我的文章</p>
          <p class="text-sm text-gray-400">管理你发布的博客文章</p>
        </div>
        <div class="bg-white rounded-xl border border-gray-100 p-6 text-center hover:shadow-md transition-shadow">
          <p class="text-3xl font-bold text-gray-800 mb-1">我的收藏</p>
          <p class="text-sm text-gray-400">查看收藏的文章</p>
        </div>
      </div>

      <p class="text-xs text-gray-300 text-center mt-8">
        我的文章 / 我的收藏功能将在后续迭代开放
      </p>
    </template>

    <n-button v-else class="mt-10" @click="goHome">返回首页</n-button>
  </div>
</template>
