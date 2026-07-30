<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NCard, NButton, NDataTable, NModal, NForm, NFormItem, NSpace, NInput,
  NPopconfirm, useMessage, type DataTableColumns
} from 'naive-ui'
import * as deptApi from '@/api/system/dept'
import type { DeptVO } from '@sca/types'

const message = useMessage()
const loading = ref(false)
const list = ref<DeptVO[]>([])

async function fetch() {
  loading.value = true
  try { list.value = await deptApi.getDeptTree() } finally { loading.value = false }
}

const columns: DataTableColumns<DeptVO> = [
  { title: '部门名称', key: 'name', width: 240 },
  { title: '负责人', key: 'leader', width: 120 },
  { title: '排序', key: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 80,
    render: (row) => row.status === 2 ? '正常' : '禁用' },
  { title: '操作', key: 'actions', width: 220,
    render: (row) => h(NSpace, null, () => [
      h(NButton, { size: 'small', onClick: () => openEdit(row) }, () => '编辑'),
      h(NButton, { size: 'small', type: 'primary',
        onClick: () => openCreate(row.id) }, () => '新增子部门'),
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除')
      })
    ])
  }
]

const rowKey = (row: DeptVO) => row.id

const dialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<DeptVO>>({
  id: undefined, parentId: 0, name: '', leader: '', sort: 0, status: 2
})

function openCreate(parentId = 0) {
  dialog.isEdit = false
  Object.assign(form, { id: undefined, parentId, name: '', leader: '', sort: 0, status: 2 })
  dialog.visible = true
}

function openEdit(row: DeptVO) {
  dialog.isEdit = true
  Object.assign(form, {
    id: row.id, parentId: row.parentId, name: row.name,
    leader: row.leader, sort: row.sort, status: row.status
  })
  dialog.visible = true
}

async function handleSubmit() {
  if (!form.name) {
    message.warning('部门名称必填')
    return
  }
  dialog.loading = true
  try {
    if (dialog.isEdit && form.id) {
      await deptApi.updateDept(form.id, form)
      message.success('修改成功')
    } else {
      await deptApi.createDept(form)
      message.success('新增成功')
    }
    dialog.visible = false
    fetch()
  } finally { dialog.loading = false }
}

async function handleDelete(id: number) {
  await deptApi.deleteDept(id)
  message.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <n-card>
      <n-space>
        <n-button type="primary" @click="openCreate(0)">新增根部门</n-button>
        <n-button @click="fetch">刷新</n-button>
      </n-space>
    </n-card>
    <n-card>
      <n-data-table :columns="columns" :data="list" :loading="loading"
        :row-key="rowKey" default-expand-all />
    </n-card>
    <n-modal v-model:show="dialog.visible" preset="card"
      :title="dialog.isEdit ? '编辑部门' : '新增部门'" style="width: 480px">
      <n-form :model="form" label-placement="top">
        <n-form-item label="部门名称" required><n-input v-model:value="form.name" /></n-form-item>
        <n-form-item label="负责人"><n-input v-model:value="form.leader" /></n-form-item>
        <n-form-item label="排序"><n-input :value="form.sort !== undefined ? String(form.sort) : ''" @update:value="(v: string) => form.sort = Number(v) || 0" /></n-form-item>
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
.page { display: flex; flex-direction: column; gap: 12px; }
</style>
