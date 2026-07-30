<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NCheckbox, useMessage } from 'naive-ui'
import { Person, LockClosed, Image } from '@vicons/ionicons5'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/store/user'
import type { R } from '@sca/types'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const captchaLoading = ref(false)
const captchaImg = ref('')

const form = reactive({
  username: 'admin',
  password: '',
  captcha: '',
  captchaKey: '',
  rememberMe: false
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' },
  captcha: { required: true, message: '请输入验证码', trigger: 'blur' }
}

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const data = await getCaptcha()
    captchaImg.value = data.captchaImg
    form.captchaKey = data.captchaKey
    form.captcha = ''
  } catch (err) {
    message.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

async function handleLogin(e: Event) {
  e.preventDefault()
  if (!form.username || !form.password || !form.captcha) {
    message.warning('请填写完整登录信息')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      captcha: form.captcha,
      captchaKey: form.captchaKey,
      rememberMe: form.rememberMe
    })
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch (err: unknown) {
    message.error(extractBizMessage(err))
    refreshCaptcha()
  } finally {
    loading.value = false
  }
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

onMounted(refreshCaptcha)
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
            <template #prefix><NIcon :component="Person" /></template>
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
            <template #prefix><NIcon :component="LockClosed" /></template>
          </n-input>
        </n-form-item>
        <n-form-item path="captcha">
          <div class="captcha-row">
            <n-input
              v-model:value="form.captcha"
              placeholder="验证码"
              maxlength="4"
              @keyup.enter="handleLogin"
            >
              <template #prefix><NIcon :component="Image" /></template>
            </n-input>
            <div class="captcha-img" :loading="captchaLoading" @click="refreshCaptcha">
              <img v-if="captchaImg" :src="captchaImg" alt="captcha" />
              <span v-else>点击获取</span>
            </div>
          </div>
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
.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.captcha-img {
  width: 120px;
  height: 40px;
  padding: 2px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  /* overflow: hidden; */
}
.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
