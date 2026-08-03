<script setup lang="ts">
import { zhCN, dateZhCN } from 'naive-ui'
import { themeOverrides } from '@sca/ui'
</script>

<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN" :theme-overrides="themeOverrides">
    <router-view />
  </n-config-provider>
</template>
