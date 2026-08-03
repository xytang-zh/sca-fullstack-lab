<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { authApi } from '@sca/api'
import type { CaptchaVO } from '@sca/types'
import { ImageOutline, LockClosedOutline, PersonOutline } from '@vicons/ionicons5'

const { message } = createDiscreteApi(['message'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

type Mode = 'login' | 'register'
const mode = ref<Mode>('login')
const captcha = ref<CaptchaVO | null>(null)
const submitting = ref(false)

const loginForm = reactive({ account: '', password: '', captchaCode: '', rememberMe: false })
const registerForm = reactive({ account: '', password: '', confirmPassword: '' })

const ACCOUNT_PATTERN = /^[a-zA-Z][a-zA-Z0-9]{5,17}$/

async function loadCaptcha() {
  try {
    captcha.value = await authApi.getCaptcha()
    loginForm.captchaCode = ''
  } catch {
    captcha.value = null
  }
}

function switchMode(next: Mode) {
  mode.value = next
  if (next === 'login') {
    loadCaptcha()
  }
}

async function submitLogin() {
  if (!loginForm.account) {
    message.error('请输入账号')
    return
  }
  if (!loginForm.password) {
    message.error('请输入密码')
    return
  }
  if (!captcha.value) {
    message.error('验证码加载失败，请点击图片刷新')
    return
  }
  if (!loginForm.captchaCode) {
    message.error('请输入验证码')
    return
  }
  submitting.value = true
  try {
    await userStore.loginByAccount({
      account: loginForm.account,
      password: loginForm.password,
      captchaKey: captcha.value.captchaKey,
      captchaCode: loginForm.captchaCode,
      rememberMe: loginForm.rememberMe
    })
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch {
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  if (!registerForm.account) {
    message.error('请输入账号')
    return
  }
  if (!ACCOUNT_PATTERN.test(registerForm.account)) {
    message.error('账号须为 6-18 位，仅含字母与数字，且以字母开头')
    return
  }
  if (!registerForm.password) {
    message.error('请输入密码')
    return
  }
  if (registerForm.password.length < 8 || registerForm.password.length > 32) {
    message.error('密码长度须为 8-32 位')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await userStore.register({
      account: registerForm.account,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword
    })
    router.replace('/')
  } finally {
    submitting.value = false
  }
}

watch(
  () => route.query.mode,
  (val) => {
    if (val === 'register') {
      mode.value = 'register'
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (mode.value === 'login') {
    loadCaptcha()
  }
})
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 登录态卡片 -->
      <template v-if="mode === 'login'">
        <div class="login-title">
          <h1>个人博客</h1>
          <p>登录后开启完整的博客体验</p>
        </div>
        <n-form label-placement="top" size="large">
          <n-form-item label="账号">
            <n-input
              v-model:value="loginForm.account"
              placeholder="请输入账号"
              maxlength="18"
              :input-props="{ autocomplete: 'username' }"
              @keyup.enter="submitLogin"
            >
              <template #prefix><n-icon :component="PersonOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item label="密码">
            <n-input
              v-model:value="loginForm.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
              :input-props="{ autocomplete: 'current-password' }"
              @keyup.enter="submitLogin"
            >
              <template #prefix><n-icon :component="LockClosedOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item label="验证码">
            <div class="flex w-full gap-2">
              <n-input
                v-model:value="loginForm.captchaCode"
                placeholder="请输入验证码"
                maxlength="4"
                class="flex-1"
                @keyup.enter="submitLogin"
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
          <n-form-item>
            <n-checkbox v-model:checked="loginForm.rememberMe">7 天免登录</n-checkbox>
          </n-form-item>
          <n-button type="primary" block size="large" :loading="submitting" @click="submitLogin">
            登 录
          </n-button>
        </n-form>
        <div class="login-switch">
          <n-text depth="3" class="text-sm">还没有账号？</n-text>
          <n-button text type="primary" class="text-sm" @click="switchMode('register')">
            立即注册
          </n-button>
        </div>
      </template>

      <!-- 注册态卡片 -->
      <template v-else>
        <div class="login-title">
          <h1>注册账号</h1>
          <p>注册后开启完整的博客体验</p>
        </div>
        <n-form label-placement="top" size="large">
          <n-form-item label="账号">
            <n-input
              v-model:value="registerForm.account"
              placeholder="6-18 位，字母或数字，以字母开头"
              maxlength="18"
              :input-props="{ autocomplete: 'username' }"
              @keyup.enter="submitRegister"
            >
              <template #prefix><n-icon :component="PersonOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item label="密码">
            <n-input
              v-model:value="registerForm.password"
              type="password"
              show-password-on="click"
              placeholder="8-32 位"
              :input-props="{ autocomplete: 'new-password' }"
              @keyup.enter="submitRegister"
            >
              <template #prefix><n-icon :component="LockClosedOutline" /></template>
            </n-input>
          </n-form-item>
          <n-form-item label="确认密码">
            <n-input
              v-model:value="registerForm.confirmPassword"
              type="password"
              show-password-on="click"
              placeholder="请再次输入密码"
              :input-props="{ autocomplete: 'new-password' }"
              @keyup.enter="submitRegister"
            >
              <template #prefix><n-icon :component="LockClosedOutline" /></template>
            </n-input>
          </n-form-item>
          <n-button type="primary" block size="large" :loading="submitting" @click="submitRegister">
            注 册
          </n-button>
        </n-form>
        <div class="login-switch">
          <n-text depth="3" class="text-sm">已有账号？</n-text>
          <n-button text type="primary" class="text-sm" @click="switchMode('login')">
            去登录
          </n-button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  max-width: calc(100vw - 32px);
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
}
.login-title h1 {
  margin: 0;
  font-size: 28px;
  color: #1f2937;
}
.login-title p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}
.login-switch {
  margin-top: 16px;
  text-align: center;
}
</style>