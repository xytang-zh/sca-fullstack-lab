<script setup lang="ts">
import { h, onMounted, reactive } from 'vue'
import {
  NCard, NButton, NDataTable, NInput, NModal, NForm, NFormItem, NSpace,
  NPopconfirm, useMessage, type DataTableColumns
} from 'naive-ui'
import { useTable } from '@/hooks/useTable'
import * as dictApi from '@/api/system/dict'
import type { DictVO, PageQuery } from '@sca/types'

const message = useMessage()
const { loading, list, total, query, fetch, handlePageChange, handlePageSizeChange, handleSearch } =
  useTable<DictVO, PageQuery>((q) => dictApi.pageDicts(q))

const columns: DataTableColumns<DictVO> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '字典类型', key: 'type', width: 140 },
  { title: '字典标签', key: 'label', width: 140 },
  { title: '字典值', key: 'value', width: 140 },
  { title: '排序', key: 'sort', width: 80 },
  { title: '备注', key: 'remark', width: 200 },
  {
    title: '操作', key: 'actions', width: 200,
    render: (row) => h(NSpace, null, () => [
      h(NButton, { size: 'small', onClick: () => openEdit(row) }, () => '编辑'),
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除')
      })
    ])
  }
]

const dialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<DictVO>>({
  id: undefined, type: '', label: '', value: '', sort: 0, remark: ''
})

function openCreate() {
  dialog.isEdit = false
  Object.assign(form, { id: undefined, type: '', label: '', value: '', sort: 0, remark: '' })
  dialog.visible = true
}

function openEdit(row: DictVO) {
  dialog.isEdit = true
  Object.assign(form, {
    id: row.id, type: row.type, label: row.label, value: row.value,
    sort: row.sort, remark: row.remark
  })
  dialog.visible = true
}

async function handleSubmit() {
  if (!form.type || !form.label || !form.value) {
    message.warning('类型、标签、值必填')
    return
  }
  dialog.loading = true
  try {
    if (dialog.isEdit && form.id) {
      await dictApi.updateDict(form.id, form)
      message.success('修改成功')
    } else {
      await dictApi.createDict(form)
      message.success('新增成功')
    }
    dialog.visible = false
    fetch()
  } finally { dialog.loading = false }
}

async function handleDelete(id: number) {
  await dictApi.deleteDict(id)
  message.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <n-card>
      <n-space>
        <n-input v-model:value="query.keyword" placeholder="字典类型/标签"
          clearable @keyup.enter="handleSearch" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button type="primary" @click="openCreate">新增字典</n-button>
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
      :title="dialog.isEdit ? '编辑字典' : '新增字典'" style="width: 480px">
      <n-form :model="form" label-placement="top">
        <n-form-item label="字典类型" required><n-input v-model:value="form.type" /></n-form-item>
        <n-form-item label="字典标签" required><n-input v-model:value="form.label" /></n-form-item>
        <n-form-item label="字典值" required><n-input v-model:value="form.value" /></n-form-item>
        <n-form-item label="排序"><n-input :value="form.sort !== undefined ? String(form.sort) : ''" @update:value="(v: string) => form.sort = Number(v) || 0" /></n-form-item>
        <n-form-item label="备注"><n-input v-model:value="form.remark" type="textarea" /></n-form-item>
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
