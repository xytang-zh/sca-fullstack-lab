<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NCard,
  NButton,
  NDataTable,
  NInput,
  NModal,
  NForm,
  NFormItem,
  NSpace,
  NTag,
  NPopconfirm,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import { useTable } from '@/hooks/useTable'
import * as userApi from '@/api/system/user'
import type { UserVO, UserCreateDTO, UserUpdateDTO, UserPageQuery } from '@sca/types'

const message = useMessage()
const { loading, list, total, query, fetch, handlePageChange, handlePageSizeChange, handleSearch } =
  useTable<UserVO, UserPageQuery>((q) => userApi.pageUsers(q))

const STATUS_TEXT: Record<number, string> = {
  1: '草稿',
  2: '正常',
  3: '禁用',
  4: '锁定',
  5: '已删除'
}

function statusTagType(status?: number): 'success' | 'warning' | 'error' | 'default' {
  if (status === 2) return 'success'
  if (status === 3) return 'warning'
  if (status === 4) return 'error'
  return 'default'
}

function statusRender(row: UserVO) {
  if (!row.status) return '-'
  return h(NTag, { type: statusTagType(row.status) }, () => STATUS_TEXT[row.status!] ?? row.status)
}

function actionsRender(row: UserVO) {
  return h(NSpace, null, () => [
    h(NButton, { size: 'small', onClick: () => openEdit(row) }, () => '编辑'),
    h(
      NPopconfirm,
      { onPositiveClick: () => handleDelete(row.id) },
      {
        trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除')
      }
    )
  ])
}

const columns: DataTableColumns<UserVO> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'username', width: 120 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '邮箱', key: 'email', width: 200 },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '状态', key: 'status', width: 90, render: statusRender },
  { title: '最后登录', key: 'lastLoginTime', width: 160 },
  { title: '操作', key: 'actions', width: 200, render: actionsRender }
]

const dialog = reactive({
  visible: false,
  isEdit: false,
  loading: false
})
const formRef = ref()
const form = reactive<UserCreateDTO & { id?: number; version?: number }>({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  deptId: undefined
})

function openCreate() {
  dialog.isEdit = false
  Object.assign(form, {
    id: undefined,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    deptId: undefined,
    version: undefined
  })
  dialog.visible = true
}

function openEdit(row: UserVO) {
  dialog.isEdit = true
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email,
    phone: row.phone,
    deptId: row.deptId,
    version: undefined
  })
  dialog.visible = true
}

async function handleSubmit() {
  if (!form.username || (!dialog.isEdit && !form.password)) {
    message.warning('用户名和密码（新增）必填')
    return
  }
  dialog.loading = true
  try {
    if (dialog.isEdit && form.id) {
      const dto: UserUpdateDTO = {
        id: form.id,
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
        deptId: form.deptId
      }
      await userApi.updateUser(form.id, dto)
      message.success('修改成功')
    } else {
      const dto: UserCreateDTO = {
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
        deptId: form.deptId
      }
      await userApi.createUser(dto)
      message.success('新增成功')
    }
    dialog.visible = false
    fetch()
  } finally {
    dialog.loading = false
  }
}

async function handleDelete(id: number) {
  await userApi.deleteUser(id)
  message.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <n-card>
      <n-space>
        <n-input
          v-model:value="query.keyword"
          placeholder="用户名/昵称"
          clearable
          @keyup.enter="handleSearch"
        />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button type="primary" @click="openCreate">新增用户</n-button>
      </n-space>
    </n-card>

    <n-card>
      <n-data-table
        remote
        :columns="columns"
        :data="list"
        :loading="loading"
        :pagination="{
          page: query.pageNum,
          pageSize: query.pageSize,
          itemCount: total,
          showSizePicker: true,
          pageSizes: [10, 20, 50]
        }"
        :row-key="(row) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <n-modal
      v-model:show="dialog.visible"
      preset="card"
      :title="dialog.isEdit ? '编辑用户' : '新增用户'"
      style="width: 480px"
    >
      <n-form ref="formRef" :model="form" label-placement="top">
        <n-form-item label="用户名" required>
          <n-input v-model:value="form.username" :disabled="dialog.isEdit" />
        </n-form-item>
        <n-form-item v-if="!dialog.isEdit" label="密码" required>
          <n-input v-model:value="form.password" type="password" />
        </n-form-item>
        <n-form-item label="昵称">
          <n-input v-model:value="form.nickname" />
        </n-form-item>
        <n-form-item label="邮箱">
          <n-input v-model:value="form.email" />
        </n-form-item>
        <n-form-item label="手机号">
          <n-input v-model:value="form.phone" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space>
          <n-button @click="dialog.visible = false">取消</n-button>
          <n-button type="primary" :loading="dialog.loading" @click="handleSubmit">确认</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
