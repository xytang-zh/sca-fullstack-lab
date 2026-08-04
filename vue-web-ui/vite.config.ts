import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import path from 'node:path'

export default defineConfig({
  // 插件链：Vue SFC 编译 + UnoCSS 原子样式 + API 自动导入 + Naive UI 组件自动注册
  plugins: [
    vue(),
    UnoCSS(),
    // 自动导入 vue/vue-router/pinia/@vueuse 的 API，组件中无需手动 import
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
      dts: 'src/auto-imports.d.ts'
    }),
    // 自动注册 Naive UI 组件（按需引入），并生成组件类型声明
    Components({
      resolvers: [NaiveUiResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    // 路径别名：@ 指向 src，组件内统一用 @/ 导入
    alias: { '@': path.resolve(__dirname, 'src') }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      // 开发代理：/api 请求转发到网关(8080)，解决跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2022',
    sourcemap: false
  }
})
