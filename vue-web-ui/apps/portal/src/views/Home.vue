<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, userApi } from '@sca/api'
import { useUserStore } from '@/store/user'
import type { ArticleVO, ColumnVO, PageVO } from '@sca/types'
import {
  EyeOutline,
  ThumbsUpOutline,
  StarOutline,
  ChatbubbleOutline,
  TimeOutline
} from '@vicons/ionicons5'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tab = computed(() => {
  const t = route.query.tab
  if (t === 'hot' || t === 'follow' || t === 'columns') {
    return t
  }
  return 'time'
})

const sort = ref<'time' | 'hot'>('time')
const articles = ref<ArticleVO[]>([])
const columns = ref<ColumnVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10

const isLoggedIn = computed(() => userStore.token !== '')

async function load() {
  loading.value = true
  try {
    if (tab.value === 'columns') {
      const data: PageVO<ColumnVO> = await articleApi.pageColumns({
        pageNum: pageNum.value,
        pageSize
      })
      columns.value = data.list
      total.value = data.total
      return
    }
    if (tab.value === 'follow') {
      if (!isLoggedIn.value) {
        articles.value = []
        total.value = 0
        return
      }
      const followingPage = await userApi.following(
        userStore.userInfo?.id ?? '',
        1,
        100
      )
      const authorIds = followingPage.list.map((u) => u.id).join(',')
      const data: PageVO<ArticleVO> = await articleApi.pageArticles({
        pageNum: pageNum.value,
        pageSize,
        sort: 'time',
        authorIds
      })
      articles.value = data.list
      total.value = data.total
      return
    }
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

function goColumn(id: string) {
  router.push(`/articles?column=${id}`)
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

function formatTime(value?: string): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(0, 10)
}

watch(tab, () => {
  pageNum.value = 1
  load()
})

onMounted(load)
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-6">
    <!-- 工具条 -->
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-xl font-semibold text-gray-800">
        {{ tab === 'follow' ? '关注' : tab === 'columns' ? '专栏' : '文章' }}
      </h2>
      <n-radio-group v-if="tab === 'time' || tab === 'hot'" :value="sort" @update:value="changeSort">
        <n-radio-button value="time">最新</n-radio-button>
        <n-radio-button value="hot">最热</n-radio-button>
      </n-radio-group>
    </div>

    <!-- 关注未登录引导 -->
    <div v-if="tab === 'follow' && !isLoggedIn" class="py-16 text-center">
      <n-empty description="登录后查看你关注的内容" class="mb-4">
        <template #extra>
          <n-button type="primary" @click="goLogin">立即登录</n-button>
        </template>
      </n-empty>
    </div>

    <!-- 加载骨架 -->
    <div v-else-if="loading" class="space-y-4">
      <div v-for="i in 4" :key="i" class="bg-white rounded-xl border border-gray-100 p-5">
        <n-skeleton text :repeat="1" width="55%" class="mb-3" />
        <n-skeleton text :repeat="2" width="90%" class="mb-4" />
        <n-skeleton text :repeat="1" width="35%" />
      </div>
    </div>

    <!-- 专栏列表 -->
    <div v-else-if="tab === 'columns'" class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div
        v-for="column in columns"
        :key="column.id"
        class="bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-all cursor-pointer p-5"
        @click="goColumn(column.id)"
      >
        <h3 class="text-lg font-semibold text-gray-800">{{ column.name }}</h3>
        <n-text v-if="column.description" depth="2" class="text-sm line-clamp-2 mt-2 block">
          {{ column.description }}
        </n-text>
        <div class="flex items-center gap-4 text-xs text-gray-400 mt-4">
          <span>{{ column.articleCount }} 篇文章</span>
          <span>{{ column.subscribeCount }} 订阅</span>
        </div>
      </div>
    </div>

    <!-- 文章卡片 -->
    <div v-else-if="articles.length > 0" class="space-y-4">
      <div
        v-for="article in articles"
        :key="article.id"
        class="group flex bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all cursor-pointer overflow-hidden"
        @click="goDetail(article.id)"
      >
        <div class="w-1 shrink-0 bg-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity" />
        <div class="flex-1 p-5">
          <h3 class="text-lg font-semibold text-gray-800 group-hover:text-indigo-600 transition-colors">
            {{ article.title }}
          </h3>
          <n-text v-if="article.summary" depth="2" class="text-sm line-clamp-2 mt-2 block">
            {{ article.summary }}
          </n-text>
          <div class="flex flex-wrap items-center gap-x-5 gap-y-1 text-xs text-gray-400 mt-4">
            <div class="flex items-center gap-1">
              <n-icon :component="TimeOutline" />
              <n-text depth="3">{{ formatTime(article.publishTime) }}</n-text>
            </div>
            <div class="flex items-center gap-1">
              <n-icon :component="EyeOutline" />
              <n-text depth="3">{{ article.views }}</n-text>
            </div>
            <div class="flex items-center gap-1">
              <n-icon :component="ThumbsUpOutline" />
              <n-text depth="3">{{ article.likes }}</n-text>
            </div>
            <div class="flex items-center gap-1">
              <n-icon :component="StarOutline" />
              <n-text depth="3">{{ article.favorites }}</n-text>
            </div>
            <div class="flex items-center gap-1">
              <n-icon :component="ChatbubbleOutline" />
              <n-text depth="3">{{ article.comments }}</n-text>
            </div>
          </div>
        </div>
      </div>
    </div>

    <n-empty v-else description="暂无内容，敬请期待" class="py-16" />

    <div v-if="total > pageSize" class="mt-8 flex justify-center">
      <n-pagination
        v-model:page="pageNum"
        :page-size="pageSize"
        :item-count="total"
        @update:page="load"
      />
    </div>
  </div>
</template>