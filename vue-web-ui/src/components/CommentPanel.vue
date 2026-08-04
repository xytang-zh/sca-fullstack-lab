<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { commentApi } from '@sca/api'
import { useUserStore } from '@/store/user'
import type { CommentVO } from '@sca/types'

const { message } = createDiscreteApi(['message'])

const props = defineProps<{
  articleId: string
  articleTitle?: string
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const PAGE_SIZE = 10
const comments = ref<CommentVO[]>([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const newContent = ref('')
const replyTarget = ref<CommentVO | null>(null)
const replyContent = ref('')
const liking = ref<string[]>([])

const isLoggedIn = computed(() => userStore.token !== '')

const grouped = computed(() => {
  const roots: CommentVO[] = []
  const replies: Record<string, CommentVO[]> = {}
  comments.value.forEach((c) => {
    if (c.parentId) {
      ;(replies[c.parentId] ??= []).push(c)
    } else {
      roots.push(c)
    }
  })
  return { roots, replies }
})

const hasMore = computed(() => comments.value.length < total.value)

function formatTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}

function requireLogin(): boolean {
  if (isLoggedIn.value) {
    return true
  }
  message.warning('请先登录后再操作')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function load(append = false) {
  loading.value = true
  try {
    const page = append ? Math.ceil(comments.value.length / PAGE_SIZE) + 1 : 1
    const data = await commentApi.pageComments(props.articleId, {
      page,
      size: PAGE_SIZE
    })
    comments.value = append ? [...comments.value, ...data.records] : data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!requireLogin()) {
    return
  }
  const content = newContent.value.trim()
  if (!content) {
    message.warning('评论内容不能为空')
    return
  }
  submitting.value = true
  try {
    await commentApi.createComment({
      articleId: props.articleId,
      articleTitle: props.articleTitle,
      content,
      nickname: userStore.userInfo?.nickname ?? userStore.userInfo?.username,
      avatar: userStore.userInfo?.avatar
    })
    newContent.value = ''
    message.success('评论发表成功')
    await load()
  } finally {
    submitting.value = false
  }
}

async function submitReply() {
  if (!replyTarget.value) {
    return
  }
  if (!requireLogin()) {
    return
  }
  const content = replyContent.value.trim()
  if (!content) {
    message.warning('回复内容不能为空')
    return
  }
  submitting.value = true
  try {
    await commentApi.replyComment(replyTarget.value.id, {
      articleId: props.articleId,
      articleTitle: props.articleTitle,
      parentId: replyTarget.value.id,
      replyTo: replyTarget.value.nickname,
      content,
      nickname: userStore.userInfo?.nickname ?? userStore.userInfo?.username,
      avatar: userStore.userInfo?.avatar
    })
    replyContent.value = ''
    replyTarget.value = null
    message.success('回复成功')
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggleLike(comment: CommentVO) {
  if (!requireLogin()) {
    return
  }
  if (liking.value.includes(comment.id)) {
    return
  }
  liking.value = [...liking.value, comment.id]
  const prev = { liked: comment.liked, likeCount: comment.likeCount }
  comment.liked = !comment.liked
  comment.likeCount += comment.liked ? 1 : -1
  try {
    const liked = await commentApi.toggleCommentLike(comment.id)
    if (liked !== comment.liked) {
      comment.liked = liked
      comment.likeCount += liked ? 1 : -1
    }
  } catch {
    comment.liked = prev.liked
    comment.likeCount = prev.likeCount
  } finally {
    liking.value = liking.value.filter((id) => id !== comment.id)
  }
}

function startReply(comment: CommentVO) {
  if (!requireLogin()) {
    return
  }
  replyTarget.value = comment
  replyContent.value = ''
}

onMounted(() => load())
</script>

<template>
  <div class="comment-panel">
    <h3 class="comment-panel__title">
      评论
      <span class="comment-panel__count">{{ total }}</span>
    </h3>

    <n-input
      v-model:value="newContent"
      type="textarea"
      :autosize="{ minRows: 3, maxRows: 6 }"
      placeholder="写下你的评论…"
      class="comment-panel__editor"
    />
    <div class="comment-panel__actions">
      <n-button
        type="primary"
        size="small"
        :loading="submitting"
        :disabled="!newContent.trim()"
        @click="submitComment"
      >
        发表评论
      </n-button>
    </div>

    <n-spin :show="loading">
      <div class="comment-panel__list">
        <div v-if="!comments.length && !loading" class="comment-panel__empty">
          暂无评论，来抢沙发
        </div>

        <div v-for="root in grouped.roots" :key="root.id" class="comment-item">
          <div class="comment-item__meta">
            <n-avatar round size="small" :src="root.avatar">
              {{ root.nickname.charAt(0) }}
            </n-avatar>
            <span class="comment-item__nick">{{ root.nickname }}</span>
            <span class="comment-item__time">{{ formatTime(root.createdAt) }}</span>
          </div>
          <p class="comment-item__content">{{ root.content }}</p>
          <div class="comment-item__bar">
            <n-button text size="tiny" @click="startReply(root)">回复</n-button>
            <n-button text size="tiny" :type="root.liked ? 'primary' : 'default'" @click="toggleLike(root)">
              赞 {{ root.likeCount }}
            </n-button>
          </div>

          <div v-if="replyTarget?.id === root.id" class="comment-item__reply-editor">
            <n-input
              v-model:value="replyContent"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              :placeholder="`回复 @${root.nickname}`"
              size="small"
            />
            <div class="comment-item__reply-actions">
              <n-button size="tiny" @click="replyTarget = null">取消</n-button>
              <n-button
                size="tiny"
                type="primary"
                :loading="submitting"
                :disabled="!replyContent.trim()"
                @click="submitReply"
              >
                回复
              </n-button>
            </div>
          </div>

          <div v-if="grouped.replies[root.id]?.length" class="comment-item__replies">
            <div v-for="reply in grouped.replies[root.id]" :key="reply.id" class="reply-item">
              <div class="reply-item__meta">
                <n-avatar round size="small" :src="reply.avatar">
                  {{ reply.nickname.charAt(0) }}
                </n-avatar>
                <span class="reply-item__nick">
                  {{ reply.nickname }}
                  <template v-if="reply.replyTo">
                    <span class="reply-item__at">回复</span>@{{ reply.replyTo }}
                  </template>
                </span>
                <span class="reply-item__time">{{ formatTime(reply.createdAt) }}</span>
              </div>
              <p class="reply-item__content">{{ reply.content }}</p>
              <div class="reply-item__bar">
                <n-button text size="tiny" @click="startReply(reply)">回复</n-button>
                <n-button
                  text
                  size="tiny"
                  :type="reply.liked ? 'primary' : 'default'"
                  @click="toggleLike(reply)"
                >
                  赞 {{ reply.likeCount }}
                </n-button>
              </div>
              <div
                v-if="replyTarget?.id === reply.id"
                class="comment-item__reply-editor"
              >
                <n-input
                  v-model:value="replyContent"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 4 }"
                  :placeholder="`回复 @${reply.nickname}`"
                  size="small"
                />
                <div class="comment-item__reply-actions">
                  <n-button size="tiny" @click="replyTarget = null">取消</n-button>
                  <n-button
                    size="tiny"
                    type="primary"
                    :loading="submitting"
                    :disabled="!replyContent.trim()"
                    @click="submitReply"
                  >
                    回复
                  </n-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <n-button
          v-if="hasMore"
          block
          quaternary
          size="small"
          class="comment-panel__more"
          @click="load(true)"
        >
          加载更多
        </n-button>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.comment-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 0;
}
.comment-panel__title {
  font-size: 1rem;
  font-weight: 600;
  color: #1f2329;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.comment-panel__count {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.75rem;
  color: #a1a1aa;
  background: #f4f4f5;
  border-radius: 999px;
  padding: 0.05rem 0.5rem;
}
.comment-panel__actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0.5rem;
}
.comment-panel__list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.comment-panel__empty {
  color: #a1a1aa;
  font-size: 0.875rem;
  text-align: center;
  padding: 2rem 0;
}
.comment-item {
  border-top: 1px solid #f4f4f5;
  padding-top: 1rem;
}
.comment-item__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.comment-item__nick {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1f2329;
}
.comment-item__time {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.75rem;
  color: #a1a1aa;
}
.comment-item__content {
  font-size: 0.9rem;
  color: #3f3f46;
  line-height: 1.7;
  margin: 0.5rem 0;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-item__bar {
  display: flex;
  gap: 1rem;
}
.comment-item__reply-editor {
  margin-top: 0.6rem;
}
.comment-item__reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.4rem;
}
.comment-item__replies {
  margin: 0.75rem 0 0;
  padding: 0.25rem 0 0.25rem 1rem;
  border-left: 2px solid #e4e4e7;
}
.reply-item {
  padding: 0.6rem 0 0.6rem 0.75rem;
  border-top: 1px dashed #f4f4f5;
}
.reply-item:first-child {
  border-top: none;
}
.reply-item__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.reply-item__nick {
  font-size: 0.8rem;
  font-weight: 600;
  color: #1f2329;
}
.reply-item__at {
  font-size: 0.75rem;
  color: #a1a1aa;
  margin: 0 0.2rem;
}
.reply-item__time {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.7rem;
  color: #a1a1aa;
}
.reply-item__content {
  font-size: 0.85rem;
  color: #3f3f46;
  line-height: 1.6;
  margin: 0.4rem 0;
  white-space: pre-wrap;
  word-break: break-word;
}
.reply-item__bar {
  display: flex;
  gap: 1rem;
}
.comment-panel__more {
  margin-top: 0.5rem;
}
</style>
