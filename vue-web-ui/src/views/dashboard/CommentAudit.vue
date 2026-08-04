<script setup lang="ts">
/**
 * 评论审核页（管理员）：分页展示待审核评论，支持通过/驳回操作。
 */
import { onMounted, ref } from 'vue'
import { createDiscreteApi } from 'naive-ui'
import { commentApi } from '@sca/api'
import type { CommentVO } from '@sca/types'

const { message } = createDiscreteApi(['message'])
/** 待审核评论列表 */
const comments = ref<CommentVO[]>([])
/** 总数（分页用） */
const total = ref(0)
/** 加载中标识 */
const loading = ref(false)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const size = 10

/** 分页加载待审核评论 */
async function load() {
  loading.value = true
  try {
    const data = await commentApi.pendingComments(page.value, size)
    comments.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 审核评论（2=通过 3=驳回），成功后提示并刷新列表 */
async function audit(comment: CommentVO, status: 2 | 3) {
  await commentApi.auditComment(comment.id, { status })
  message.success(status === 2 ? '已通过' : '已驳回')
  load()
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">评论审核</h2>
    <n-spin :show="loading">
      <div v-if="comments.length" class="space-y-3">
        <div
          v-for="comment in comments"
          :key="comment.id"
          class="bg-white rounded-xl border border-gray-100 p-4"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="min-w-0">
              <n-text depth="3" class="text-xs">「{{ comment.nickname }}」评论于文章 #{{ comment.articleId }}</n-text>
              <n-text class="text-sm text-gray-700 block mt-1">{{ comment.content }}</n-text>
              <n-text depth="3" class="text-xs mt-2 block">
                {{ (comment.createdAt ?? '').replace('T', ' ').slice(0, 16) }}
              </n-text>
            </div>
            <div class="flex gap-2 shrink-0">
              <n-button size="tiny" type="primary" @click="audit(comment, 2)">通过</n-button>
              <n-button size="tiny" type="error" quaternary @click="audit(comment, 3)">驳回</n-button>
            </div>
          </div>
        </div>
      </div>
      <n-empty v-else description="暂无待审核评论" class="py-16" />
    </n-spin>
    <div v-if="total > size" class="mt-4 flex justify-center">
      <n-pagination v-model:page="page" :page-size="size" :item-count="total" @update:page="load" />
    </div>
  </div>
</template>