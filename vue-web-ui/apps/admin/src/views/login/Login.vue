<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NCheckbox, useMessage } from 'naive-ui'
import { ImageOutline, LockClosed, Person } from '@vicons/ionicons5'
import { useUserStore } from '@/store/user'
import { getCaptcha } from '@/api/auth'
import type { CaptchaVO, R } from '@sca/types'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const captcha = ref<CaptchaVO | null>(null)

const form = reactive({
  account: '',
  password: '',
  captchaCode: '',
  rememberMe: false
})

const rules = {
  account: { required: true, message: '请输入账号', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' },
  captchaCode: { required: true, message: '请输入验证码', trigger: 'blur' }
}

async function loadCaptcha() {
  try {
    captcha.value = await getCaptcha()
    form.captchaCode = ''
  } catch {
    captcha.value = null
  }
}

async function handleLogin(e: Event) {
  e.preventDefault()
  if (!form.account || !form.password) {
    message.warning('请填写账号和密码')
    return
  }
  if (!captcha.value) {
    message.warning('验证码加载失败，请点击图片刷新')
    return
  }
  if (!form.captchaCode) {
    message.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      account: form.account,
      password: form.password,
      captchaKey: captcha.value.captchaKey,
      captchaCode: form.captchaCode,
      rememberMe: form.rememberMe
    })
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch (err: unknown) {
    message.error(extractBizMessage(err))
    // 登录失败后刷新验证码
    await loadCaptcha()
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

onMounted(loadCaptcha)
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-600 to-violet-600 relative overflow-hidden">
    <div class="absolute -top-24 -left-24 w-80 h-80 rounded-full bg-white/10" />
    <div class="absolute -bottom-32 -right-16 w-96 h-96 rounded-full bg-white/5" />
    <div class="relative w-[420px] p-10 bg-white rounded-2xl shadow-2xl">
      <div class="text-center mb-6">
        <n-text class="text-xs tracking-widest text-indigo-500 mb-2 block">SCA FULLSTACK LAB</n-text>
        <n-text tag="h1" class="m-0 text-[28px] font-bold text-gray-800 block">Sca Admin</n-text>
        <n-text depth="3" class="m-0 mt-1.5 text-[13px] block">Spring Cloud Alibaba 一体化管理平台</n-text>
      </div>
      <n-form :model="form" :rules="rules" label-placement="top" size="large">
        <n-form-item path="account">
          <n-input
            v-model:value="form.account"
            placeholder="账号"
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
        <n-form-item path="captchaCode">
          <div class="flex w-full gap-2">
            <n-input
              v-model:value="form.captchaCode"
              placeholder="验证码"
              maxlength="4"
              class="flex-1"
              @keyup.enter="handleLogin"
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
