<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createDiscreteApi, type FormInst, type FormRules } from 'naive-ui'
import { useUserStore } from '@/store/user'
import {
  ArrowBackOutline,
  ChatbubbleEllipsesOutline,
  LockClosedOutline,
  PersonOutline,
  ShieldCheckmarkOutline,
  SparklesOutline
} from '@vicons/ionicons5'

const { message } = createDiscreteApi(['message'])

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInst | null>(null)
const form = reactive({ account: '', password: '', confirmPassword: '' })
const submitting = ref(false)

const ACCOUNT_PATTERN = /^[a-zA-Z][a-zA-Z0-9]{5,17}$/

const rules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: ['blur', 'input'] },
    { pattern: ACCOUNT_PATTERN, message: '账号须为 6-18 位，仅含字母与数字，且以字母开头', trigger: ['blur', 'input'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['blur', 'input'] },
    { min: 8, max: 32, message: '密码长度须为 8-32 位', trigger: ['blur', 'input'] }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: ['blur', 'input'] },
    {
      validator: (_rule, value: string) => value === form.password,
      message: '两次输入的密码不一致',
      trigger: ['blur', 'input']
    }
  ]
}

const features = [
  { icon: SparklesOutline, text: '记录微服务与前端工程的落地实践' },
  { icon: ChatbubbleEllipsesOutline, text: '注册后即可评论、点赞与收藏' },
  { icon: ShieldCheckmarkOutline, text: '账号 6-18 位，仅字母与数字' }
]

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await userStore.register({
      account: form.account,
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    message.success('注册成功，欢迎加入')
    router.replace('/dashboard/profile')
  } finally {
    submitting.value = false
  }
}

function goHome() {
  router.push('/')
}

function goLogin() {
  router.push('/login')
}
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

    <!-- 右侧注册卡片 -->
    <div class="flex-1 flex items-center justify-center px-4 py-12">
      <div class="w-full max-w-md">
        <n-button quaternary size="small" class="mb-6 px-0 text-gray-400" @click="goHome">
          <template #icon><n-icon :component="ArrowBackOutline" /></template>
          返回首页
        </n-button>
        <div class="bg-white rounded-2xl shadow-lg p-8">
          <n-text tag="h2" class="text-xl font-bold text-gray-800 mb-1 block text-center">创建账号</n-text>
          <n-text depth="3" class="text-sm mb-6 block text-center">注册后即可开启完整的博客体验</n-text>

          <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
            <n-form-item label="账号" path="account">
              <n-input
                v-model:value="form.account"
                placeholder="6-18 位，字母或数字，以字母开头"
                maxlength="18"
                @keyup.enter="submit"
              >
                <template #prefix><n-icon :component="PersonOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item label="密码" path="password">
              <n-input
                v-model:value="form.password"
                type="password"
                show-password-on="click"
                placeholder="8-32 位"
                @keyup.enter="submit"
              >
                <template #prefix><n-icon :component="LockClosedOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item label="再次输入密码" path="confirmPassword">
              <n-input
                v-model:value="form.confirmPassword"
                type="password"
                show-password-on="click"
                placeholder="请再次输入密码"
                @keyup.enter="submit"
              >
                <template #prefix><n-icon :component="LockClosedOutline" /></template>
              </n-input>
            </n-form-item>
            <n-button type="primary" block size="large" :loading="submitting" class="mt-2" @click="submit">
              注册
            </n-button>
            <div class="mt-3 text-center">
              <n-text depth="3" class="text-sm">已有账号？</n-text>
              <n-button text type="primary" class="text-sm" @click="goLogin">去登录</n-button>
            </div>
          </n-form>
        </div>
      </div>
    </div>
  </div>
</template>
