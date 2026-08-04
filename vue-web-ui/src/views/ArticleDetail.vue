<script setup lang="ts">
/**
 * 文章详情页：标题/元信息 + Markdown 正文渲染 + 右侧目录导航 + 底部评论区。
 * - 目录由 ArticleMarkdown 渲染时提取，经 useArticleToc 联动高亮
 * - 窄屏（<1024px）自动收起目录侧栏
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MenuOutline } from '@vicons/ionicons5'
import { articleApi } from '@sca/api'
import { useUserStore } from '@/store/user'
import type { ArticleDetailVO } from '@sca/types'
import ArticleMarkdown from '@/components/ArticleMarkdown.vue'
import TocNav from '@/components/TocNav.vue'
import CommentPanel from '@/components/CommentPanel.vue'
import { useArticleToc, type TocItem } from '@/hooks/useArticleToc'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 文章详情数据 */
const article = ref<ArticleDetailVO | null>(null)
/** 加载中标识 */
const loading = ref(false)
/** 目录项列表（ArticleMarkdown 渲染时回传） */
const toc = ref<TocItem[]>([])
/** 目录侧栏是否收起（窄屏默认收起） */
const tocCollapsed = ref(false)
/** 视口是否宽屏（≥1024px） */
const isWide = ref(true)

/** 当前激活章节 id（由 useArticleToc 计算，驱动 TocNav 高亮） */
const { activeId } = useArticleToc(toc)

/** 视口尺寸监听：宽屏阈值 1024px（对应 tailwind lg 断点） */
function onResize() {
  isWide.value = window.innerWidth >= 1024
}

/** 接收 ArticleMarkdown 渲染出的目录项 */
function onTocUpdated(items: TocItem[]) {
  toc.value = items
}

/** 加载文章详情（按路由参数 id） */
async function load() {
  loading.value = true
  try {
    article.value = await articleApi.getArticle(route.params.id as string)
  } finally {
    loading.value = false
  }
}

/** 时间格式化：ISO 时间裁剪为 "YYYY-MM-DD HH:mm" */
function formatTime(value?: string): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(0, 16)
}

/** 登录守卫：未登录携带 redirect 跳登录页 */
function requireLogin(): boolean {
  if (userStore.token) {
    return true
  }
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

/** 点赞/取消点赞：成功后按结果 ±1 更新显示 */
async function toggleLike() {
  if (!article.value || !requireLogin()) {
    return
  }
  const liked = await articleApi.toggleLike(article.value.id)
  article.value.likes += liked ? 1 : -1
}

/** 收藏/取消收藏：成功后按结果 ±1 更新显示 */
async function toggleFavorite() {
  if (!article.value || !requireLogin()) {
    return
  }
  const favorited = await articleApi.toggleFavorite(article.value.id)
  article.value.favorites += favorited ? 1 : -1
}

onMounted(() => {
  onResize()
  tocCollapsed.value = !isWide.value
  window.addEventListener('resize', onResize)
  load()
})

onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>

<template>
  <div class="detail-page">
    <div
      class="detail-layout"
      :class="{ 'detail-layout--collapsed': tocCollapsed }"
    >
      <aside class="toc-sidebar" :class="{ 'toc-sidebar--visible': !tocCollapsed }">
        <div class="toc-sidebar__inner">
          <TocNav :toc="toc" :active-id="activeId" />
        </div>
      </aside>

      <main class="content-col">
        <div class="content-col__inner">
          <n-button quaternary size="small" class="mb-4 px-0" @click="router.back()">
            ← 返回列表
          </n-button>

          <n-spin :show="loading">
            <template v-if="article">
              <h1 class="article-title">{{ article.title }}</h1>

              <div class="article-meta">
                <n-avatar round size="small" class="article-meta__avatar">
                  {{ article.authorId.charAt(0) }}
                </n-avatar>
                <span class="article-meta__author">#{{ article.authorId.slice(0, 8) }}</span>
                <span class="article-meta__divider">/</span>
                <span class="article-meta__tag">发布于 {{ formatTime(article.publishTime) }}</span>
                <span class="article-meta__divider">/</span>
                <span class="article-meta__tag">阅读 {{ article.views }}</span>
              </div>

              <ArticleMarkdown
                :content-md="article.contentMd"
                @toc-updated="onTocUpdated"
              />

              <div class="article-actions">
                <n-button class="article-actions__btn" @click="toggleLike">
                  赞 {{ article.likes }}
                </n-button>
                <n-button class="article-actions__btn" @click="toggleFavorite">
                  收藏 {{ article.favorites }}
                </n-button>
              </div>
            </template>

            <n-empty
              v-else-if="!loading"
              class="py-24"
              description="文章不存在或已下架"
            />
          </n-spin>
        </div>

        <button
          type="button"
          class="toc-toggle"
          :title="tocCollapsed ? '显示目录' : '隐藏目录'"
          @click="tocCollapsed = !tocCollapsed"
        >
          <n-icon :component="MenuOutline" />
        </button>
      </main>

      <aside class="comment-col">
        <div class="comment-col__inner">
          <CommentPanel v-if="article" :article-id="article.id" :article-title="article.title" />
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  padding: 1.5rem 2rem 4rem;
}
.detail-layout {
  max-width: 1440px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2fr) minmax(0, 1fr);
  gap: 2.5rem;
  align-items: start;
  transition: grid-template-columns 0.25s ease;
}
.detail-layout--collapsed {
  grid-template-columns: 0fr minmax(0, 2fr) minmax(0, 1fr);
}

