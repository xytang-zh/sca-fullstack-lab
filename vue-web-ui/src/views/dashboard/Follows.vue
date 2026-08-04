<script setup lang="ts">
/**
 * 关注订阅页（用户中心）：Tab 切换展示我关注的人 / 关注我的人 / 我订阅的专栏。
 */
import { onMounted, ref, watch } from 'vue'
import { articleApi, userApi } from '@sca/api'
import { useUserStore } from '@/store/user'
import type { ColumnVO, UserVO } from '@sca/types'

const userStore = useUserStore()

type Tab = 'following' | 'followers' | 'columns'
/** 当前 Tab：following=我关注的人 followers=关注我的人 columns=我订阅的专栏 */
const tab = ref<Tab>('following')
/** 用户列表（关注/粉丝 Tab 共用） */
const users = ref<UserVO[]>([])
/** 专栏列表（订阅 Tab 使用） */
const columns = ref<ColumnVO[]>([])
/** 总数（分页用） */
const total = ref(0)
/** 加载中标识 */
const loading = ref(false)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const size = 10

/** Tab 配置 */
const tabs = [
  { key: 'following' as Tab, label: '我关注的人' },
  { key: 'followers' as Tab, label: '关注我的人' },
  { key: 'columns' as Tab, label: '我订阅的专栏' }
]

/** 加载当前 Tab 数据：订阅 Tab 查专栏，其余查用户 */
async function load() {
  loading.value = true
  try {
    const myId = userStore.userInfo?.id ?? ''
    if (tab.value === 'columns') {
      const data = await articleApi.myColumnSubscriptions(page.value, size)
      columns.value = data.records
      total.value = data.total
    } else {
      const data =
        tab.value === 'following'
          ? await userApi.following(myId, page.value, size)
          : await userApi.followers(myId, page.value, size)
      users.value = data.records
      total.value = data.total
    }
  } finally {
    loading.value = false
  }
}

/** Tab 切换：重置页码并重新加载 */
watch(tab, () => {
  page.value = 1
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

    <div v-if="total > size" class="mt-4 flex justify-center">
      <n-pagination v-model:page="page" :page-size="size" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>