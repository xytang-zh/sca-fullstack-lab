<script setup lang="ts">
/**
 * 公开布局：知乎风格导航栏（NavBar）+ 内容区 + 页脚，博客前台页面使用。
 */
import NavBar from '@/components/NavBar.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 顶部导航：品牌/搜索/登录入口 -->
    <NavBar />
    <!-- 内容区：路由视图出口 -->
    <main class="flex-1">
      <router-view />
    </main>
    <!-- 页脚：版权信息 -->
    <footer class="py-6 text-center">
      <n-text depth="3" class="text-xs">Sca 博客 · 企业级一体化智能管理平台示例</n-text>
    </footer>
  </div>
</template>