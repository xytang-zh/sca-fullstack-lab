<script setup lang="ts">
/**
 * 搜索结果页：从 URL query 读取关键字并展示（全文搜索功能开发中，当前为占位）。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
/** 搜索关键字（来自 URL ?q=） */
const keyword = computed(() => (route.query.q as string) ?? '')
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-10">
    <h2 class="text-xl font-semibold text-gray-800 mb-2">搜索「{{ keyword }}」</h2>
    <n-empty description="全文搜索功能开发中，敬请期待" class="py-16" />
  </div>
</template>