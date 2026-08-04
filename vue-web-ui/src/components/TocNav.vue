<script setup lang="ts">
/**
 * 文章目录导航：展示标题目录（h2 编号、h3 缩进），点击平滑滚动到对应章节。
 */
import { computed } from 'vue'
import type { TocItem } from '@/hooks/useArticleToc'

const props = defineProps<{
  /** 目录项列表（含标题层级与 id） */
  toc: TocItem[]
  /** 当前激活章节 id（由 useArticleToc 计算，用于高亮） */
  activeId: string
}>()

/** 顶层序号（h2 出现顺序，真实阅读顺序），h3 缩进不编号 */
const numbered = computed(() => {
  let n = 0
  return props.toc.map((item) => ({
    ...item,
    no: item.level === 2 ? String(++n).padStart(2, '0') : ''
  }))
})

/** 平滑滚动到指定标题元素 */
function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <!-- 目录容器：标题 + 目录项列表 -->
  <nav class="toc-nav">
    <div class="toc-nav__title">目录</div>
    <ul class="toc-nav__list">
      <!-- 目录项：h3 缩进显示，当前激活项高亮 -->
      <li
        v-for="item in numbered"
        :key="item.id"
        class="toc-nav__item"
        :class="[
          item.level === 3 ? 'toc-nav__item--sub' : '',
          { 'toc-nav__item--active': item.id === props.activeId }
        ]"
      >
        <button type="button" class="toc-nav__link" @click="scrollTo(item.id)">
          <span class="toc-nav__index">{{ item.no }}</span>
          <span class="toc-nav__text">{{ item.text }}</span>
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
/* 目录容器：相对定位，供左侧竖线对齐 */
.toc-nav {
  position: relative;
}
/* 目录标题：等宽字体 + 全大写 + 宽字距的标注风格 */
.toc-nav__title {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.75rem;
  letter-spacing: 0.15em;
  color: #a1a1aa;
  text-transform: uppercase;
  margin-bottom: 0.75rem;
}
/* 目录列表：去除默认列表样式 */
.toc-nav__list {
  list-style: none;
  padding: 0;
  margin: 0;
}
/* 目录项：左侧竖线作为层级/激活指示 */
.toc-nav__item {
  position: relative;
  border-left: 1px solid #e4e4e7;
}
/* 目录链接：基线对齐的序号 + 文本 */
.toc-nav__link {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  width: 100%;
  text-align: left;
  padding: 0.4rem 0.9rem;
  font-size: 0.875rem;
  color: #71717a;
  cursor: pointer;
  transition: color 0.2s;
}
/* 悬停：品牌青色 */
.toc-nav__link:hover {
  color: #0d9488;
}
/* 序号：等宽字体，弱化显示 */
.toc-nav__index {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.75rem;
  color: #a1a1aa;
  flex-shrink: 0;
}
/* 子级（h3）：右移缩进区分层级 */
.toc-nav__item--sub .toc-nav__link {
  padding-left: 2.1rem;
}
/* 子级（h3）：不显示序号 */
.toc-nav__item--sub .toc-nav__index {
  display: none;
}
/* 激活项：左侧竖线变青 */
.toc-nav__item--active {
  border-left-color: #0d9488;
}
/* 激活项：文本变青加粗 */
.toc-nav__item--active .toc-nav__link {
  color: #0d9488;
  font-weight: 600;
}
/* 激活项：序号变青 */
.toc-nav__item--active .toc-nav__index {
  color: #0d9488;
}
</style>
