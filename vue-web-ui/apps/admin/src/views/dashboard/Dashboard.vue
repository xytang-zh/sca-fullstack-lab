<script setup lang="ts">
import { computed } from 'vue'
import { NCard, NGrid, NGridItem, NStatistic } from 'naive-ui'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const nickname = computed(() => userStore.userInfo?.nickname ?? userStore.userInfo?.username ?? '管理员')
</script>

<template>
  <div class="dashboard">
    <n-card class="welcome">
      <div class="welcome-text">
        <n-text class="block text-lg font-bold" :style="{ color: '#fff' }">
          {{ greeting }}，{{ nickname }}！
        </n-text>
        <n-text class="block" :style="{ color: 'rgba(255,255,255,0.85)' }">
          欢迎使用 Spring Cloud Alibaba 一体化管理平台 MVP。
        </n-text>
      </div>
    </n-card>

    <n-grid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <n-grid-item span="4 m:2 l:1">
        <n-card>
          <n-statistic label="系统模块" :value="17">
            <template #suffix>个公共模块</template>
          </n-statistic>
        </n-card>
      </n-grid-item>
      <n-grid-item span="4 m:2 l:1">
        <n-card>
          <n-statistic label="业务服务" :value="11">
            <template #suffix>个微服务</template>
          </n-statistic>
        </n-card>
      </n-grid-item>
      <n-grid-item span="4 m:2 l:1">
        <n-card>
          <n-statistic label="技术栈" :value="20">
            <template #suffix>项</template>
          </n-statistic>
        </n-card>
      </n-grid-item>
      <n-grid-item span="4 m:2 l:1">
        <n-card>
          <n-statistic label="权限点" :value="userStore.perms.length">
            <template #suffix>个</template>
          </n-statistic>
        </n-card>
      </n-grid-item>
    </n-grid>

    <n-card title="快速入口" class="quick-links">
      <div class="links">
        <router-link to="/system/users">用户管理</router-link>
        <router-link to="/system/roles">角色管理</router-link>
        <router-link to="/system/menus">菜单管理</router-link>
        <router-link to="/system/depts">部门管理</router-link>
        <router-link to="/system/dicts">字典管理</router-link>
        <router-link to="/system/params">参数管理</router-link>
        <router-link to="/system/notices">通知公告</router-link>
      </div>
    </n-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.welcome {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.welcome-text h2 {
  margin: 0 0 8px;
  color: #fff;
}
.welcome-text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
}
.quick-links .links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.quick-links a {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  color: #1f2937;
  text-decoration: none;
  font-size: 14px;
}
.quick-links a:hover {
  border-color: #667eea;
  color: #667eea;
}
</style>
