<script setup lang="ts">
/**
 * 用户管理页（管理员）：分页展示系统用户，支持关键字搜索（用户名/昵称）。
 */
import { onMounted, ref } from 'vue'
import { userApi } from '@sca/api'
import type { UserVO } from '@sca/types'

/** 用户列表 */
const users = ref<UserVO[]>([])
/** 总数（分页用） */
const total = ref(0)
/** 加载中标识 */
const loading = ref(false)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const size = 10
/** 搜索关键字（用户名/昵称模糊） */
const keyword = ref('')

/** 分页加载用户列表（带关键字过滤） */
async function load() {
  loading.value = true
  try {
    const data = await userApi.pageUsers({
      page: page.value,
      size,
      keyword: keyword.value || undefined
    })
    users.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索：重置页码后重新加载 */
function search() {
  page.value = 1
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
    <div v-if="total > size" class="mt-4 flex justify-center">
      <n-pagination v-model:page="page" :page-size="size" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>