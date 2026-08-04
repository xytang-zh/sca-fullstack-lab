<script setup lang="ts">
/**
 * 我的收藏页（用户中心）：分页展示当前用户收藏的文章。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi } from '@sca/api'
import type { ArticleVO } from '@sca/types'

const router = useRouter()
/** 收藏文章列表 */
const articles = ref<ArticleVO[]>([])
/** 总数（分页用） */
const total = ref(0)
/** 加载中标识 */
const loading = ref(false)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const size = 10

/** 分页加载我收藏的文章 */
async function load() {
  loading.value = true
  try {
    const data = await articleApi.myFavorites(page.value, size)
    articles.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 跳转文章详情 */
function goDetail(id: string) {
  router.push(`/articles/${id}`)
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">我的收藏</h2>
    <n-spin :show="loading">
      <div v-if="articles.length" class="space-y-3">
        <div
          v-for="article in articles"
          :key="article.id"
          class="bg-white rounded-xl border border-gray-100 p-4 cursor-pointer hover:shadow-md transition-all"
          @click="goDetail(article.id)"
        >
          <n-text class="font-medium text-gray-800 block">{{ article.title }}</n-text>
          <n-text v-if="article.summary" depth="2" class="text-sm line-clamp-1 mt-1 block">
            {{ article.summary }}
          </n-text>
          <n-text depth="3" class="text-xs mt-2 block">
            点赞 {{ article.likes }} · 收藏 {{ article.favorites }} · 评论 {{ article.comments }}
          </n-text>
        </div>
      </div>
      <n-empty v-else description="还没有收藏文章" class="py-16" />
    </n-spin>
    <div v-if="total > size" class="mt-4 flex justify-center">
      <n-pagination v-model:page="page" :page-size="size" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>