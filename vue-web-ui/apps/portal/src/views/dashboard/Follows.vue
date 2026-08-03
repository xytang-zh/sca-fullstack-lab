<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { articleApi, userApi } from '@sca/api'
import { useUserStore } from '@/store/user'
import type { ColumnVO, UserVO } from '@sca/types'

const userStore = useUserStore()

type Tab = 'following' | 'followers' | 'columns'
const tab = ref<Tab>('following')
const users = ref<UserVO[]>([])
const columns = ref<ColumnVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10

const tabs = [
  { key: 'following' as Tab, label: '我关注的人' },
  { key: 'followers' as Tab, label: '关注我的人' },
  { key: 'columns' as Tab, label: '我订阅的专栏' }
]

async function load() {
  loading.value = true
  try {
    const myId = userStore.userInfo?.id ?? ''
    if (tab.value === 'columns') {
      const data = await articleApi.myColumnSubscriptions(pageNum.value, pageSize)
      columns.value = data.list
      total.value = data.total
    } else {
      const data =
        tab.value === 'following'
          ? await userApi.following(myId, pageNum.value, pageSize)
          : await userApi.followers(myId, pageNum.value, pageSize)
      users.value = data.list
      total.value = data.total
    }
  } finally {
    loading.value = false
  }
}

watch(tab, () => {
  pageNum.value = 1
  load()
})

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">关注订阅</h2>
    <n-tabs v-model:value="tab" type="line" class="mb-4">
      <n-tab-pane v-for="t in tabs" :key="t.key" :name="t.key" :tab="t.label" />
    </n-tabs>

    <n-spin :show="loading">
      <!-- 用户列表 -->
      <div v-if="tab !== 'columns'">
        <div v-if="users.length" class="space-y-3">
          <div
            v-for="user in users"
            :key="user.id"
            class="bg-white rounded-xl border border-gray-100 p-4 flex items-center gap-4"
          >
            <n-avatar round :size="44" :src="user.avatar">
              {{ (user.nickname ?? user.username).charAt(0) }}
            </n-avatar>
            <div class="min-w-0">
              <n-text class="font-medium text-gray-800 block">{{ user.nickname ?? user.username }}</n-text>
              <n-text depth="3" class="text-xs block truncate">
                @{{ user.username }} · 关注 {{ user.followCount ?? 0 }} · 粉丝 {{ user.followerCount ?? 0 }}
              </n-text>
            </div>
          </div>
        </div>
        <n-empty v-else description="暂无用户" class="py-16" />
      </div>

      <!-- 专栏列表 -->
      <div v-else>
        <div v-if="columns.length" class="space-y-3">
          <div
            v-for="column in columns"
            :key="column.id"
            class="bg-white rounded-xl border border-gray-100 p-4 flex items-center justify-between"
          >
            <div>
              <n-text class="font-medium text-gray-800 block">{{ column.name }}</n-text>
              <n-text v-if="column.description" depth="3" class="text-xs line-clamp-1 block">
                {{ column.description }}
              </n-text>
            </div>
            <n-text depth="3" class="text-xs shrink-0 ml-4">{{ column.articleCount }} 篇文章</n-text>
          </div>
        </div>
        <n-empty v-else description="还没有订阅专栏" class="py-16" />
      </div>
    </n-spin>

    <div v-if="total > pageSize" class="mt-4 flex justify-center">
      <n-pagination v-model:page="pageNum" :page-size="pageSize" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>