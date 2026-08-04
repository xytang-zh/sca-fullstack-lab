<script setup lang="ts">
/**
 * 文章审核页（管理员）：分页展示待审核文章，支持通过/驳回操作。
 */
import { onMounted, ref } from 'vue'
import { createDiscreteApi } from 'naive-ui'
import { articleApi } from '@sca/api'
import type { ArticleVO } from '@sca/types'

const { message } = createDiscreteApi(['message'])
/** 待审核文章列表 */
const articles = ref<ArticleVO[]>([])
/** 总数（分页用） */
const total = ref(0)
/** 加载中标识 */
const loading = ref(false)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const size = 10

/** 分页加载待审核文章 */
async function load() {
  loading.value = true
  try {
    const data = await articleApi.pendingArticles(page.value, size)
    articles.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 审核文章（3=通过 4=驳回），成功后提示并刷新列表 */
async function audit(article: ArticleVO, status: 3 | 4) {
  await articleApi.auditArticle(article.id, status)
  message.success(status === 3 ? '已通过' : '已驳回')
  load()
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">文章审核</h2>
    <n-spin :show="loading">
      <div v-if="articles.length" class="space-y-3">
        <div
          v-for="article in articles"
          :key="article.id"
          class="bg-white rounded-xl border border-gray-100 p-4"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0">
              <n-text class="font-medium text-gray-800 block">{{ article.title }}</n-text>
              <n-text v-if="article.summary" depth="2" class="text-sm line-clamp-1 mt-1 block">
                {{ article.summary }}
              </n-text>
              <n-text depth="3" class="text-xs mt-2 block">
                提交于 {{ (article.publishTime ?? '').replace('T', ' ').slice(0, 16) }}
              </n-text>
            </div>
            <div class="flex gap-2 shrink-0">
              <n-button size="tiny" type="primary" @click="audit(article, 3)">通过</n-button>
              <n-button size="tiny" type="error" quaternary @click="audit(article, 4)">驳回</n-button>
            </div>
          </div>
        </div>
      </div>
      <n-empty v-else description="暂无待审核文章" class="py-16" />
    </n-spin>
    <div v-if="total > size" class="mt-4 flex justify-center">
      <n-pagination v-model:page="page" :page-size="size" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>