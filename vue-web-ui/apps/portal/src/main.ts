import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { registerMessageHandler } from '@sca/api'
import { createDiscreteApi } from 'naive-ui'
import 'uno.css'

const { message } = createDiscreteApi(['message'])

registerMessageHandler((content, type = 'error') => {
  if (type === 'info') {
    message.info(content)
  } else if (type === 'warning') {
    message.warning(content)
  } else {
    message.error(content)
  }
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
