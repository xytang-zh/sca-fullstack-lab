<script setup lang="ts">
import { NButton } from 'naive-ui'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-[60vh]">
    <n-result status="404" title="404" description="页面不存在">
      <template #footer>
        <n-button type="primary" @click="router.replace('/dashboard')">返回工作台</n-button>
      </template>
    </n-result>
  </div>
</template>
