<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { authApi } from '@sca/api'
import type { CaptchaVO } from '@sca/types'
import {
  ArrowBackOutline,
  ChatbubbleEllipsesOutline,
  ImageOutline,
  LockClosedOutline,
  PersonOutline,
  ShieldCheckmarkOutline,
  SparklesOutline
} from '@vicons/ionicons5'

const { message } = createDiscreteApi(['message'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive({ account: '', password: '', captchaCode: '' })
const captcha = ref<CaptchaVO | null>(null)
const submitting = ref(false)

const features = [
  { icon: SparklesOutline, text: '记录微服务与前端工程的落地实践' },
  { icon: ChatbubbleEllipsesOutline, text: '登录后即可评论、点赞与收藏' },
  { icon: ShieldCheckmarkOutline, text: '账号密码 + 文字验证码安全登录' }
]

async function loadCaptcha() {
  try {
    captcha.value = await authApi.getCaptcha()
    form.captchaCode = ''
  } catch {
    captcha.value = null
  }
}

async function submit() {
  if (!form.account) {
    message.error('请输入账号')
    return
  }
  if (!form.password) {
    message.error('请输入密码')
    return
  }
  if (!captcha.value) {
    message.error('验证码加载失败，请点击图片刷新')
    return
  }
  if (!form.captchaCode) {
    message.error('请输入验证码')
    return
  }
  submitting.value = true
  try {
    await userStore.loginByAccount({
      account: form.account,
      password: form.password,
      captchaKey: captcha.value.captchaKey,
      captchaCode: form.captchaCode
    })
    const redirect = (route.query.redirect as string) || '/dashboard/profile'
    router.replace(redirect)
  } catch {
    // 错误提示由 axios 拦截器统一弹出，失败后刷新验证码
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}

function goHome() {
  router.push('/')
}

function goRegister() {
  router.push('/register')
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="min-h-screen flex bg-gray-50">
    <!-- 左侧品牌区 -->
    <div
      class="hidden lg:flex flex-1 flex-col justify-center px-16 bg-gradient-to-br from-indigo-600 to-violet-600 text-white relative overflow-hidden"
    >
      <div class="absolute -top-20 -left-20 w-72 h-72 rounded-full bg-white/10" />
      <div class="absolute -bottom-28 right-0 w-80 h-80 rounded-full bg-white/5" />
      <div class="relative">
        <n-text class="text-indigo-200 text-sm tracking-widest mb-4 block">SCA FULLSTACK LAB</n-text>
        <n-text class="text-4xl font-bold mb-4 block">Sca 博客</n-text>
        <n-text class="text-indigo-100 text-lg leading-8 max-w-md mb-10 block">
          记录与分享的开放空间。游客可自由浏览文章，登录后即可评论、点赞、收藏与发布。
        </n-text>
        <n-list :bordered="false" class="bg-transparent space-y-4">
          <n-list-item v-for="feature in features" :key="feature.text" class="!p-0 !border-none text-indigo-50">
            <template #prefix>
              <span class="flex items-center justify-center w-8 h-8 rounded-full bg-white/15">
                <n-icon :component="feature.icon" />
              </span>
            </template>
            {{ feature.text }}
          </n-list-item>
        </n-list>
      </div>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="flex-1 flex items-center justify-center px-4 py-12">
      <div class="w-full max-w-md">
        <n-button quaternary size="small" class="mb-6 px-0 text-gray-400" @click="goHome">
          <template #icon><n-icon :component="ArrowBackOutline" /></template>
          返回首页
        </n-button>
        <div class="bg-white rounded-2xl shadow-lg p-8">
          <n-text tag="h2" class="text-xl font-bold text-gray-800 mb-1 block text-center">欢迎回来</n-text>
          <n-text depth="3" class="text-sm mb-6 block text-center">登录后开启完整的博客体验</n-text>

          <n-form label-placement="top">
            <n-form-item label="账号">
              <n-input
                v-model:value="form.account"
                placeholder="请输入账号"
                maxlength="18"
                @keyup.enter="submit"
              >
                <template #prefix><n-icon :component="PersonOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item label="密码">
              <n-input
                v-model:value="form.password"
                type="password"
                show-password-on="click"
                placeholder="请输入密码"
                @keyup.enter="submit"
              >
                <template #prefix><n-icon :component="LockClosedOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item label="验证码">
              <div class="flex w-full gap-2">
                <n-input
                  v-model:value="form.captchaCode"
                  placeholder="请输入验证码"
                  maxlength="4"
                  class="flex-1"
                  @keyup.enter="submit"
                >
                  <template #prefix><n-icon :component="ImageOutline" /></template>
                </n-input>
                <n-image
                  v-if="captcha"
                  :src="captcha.imageBase64"
                  alt="验证码"
                  preview-disabled
                  class="w-24 h-10 cursor-pointer shrink-0 rounded"
                  object-fit="cover"
                  @click="loadCaptcha"
                />
              </div>
            </n-form-item>
            <n-button type="primary" block size="large" :loading="submitting" class="mt-2" @click="submit">
              登录
            </n-button>
            <div class="mt-3 text-center">
              <n-text depth="3" class="text-sm">还没有账号？</n-text>
              <n-button text type="primary" class="text-sm" @click="goRegister">立即注册</n-button>
            </div>
          </n-form>
        </div>
      </div>
    </div>
  </div>
</template>
