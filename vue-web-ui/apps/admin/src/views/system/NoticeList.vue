<script setup lang="ts">
import { h, onMounted, reactive } from 'vue'
import {
  NCard, NButton, NDataTable, NInput, NModal, NForm, NFormItem, NSpace,
  NPopconfirm, NTag, useMessage, type DataTableColumns
} from 'naive-ui'
import { useTable } from '@/hooks/useTable'
import * as noticeApi from '@/api/system/notice'
import type { NoticeVO, PageQuery } from '@sca/types'

const message = useMessage()
const { loading, list, total, query, fetch, handlePageChange, handlePageSizeChange, handleSearch } =
  useTable<NoticeVO, PageQuery>((q) => noticeApi.pageNotices(q))

function statusTag(status: number) {
  const type = status === 3 ? 'success' : status === 1 ? 'default' : 'warning'
  const text = ['', '草稿', '待发布', '已发布', '已撤回'][status] ?? status
  return h(NTag, { type: type as never }, () => text)
}

const columns: DataTableColumns<NoticeVO> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '标题', key: 'title', width: 220 },
  { title: '状态', key: 'status', width: 100, render: (row) => statusTag(row.status) },
  { title: '创建时间', key: 'createTime', width: 160 },
  { title: '发布时间', key: 'publishTime', width: 160 },
  {
    title: '操作', key: 'actions', width: 280,
    render: (row) => h(NSpace, null, () => [
      h(NButton, { size: 'small', onClick: () => openEdit(row) }, () => '编辑'),
      row.status !== 3
        ? h(NButton, { size: 'small', type: 'success',
            onClick: () => handlePublish(row.id) }, () => '发布')
        : null,
      row.status === 3
        ? h(NButton, { size: 'small', type: 'warning',
            onClick: () => handleRevoke(row.id) }, () => '撤回')
        : null,
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除')
      })
    ])
  }
]

const dialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<NoticeVO>>({
  id: undefined, title: '', content: '', type: 1, status: 1
})

function openCreate() {
  dialog.isEdit = false
  Object.assign(form, { id: undefined, title: '', content: '', type: 1, status: 1 })
  dialog.visible = true
}

function openEdit(row: NoticeVO) {
  dialog.isEdit = true
  Object.assign(form, {
    id: row.id, title: row.title, content: row.content, type: row.type, status: row.status
  })
  dialog.visible = true
}

async function handleSubmit() {
  if (!form.title || !form.content) {
    message.warning('标题与内容必填')
    return
  }
  dialog.loading = true
  try {
    if (dialog.isEdit && form.id) {
      await noticeApi.updateNotice(form.id, form)
      message.success('修改成功')
    } else {
      await noticeApi.createNotice(form)
      message.success('新增成功')
    }
    dialog.visible = false
    fetch()
  } finally { dialog.loading = false }
}

async function handleDelete(id: number) {
  await noticeApi.deleteNotice(id)
  message.success('删除成功')
  fetch()
}

async function handlePublish(id: number) {
  await noticeApi.publishNotice(id)
  message.success('已发布')
  fetch()
}

async function handleRevoke(id: number) {
  await noticeApi.revokeNotice(id)
  message.success('已撤回')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <n-card>
      <n-space>
        <n-input v-model:value="query.keyword" placeholder="标题"
          clearable @keyup.enter="handleSearch" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button type="primary" @click="openCreate">新增通知</n-button>
      </n-space>
    </n-card>
    <n-card>
      <n-data-table remote :columns="columns" :data="list" :loading="loading"
        :pagination="{
          page: query.pageNum, pageSize: query.pageSize, itemCount: total,
          showSizePicker: true, pageSizes: [10, 20, 50]
        }" :row-key="(row) => row.id"
        @update:page="handlePageChange" @update:page-size="handlePageSizeChange" />
    </n-card>
    <n-modal v-model:show="dialog.visible" preset="card"
      :title="dialog.isEdit ? '编辑通知' : '新增通知'" style="width: 640px">
      <n-form :model="form" label-placement="top">
        <n-form-item label="标题" required><n-input v-model:value="form.title" /></n-form-item>
        <n-form-item label="内容" required>
          <n-input v-model:value="form.content" type="textarea" :rows="6" />
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
.page { display: flex; flex-direction: column; gap: 12px; }
</style>
