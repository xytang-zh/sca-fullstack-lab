<script setup lang="ts">
import { defineComponent } from 'vue'
import {
  NConfigProvider,
  NMessageProvider,
  NDialogProvider,
  NLoadingBarProvider,
  zhCN,
  dateZhCN,
  useMessage,
  useDialog,
  useLoadingBar
} from 'naive-ui'
import { registerMessageHandler } from '@sca/api'

const GlobalMessageBridge = defineComponent({
  name: 'GlobalMessageBridge',
  setup() {
    const message = useMessage()
    const dialog = useDialog()
    const loadingBar = useLoadingBar()
    registerMessageHandler((content, type) => {
      if (type === 'error') message.error(content)
      else if (type === 'warning') message.warning(content)
      else message.info(content)
    })
    void dialog
    void loadingBar
    return () => null
  }
})
</script>

<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN">
    <n-loading-bar-provider>
      <n-message-provider>
        <n-dialog-provider>
          <GlobalMessageBridge />
          <router-view />
        </n-dialog-provider>
      </n-message-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>
