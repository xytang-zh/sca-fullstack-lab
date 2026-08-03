<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { articleApi } from '@sca/api'
import type { ArticleVO } from '@sca/types'

const { message } = createDiscreteApi(['message'])
const router = useRouter()

const articles = ref<ArticleVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const data = await articleApi.myArticles(pageNum.value, pageSize)
    articles.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function handleDelete(article: ArticleVO) {
  await articleApi.deleteArticle(article.id)
  message.success('已删除')
  load()
}

function goDetail(id: string) {
  router.push(`/articles/${id}`)
}

function goEdit(article: ArticleVO) {
  router.push({ path: '/dashboard/write', query: { id: article.id } })
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-xl font-semibold text-gray-800">我的文章</h2>
      <n-button type="primary" size="small" @click="router.push('/dashboard/write')">写文章</n-button>
    </div>

    <n-spin :show="loading">
      <div v-if="articles.length" class="space-y-3">
        <div
          v-for="article in articles"
          :key="article.id"
          class="bg-white rounded-xl border border-gray-100 p-4 flex items-center justify-between"
        >
          <div class="min-w-0 cursor-pointer" @click="goDetail(article.id)">
            <n-text class="font-medium text-gray-800 block truncate">{{ article.title }}</n-text>
            <n-text depth="3" class="text-xs">
              {{ (article.publishTime ?? '').replace('T', ' ').slice(0, 10) }}
              · 阅读 {{ article.views }} · 点赞 {{ article.likes }} · 评论 {{ article.comments }}
            </n-text>
          </div>
          <div class="flex gap-2 shrink-0 ml-4">
            <n-button size="tiny" @click="goEdit(article)">编辑</n-button>
            <n-button size="tiny" type="error" quaternary @click="handleDelete(article)">删除</n-button>
          </div>
        </div>
      </div>
      <n-empty v-else description="还没有发布文章" class="py-16" />
    </n-spin>

    <div v-if="total > pageSize" class="mt-4 flex justify-center">
      <n-pagination v-model:page="pageNum" :page-size="pageSize" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>