<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@sca/api'
import { useUserStore } from '@/store/user'
import type { ArticleDetailVO } from '@sca/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref<ArticleDetailVO | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    article.value = await articleApi.getArticle(route.params.id as string)
  } finally {
    loading.value = false
  }
}

function formatTime(value?: string): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(0, 16)
}

function requireLogin(): boolean {
  if (userStore.token) {
    return true
  }
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function toggleLike() {
  if (!article.value || !requireLogin()) {
    return
  }
  const liked = await articleApi.toggleLike(article.value.id)
  article.value.likes += liked ? 1 : -1
}

async function toggleFavorite() {
  if (!article.value || !requireLogin()) {
    return
  }
  const favorited = await articleApi.toggleFavorite(article.value.id)
  article.value.favorites += favorited ? 1 : -1
}

onMounted(load)
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <button class="text-sm text-gray-400 hover:text-gray-600 mb-4" @click="router.back()">
      ← 返回列表
    </button>
    <n-spin :show="loading">
      <template v-if="article">
        <h1 class="text-3xl font-bold text-gray-900 mb-4">{{ article.title }}</h1>
        <div class="flex items-center gap-4 text-sm text-gray-400 mb-8">
          <span>{{ formatTime(article.publishTime) }}</span>
          <span>阅读 {{ article.views }}</span>
        </div>
        <div class="bg-white rounded-lg shadow-sm border border-gray-100 p-6 mb-6">
          <pre class="text-sm leading-7 text-gray-700 whitespace-pre-wrap font-mono">{{ article.contentMd }}</pre>
        </div>
        <div class="flex items-center gap-3">
          <n-button @click="toggleLike">点赞 {{ article.likes }}</n-button>
          <n-button @click="toggleFavorite">收藏 {{ article.favorites }}</n-button>
          <span v-if="!userStore.token" class="text-xs text-gray-400">
            登录后可点赞、收藏、评论
          </span>
        </div>
      </template>
    </n-spin>
  </div>
</template>
