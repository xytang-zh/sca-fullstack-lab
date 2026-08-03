<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { authApi } from '@sca/api'
import { useUserStore } from '@/store/user'

const { message } = createDiscreteApi(['message'])
const router = useRouter()
const userStore = useUserStore()

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const submitting = ref(false)

async function submit() {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    message.warning('请填写完整')
    return
  }
  if (form.newPassword.length < 8 || form.newPassword.length > 32) {
    message.warning('新密码长度需为 8-32 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  submitting.value = true
  try {
    await authApi.updatePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })
    message.success('密码修改成功，请重新登录')
    userStore.reset()
    router.replace('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="max-w-md mx-auto">
    <n-card title="修改密码" class="rounded-2xl">
      <n-form label-placement="top">
        <n-form-item label="原密码">
          <n-input
            v-model:value="form.oldPassword"
            type="password"
            show-password-on="click"
            placeholder="请输入原密码"
          />
        </n-form-item>
        <n-form-item label="新密码（8-32 位，含字母与数字）">
          <n-input
            v-model:value="form.newPassword"
            type="password"
            show-password-on="click"
            placeholder="请输入新密码"
          />
        </n-form-item>
        <n-form-item label="确认新密码">
          <n-input
            v-model:value="form.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="请再次输入新密码"
          />
        </n-form-item>
        <n-button type="primary" block :loading="submitting" @click="submit">
          确认修改
        </n-button>
      </n-form>
    </n-card>
  </div>
</template>