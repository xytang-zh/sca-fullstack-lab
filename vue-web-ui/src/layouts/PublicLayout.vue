<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <NavBar />
    <main class="flex-1">
      <router-view />
    </main>
    <footer class="py-6 text-center">
      <n-text depth="3" class="text-xs">Sca 博客 · 企业级一体化智能管理平台示例</n-text>
    </footer>
  </div>
</template>