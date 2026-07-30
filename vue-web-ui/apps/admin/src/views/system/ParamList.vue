<script setup lang="ts">
import { h, onMounted, reactive } from 'vue'
import {
  NCard, NButton, NDataTable, NInput, NModal, NForm, NFormItem, NSpace,
  NPopconfirm, useMessage, type DataTableColumns
} from 'naive-ui'
import { useTable } from '@/hooks/useTable'
import * as paramApi from '@/api/system/param'
import type { ParamVO, PageQuery } from '@sca/types'

const message = useMessage()
const { loading, list, total, query, fetch, handlePageChange, handlePageSizeChange, handleSearch } =
  useTable<ParamVO, PageQuery>((q) => paramApi.pageParams(q))

const columns: DataTableColumns<ParamVO> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '参数 Key', key: 'key', width: 180 },
  { title: '参数值', key: 'value', width: 240 },
  { title: '更新时间', key: 'updateTime', width: 160 },
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
const form = reactive<Partial<ParamVO>>({
  id: undefined, key: '', value: '', remark: ''
})

function openCreate() {
  dialog.isEdit = false
  Object.assign(form, { id: undefined, key: '', value: '', remark: '' })
  dialog.visible = true
}

function openEdit(row: ParamVO) {
  dialog.isEdit = true
  Object.assign(form, {
    id: row.id, key: row.key, value: row.value, remark: row.remark
  })
  dialog.visible = true
}

async function handleSubmit() {
  if (!form.key || !form.value) {
    message.warning('Key 和 Value 必填')
    return
  }
  dialog.loading = true
  try {
    if (dialog.isEdit && form.id) {
      await paramApi.updateParam(form.id, form)
      message.success('修改成功')
    } else {
      await paramApi.createParam(form)
      message.success('新增成功')
    }
    dialog.visible = false
    fetch()
  } finally { dialog.loading = false }
}

async function handleDelete(id: number) {
  await paramApi.deleteParam(id)
  message.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <n-card>
      <n-space>
        <n-input v-model:value="query.keyword" placeholder="参数 Key"
          clearable @keyup.enter="handleSearch" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button type="primary" @click="openCreate">新增参数</n-button>
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
      :title="dialog.isEdit ? '编辑参数' : '新增参数'" style="width: 480px">
      <n-form :model="form" label-placement="top">
        <n-form-item label="参数 Key" required>
          <n-input v-model:value="form.key" :disabled="dialog.isEdit" />
        </n-form-item>
        <n-form-item label="参数值" required><n-input v-model:value="form.value" type="textarea" /></n-form-item>
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
