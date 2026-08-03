<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi, userApi } from '@sca/api'
import type { ArticleVO, UserVO } from '@sca/types'

const router = useRouter()
const profile = ref<UserVO | null>(null)
const articles = ref<ArticleVO[]>([])
const myColumns = ref<{ total: number }>({ total: 0 })

onMounted(async () => {
  profile.value = await userApi.getMyProfile()
  const [articlePage, columnPage] = await Promise.all([
    articleApi.myArticles(1, 5),
    articleApi.myColumns(1, 10)
  ])
  articles.value = articlePage.list
  myColumns.value = { total: columnPage.total }
})

function goDetail(id: string) {
  router.push(`/articles/${id}`)
}
</script>

<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <template v-if="profile">
      <n-card class="rounded-2xl" content-style="padding: 32px">
        <div class="flex items-center gap-6">
          <n-avatar round :size="80" :src="profile.avatar">
            {{ (profile.nickname ?? profile.username).charAt(0) }}
          </n-avatar>
          <div>
            <h1 class="text-2xl font-bold text-gray-900">{{ profile.nickname || profile.username }}</h1>
            <n-text depth="3" class="text-sm mt-1 block">@{{ profile.username }}</n-text>
            <n-text v-if="profile.bio" depth="2" class="text-sm mt-2 block">{{ profile.bio }}</n-text>
          </div>
        </div>
        <div class="flex gap-8 mt-6 pt-6 border-t border-gray-100">
          <div>
            <n-text class="text-lg font-bold text-gray-800 block">{{ profile.followCount ?? 0 }}</n-text>
            <n-text depth="3" class="text-xs">关注</n-text>
          </div>
          <div>
            <n-text class="text-lg font-bold text-gray-800 block">{{ profile.followerCount ?? 0 }}</n-text>
            <n-text depth="3" class="text-xs">粉丝</n-text>
          </div>
          <div>
            <n-text class="text-lg font-bold text-gray-800 block">{{ articles.length }}</n-text>
            <n-text depth="3" class="text-xs">文章</n-text>
          </div>
          <div>
            <n-text class="text-lg font-bold text-gray-800 block">{{ myColumns.total }}</n-text>
            <n-text depth="3" class="text-xs">专栏</n-text>
          </div>
        </div>
      </n-card>

      <n-card title="最近文章" class="rounded-2xl">
        <div v-if="articles.length" class="space-y-3">
          <div
            v-for="article in articles"
            :key="article.id"
            class="flex items-center justify-between cursor-pointer hover:bg-gray-50 px-3 py-2 rounded-lg"
            @click="goDetail(article.id)"
          >
            <n-text class="text-sm font-medium text-gray-800">{{ article.title }}</n-text>
            <n-text depth="3" class="text-xs shrink-0 ml-4">
              {{ (article.publishTime ?? '').replace('T', ' ').slice(0, 10) }}
            </n-text>
          </div>
        </div>
        <n-empty v-else description="还没有发布文章" :show-icon="false" />
      </n-card>
    </template>
  </div>
</template>