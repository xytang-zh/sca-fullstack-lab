<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { userApi } from '@sca/api'
import type { UserVO } from '@sca/types'

const users = ref<UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const data = await userApi.pageUsers({
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value || undefined
    })
    users.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-4 gap-4">
      <h2 class="text-xl font-semibold text-gray-800">用户管理</h2>
      <n-input
        v-model:value="keyword"
        placeholder="搜索用户名/昵称"
        clearable
        style="max-width: 240px"
        @keyup.enter="search"
        @clear="search"
      />
    </div>

    <n-spin :show="loading">
      <div v-if="users.length" class="space-y-3">
        <div
          v-for="user in users"
          :key="user.id"
          class="bg-white rounded-xl border border-gray-100 p-4 flex items-center gap-4"
        >
          <n-avatar round :size="44" :src="user.avatar">
            {{ (user.nickname ?? user.username).charAt(0) }}
          </n-avatar>
          <div class="min-w-0 flex-1">
            <n-text class="font-medium text-gray-800 block">{{ user.nickname ?? user.username }}</n-text>
            <n-text depth="3" class="text-xs block">
              @{{ user.username }} · 关注 {{ user.followCount ?? 0 }} · 粉丝 {{ user.followerCount ?? 0 }}
            </n-text>
          </div>
          <n-tag size="small" :type="user.status === 2 ? 'success' : 'default'">
            {{ user.status === 2 ? '正常' : '禁用' }}
          </n-tag>
        </div>
      </div>
      <n-empty v-else description="暂无用户" class="py-16" />
    </n-spin>
    <div v-if="total > pageSize" class="mt-4 flex justify-center">
      <n-pagination v-model:page="pageNum" :page-size="pageSize" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>