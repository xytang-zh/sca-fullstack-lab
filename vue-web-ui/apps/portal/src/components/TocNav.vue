<script setup lang="ts">
import { computed } from 'vue'
import type { TocItem } from '@/hooks/useArticleToc'

const props = defineProps<{
  toc: TocItem[]
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

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <nav class="toc-nav">
    <div class="toc-nav__title">目录</div>
    <ul class="toc-nav__list">
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
.toc-nav {
  position: relative;
}
.toc-nav__title {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.75rem;
  letter-spacing: 0.15em;
  color: #a1a1aa;
  text-transform: uppercase;
  margin-bottom: 0.75rem;
}
.toc-nav__list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.toc-nav__item {
  position: relative;
  border-left: 1px solid #e4e4e7;
}
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
.toc-nav__link:hover {
  color: #0d9488;
}
.toc-nav__index {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.75rem;
  color: #a1a1aa;
  flex-shrink: 0;
}
.toc-nav__item--sub .toc-nav__link {
  padding-left: 2.1rem;
}
.toc-nav__item--sub .toc-nav__index {
  display: none;
}
.toc-nav__item--active {
  border-left-color: #0d9488;
}
.toc-nav__item--active .toc-nav__link {
  color: #0d9488;
  font-weight: 600;
}
.toc-nav__item--active .toc-nav__index {
  color: #0d9488;
}
</style>
