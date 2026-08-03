<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NCheckbox, useMessage } from 'naive-ui'
import { Person, LockClosed } from '@vicons/ionicons5'
import { useUserStore } from '@/store/user'
import CaptchaModal from '@/components/common/CaptchaModal.vue'
import type { R } from '@sca/types'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const showCaptcha = ref(false)
const captchaRequired = ref(false)

const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

async function handleLogin(e: Event) {
  e.preventDefault()
  if (!form.username || !form.password) {
    message.warning('请填写用户名和密码')
    return
  }
  // 如果没有要求验证码，直接提交登录
  if (!captchaRequired.value) {
    await submitLogin('')
  } else {
    // 需要验证码 → 弹出滑块
    showCaptcha.value = true
  }
}

async function onCaptchaSuccess(checkToken: string) {
  showCaptcha.value = false
  await submitLogin(checkToken)
}

async function submitLogin(checkToken: string) {
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      checkToken,
      rememberMe: form.rememberMe
    })
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch (err: unknown) {
    const bizCode = extractBizCode(err)
    if (bizCode === '01201') {
      // 验证码错误 → 下次登录需要验证码
      captchaRequired.value = true
      showCaptcha.value = true
    } else {
      message.error(extractBizMessage(err))
    }
  } finally {
    loading.value = false
  }
}

function extractBizCode(err: unknown): string | null {
  if (err && typeof err === 'object') {
    const r = err as Partial<R<unknown> & { code?: string }>
    if (r.code) return String(r.code)
    const axiosErr = err as { response?: { data?: Partial<R<unknown> & { code?: string }> } }
    if (axiosErr.response?.data?.code) return String(axiosErr.response.data.code)
  }
  return null
}

function extractBizMessage(err: unknown): string {
  if (err && typeof err === 'object') {
    const r = err as Partial<R<unknown>>
    if (typeof r.message === 'string' && r.message) {
      return r.message
    }
    const axiosErr = err as { response?: { data?: Partial<R<unknown>> } }
    if (axiosErr.response?.data?.message) {
      return axiosErr.response.data.message
    }
  }
  if (err instanceof Error && err.message) {
    return err.message
  }
  return '登录失败'
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">
        <h1>Sca Admin</h1>
        <p>Spring Cloud Alibaba 一体化管理平台</p>
      </div>
      <n-form :model="form" :rules="rules" label-placement="top" size="large">
        <n-form-item path="username">
          <n-input
            v-model:value="form.username"
            placeholder="用户名"
            clearable
            :input-props="{ autocomplete: 'username' }"
          >
            <template #prefix><n-icon :component="Person" /></template>
          </n-input>
        </n-form-item>
        <n-form-item path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            placeholder="密码"
            :input-props="{ autocomplete: 'current-password' }"
            @keyup.enter="handleLogin"
          >
            <template #prefix><n-icon :component="LockClosed" /></template>
          </n-input>
        </n-form-item>
        <n-form-item>
          <n-checkbox v-model:checked="form.rememberMe">7 天内免登录</n-checkbox>
        </n-form-item>
        <n-button
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </n-button>
      </n-form>
    </div>
    <captcha-modal :show="showCaptcha" @success="onCaptchaSuccess" @close="showCaptcha = false" />
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
</style>