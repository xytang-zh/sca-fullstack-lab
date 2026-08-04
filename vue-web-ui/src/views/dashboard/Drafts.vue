<script setup lang="ts">
/**
 * 草稿箱（用户中心）：分页展示当前用户的草稿，支持继续编辑与删除。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { articleApi } from '@sca/api'
import type { ArticleVO } from '@sca/types'

const { message } = createDiscreteApi(['message'])
const router = useRouter()

/** 草稿列表 */
const drafts = ref<ArticleVO[]>([])
/** 总数（分页用） */
const total = ref(0)
/** 加载中标识 */
const loading = ref(false)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const size = 10

/** 分页加载我的草稿 */
async function load() {
  loading.value = true
  try {
    const data = await articleApi.myDrafts(page.value, size)
    drafts.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 删除草稿并刷新列表 */
async function handleDelete(draft: ArticleVO) {
  await articleApi.deleteArticle(draft.id)
  message.success('草稿已删除')
  load()
}

/** 跳转编辑器并携带草稿 ID 继续编辑 */
function goEdit(draft: ArticleVO) {
  router.push({ path: '/dashboard/write', query: { id: draft.id } })
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-xl font-semibold text-gray-800">草稿</h2>
      <n-button type="primary" size="small" @click="router.push('/dashboard/write')">写文章</n-button>
    </div>

    <n-spin :show="loading">
      <div v-if="drafts.length" class="space-y-3">
        <div
          v-for="draft in drafts"
          :key="draft.id"
          class="bg-white rounded-xl border border-gray-100 p-4 flex items-center justify-between"
        >
          <div class="min-w-0 cursor-pointer" @click="goEdit(draft)">
            <n-text class="font-medium text-gray-800 block truncate">{{ draft.title }}</n-text>
            <n-text depth="3" class="text-xs">
              更新于 {{ (draft.publishTime ?? '').replace('T', ' ').slice(0, 10) || '未知' }}
            </n-text>
          </div>
          <div class="flex gap-2 shrink-0 ml-4">
            <n-button size="tiny" @click="goEdit(draft)">继续编辑</n-button>
            <n-button size="tiny" type="error" quaternary @click="handleDelete(draft)">删除</n-button>
          </div>
        </div>
      </div>
      <n-empty v-else description="暂无草稿" class="py-16" />
    </n-spin>

    <div v-if="total > size" class="mt-4 flex justify-center">
      <n-pagination v-model:page="page" :page-size="size" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>