.toc-sidebar {
  min-width: 0;
  overflow: hidden;
}
.toc-sidebar__inner {
  position: sticky;
  top: 88px;
  max-height: calc(100vh - 112px);
  overflow-y: auto;
  padding-right: 0.5rem;
  border-right: 1px solid #f4f4f5;
  padding-top: 1rem;
}

.content-col {
  position: relative;
  min-width: 0;
}
.content-col__inner {
  max-width: 760px;
}
.article-title {
  font-size: 1.9rem;
  font-weight: 700;
  line-height: 1.35;
  color: #18181b;
  margin: 0.25rem 0 1rem;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding-bottom: 1.25rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #f4f4f5;
  flex-wrap: wrap;
}
.article-meta__avatar {
  background: #0d9488;
  color: #fff;
  font-weight: 600;
}
.article-meta__author {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.8rem;
  color: #0d9488;
  font-weight: 600;
}
.article-meta__divider {
  color: #d4d4d8;
}
.article-meta__tag {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.75rem;
  color: #71717a;
}
.article-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #f4f4f5;
}
.article-actions__btn {
  min-width: 7rem;
}

.toc-toggle {
  position: fixed;
  left: 1.25rem;
  bottom: 1.5rem;
  z-index: 30;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  border: none;
  background: #0d9488;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.35);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.toc-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(13, 148, 136, 0.45);
}

.comment-col {
  min-width: 0;
}
.comment-col__inner {
  position: sticky;
  top: 88px;
  max-height: calc(100vh - 112px);
  overflow-y: auto;
  padding-left: 0.5rem;
}

/* 窄屏（<1024px）：左栏改为覆盖层，默认隐藏 */
@media (max-width: 1023px) {
  .detail-layout {
    grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
    transition: none;
  }
  .detail-layout--collapsed {
    grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  }
  .toc-sidebar {
    position: fixed;
    left: 0;
    top: 56px;
    bottom: 0;
    width: 240px;
    z-index: 25;
    background: #fff;
    box-shadow: 0 0 24px rgba(0, 0, 0, 0.12);
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }
  .toc-sidebar--visible {
    transform: translateX(0);
  }
  .toc-sidebar__inner {
    position: static;
    max-height: none;
    height: 100%;
    border-right: none;
    padding: 1.5rem 1.25rem;
  }
}

/* 小屏（<768px）：单列，评论堆叠到内容下方 */
@media (max-width: 767px) {
  .detail-page {
    padding: 1rem 1rem 3rem;
  }
  .detail-layout,
  .detail-layout--collapsed {
    grid-template-columns: minmax(0, 1fr);
    gap: 2rem;
  }
  .comment-col__inner {
    position: static;
    max-height: none;
    padding-left: 0;
  }
  .toc-toggle {
    left: 1rem;
    bottom: 1rem;
  }
}
</style>
