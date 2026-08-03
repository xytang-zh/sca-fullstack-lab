<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { authApi } from '@sca/api'
import CaptchaSlider from '@/components/CaptchaSlider.vue'

const { message } = createDiscreteApi(['message'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tab = ref<'sms' | 'password'>('sms')
const smsForm = reactive({ phone: '', code: '' })
const pwdForm = reactive({ phone: '', password: '' })
const submitting = ref(false)
const countdown = ref(0)
const sliderVisible = ref(false)
const sending = ref(false)

let countdownTimer: number | undefined

function startCountdown() {
  countdown.value = 60
  clearTimeout(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

function openSlider() {
  if (countdown.value > 0 || sending.value) {
    return
  }
  if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    message.error('请输入正确的手机号')
    return
  }
  sliderVisible.value = true
}

async function handleCaptchaSuccess(checkToken: string) {
  sending.value = true
  try {
    await authApi.sendSmsCode({ phone: smsForm.phone, checkToken })
    startCountdown()
  } finally {
    sending.value = false
  }
}

async function submitSms() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    message.error('请输入正确的手机号')
    return
  }
  if (smsForm.code.length !== 6) {
    message.error('请输入 6 位验证码')
    return
  }
  submitting.value = true
  try {
    await userStore.loginBySms({ phone: smsForm.phone, code: smsForm.code })
    finishLogin()
  } finally {
    submitting.value = false
  }
}

async function submitPassword() {
  if (!/^1[3-9]\d{9}$/.test(pwdForm.phone)) {
    message.error('请输入正确的手机号')
    return
  }
  if (!pwdForm.password) {
    message.error('请输入密码')
    return
  }
  submitting.value = true
  try {
    await userStore.loginByPassword({
      username: pwdForm.phone,
      password: pwdForm.password
    })
    finishLogin()
  } finally {
    submitting.value = false
  }
}

function finishLogin() {
  const redirect = (route.query.redirect as string) || '/profile'
  router.replace(redirect)
}

function goHome() {
  router.push('/')
}

onUnmounted(() => {
  clearInterval(countdownTimer)
})
</script>

<template>
  <div class="min-h-screen flex bg-gray-50">
    <!-- 左侧品牌区 -->
    <div class="hidden lg:flex flex-1 flex-col justify-center px-16 bg-gradient-to-br from-blue-600 to-indigo-700 text-white">
      <h1 class="text-4xl font-bold mb-4">Sca 博客</h1>
      <p class="text-blue-100 text-lg leading-8 max-w-md">
        记录与分享的开放空间。游客可自由浏览文章，登录后即可评论、点赞、收藏与发布。
      </p>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="flex-1 flex items-center justify-center px-4 py-12">
      <div class="w-full max-w-md">
        <button class="text-sm text-gray-400 hover:text-gray-600 mb-6" @click="goHome">
          ← 返回首页
        </button>
        <div class="bg-white rounded-2xl shadow-lg p-8">
          <h2 class="text-xl font-bold text-gray-800 mb-6 text-center">欢迎回来</h2>

          <n-tabs v-model:value="tab" type="line" justify-content="center" class="mb-6">
            <n-tab-pane name="sms" tab="验证码登录" />
            <n-tab-pane name="password" tab="密码登录" />
          </n-tabs>

          <!-- 验证码登录 -->
          <n-form v-if="tab === 'sms'" label-placement="top">
            <n-form-item label="手机号">
              <n-input v-model:value="smsForm.phone" placeholder="请输入手机号" maxlength="11" />
            </n-form-item>
            <n-form-item label="验证码">
              <div class="flex w-full gap-2">
                <n-input
                  v-model:value="smsForm.code"
                  placeholder="6 位验证码"
                  maxlength="6"
                  @keyup.enter="submitSms"
                />
                <n-button :loading="sending" :disabled="countdown > 0" @click="openSlider">
                  {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                </n-button>
              </div>
            </n-form-item>
            <n-button
              type="primary"
              block
              size="large"
              :loading="submitting"
              class="mt-2"
              @click="submitSms"
            >
              登录/注册
            </n-button>
            <p class="text-xs text-gray-400 mt-3 text-center">
              未注册的手机号验证通过后将自动注册并登录
            </p>
          </n-form>

          <!-- 密码登录 -->
          <n-form v-else label-placement="top">
            <n-form-item label="手机号">
              <n-input v-model:value="pwdForm.phone" placeholder="请输入手机号" maxlength="11" />
            </n-form-item>
            <n-form-item label="密码">
              <n-input
                v-model:value="pwdForm.password"
                type="password"
                show-password-on="click"
                placeholder="请输入密码"
                @keyup.enter="submitPassword"
              />
            </n-form-item>
            <n-button
              type="primary"
              block
              size="large"
              :loading="submitting"
              class="mt-2"
              @click="submitPassword"
            >
              登录
            </n-button>
          </n-form>
        </div>
      </div>
    </div>

    <!-- 滑块验证码弹窗 -->
    <CaptchaSlider v-model:show="sliderVisible" @success="handleCaptchaSuccess" />
  </div>
</template>
