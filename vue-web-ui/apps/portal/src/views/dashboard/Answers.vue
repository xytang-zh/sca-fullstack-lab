<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { commentApi } from '@sca/api'
import type { CommentMyVO } from '@sca/types'

const router = useRouter()
const comments = ref<CommentMyVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10

const statusText: Record<number, { label: string; type: 'default' | 'success' | 'warning' }> = {
  1: { label: '待审核', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'default' }
}

async function load() {
  loading.value = true
  try {
    const data = await commentApi.myComments(pageNum.value, pageSize)
    comments.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function goArticle(articleId: string) {
  router.push(`/articles/${articleId}`)
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">我的回答</h2>
    <n-spin :show="loading">
      <div v-if="comments.length" class="space-y-3">
        <div
          v-for="comment in comments"
          :key="comment.id"
          class="bg-white rounded-xl border border-gray-100 p-4 cursor-pointer hover:shadow-md transition-all"
          @click="goArticle(comment.articleId)"
        >
          <div class="flex items-center justify-between mb-1">
            <n-text depth="3" class="text-xs">评论于「{{ comment.articleTitle }}」</n-text>
            <n-tag size="tiny" :type="statusText[comment.status]?.type ?? 'default'">
              {{ statusText[comment.status]?.label ?? '未知' }}
            </n-tag>
          </div>
          <n-text class="text-sm text-gray-700 block">{{ comment.content }}</n-text>
          <n-text depth="3" class="text-xs mt-2 block">
            {{ (comment.createdAt ?? '').replace('T', ' ').slice(0, 16) }} · 赞 {{ comment.likeCount }}
          </n-text>
        </div>
      </div>
      <n-empty v-else description="还没有发表过评论" class="py-16" />
    </n-spin>
    <div v-if="total > pageSize" class="mt-4 flex justify-center">
      <n-pagination v-model:page="pageNum" :page-size="pageSize" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>