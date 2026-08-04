<script setup lang="ts">
/**
 * 数据统计页（管理员）：卡片式展示文章总数/已发布/待审核/草稿/点赞/收藏。
 */
import { onMounted, ref } from 'vue'
import { articleApi } from '@sca/api'

/** 文章统计数据结构（与后端 ArticleStatsVO 对齐） */
interface Stats {
  totalArticles: number
  publishedArticles: number
  pendingArticles: number
  draftArticles: number
  totalLikes: number
  totalFavorites: number
}

/** 统计结果（加载前后为 null） */
const stats = ref<Stats | null>(null)

/** 统计卡片配置：key 对应 Stats 字段，label 为展示名，color 为数值颜色 */
const cards = [
  { key: 'totalArticles', label: '文章总数', color: 'text-indigo-600' },
  { key: 'publishedArticles', label: '已发布', color: 'text-green-600' },
  { key: 'pendingArticles', label: '待审核', color: 'text-amber-600' },
  { key: 'draftArticles', label: '草稿', color: 'text-gray-600' },
  { key: 'totalLikes', label: '总点赞', color: 'text-rose-600' },
  { key: 'totalFavorites', label: '总收藏', color: 'text-sky-600' }
] as const

onMounted(async () => {
  stats.value = await articleApi.articleStats()
})
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">数据统计</h2>
    <div v-if="stats" class="grid grid-cols-2 md:grid-cols-3 gap-4">
      <div
        v-for="card in cards"
        :key="card.key"
        class="bg-white rounded-xl border border-gray-100 p-5 text-center"
      >
        <n-text class="text-3xl font-bold block mb-1" :class="card.color">
          {{ stats[card.key] }}
        </n-text>
        <n-text depth="3" class="text-sm">{{ card.label }}</n-text>
      </div>
    </div>
    <n-skeleton v-else text :repeat="6" class="mt-4" />
  </div>
</template>