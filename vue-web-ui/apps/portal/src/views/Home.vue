<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi } from '@sca/api'
import type { ArticleVO, PageVO } from '@sca/types'

const router = useRouter()
const sort = ref<'time' | 'hot'>('time')
const articles = ref<ArticleVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10

async function load() {
  loading.value = true
  try {
    const data: PageVO<ArticleVO> = await articleApi.pageArticles({
      pageNum: pageNum.value,
      pageSize,
      sort: sort.value
    })
    articles.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function changeSort(s: 'time' | 'hot') {
  if (sort.value === s) {
    return
  }
  sort.value = s
  pageNum.value = 1
  load()
}

function goDetail(id: string) {
  router.push(`/articles/${id}`)
}

function formatTime(value?: string): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-800">博客文章</h1>
      <n-radio-group :value="sort" @update:value="changeSort">
        <n-radio-button value="time">最新</n-radio-button>
        <n-radio-button value="hot">最热</n-radio-button>
      </n-radio-group>
    </div>

    <n-spin :show="loading">
      <div v-if="articles.length === 0 && !loading" class="py-16 text-center text-gray-400">
        暂无文章
      </div>
      <div class="space-y-4">
        <article
          v-for="article in articles"
          :key="article.id"
          class="bg-white rounded-lg shadow-sm border border-gray-100 p-5 cursor-pointer hover:shadow-md transition-shadow"
          @click="goDetail(article.id)"
        >
          <h2 class="text-lg font-semibold text-gray-800 hover:text-blue-600 mb-2">
            {{ article.title }}
          </h2>
          <p v-if="article.summary" class="text-sm text-gray-500 line-clamp-2 mb-3">
            {{ article.summary }}
          </p>
          <div class="flex items-center gap-4 text-xs text-gray-400">
            <span>{{ formatTime(article.publishTime) }}</span>
            <span>阅读 {{ article.views }}</span>
            <span>点赞 {{ article.likes }}</span>
            <span>收藏 {{ article.favorites }}</span>
            <span>评论 {{ article.comments }}</span>
          </div>
        </article>
      </div>
    </n-spin>

    <div v-if="total > pageSize" class="mt-6 flex justify-center">
      <n-pagination
        v-model:page="pageNum"
        :page-size="pageSize"
        :item-count="total"
        @update:page="load"
      />
    </div>
  </div>
</template>
