<script setup lang="ts">
import { h, onMounted, reactive } from 'vue'
import {
  NCard,
  NButton,
  NDataTable,
  NInput,
  NModal,
  NForm,
  NFormItem,
  NSpace,
  NPopconfirm,
  NSelect,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import { useTable } from '@/hooks/useTable'
import * as roleApi from '@/api/system/role'
import type { RoleVO, PageQuery } from '@sca/types'

const message = useMessage()
const { loading, list, total, query, fetch, handlePageChange, handlePageSizeChange, handleSearch } =
  useTable<RoleVO, PageQuery>((q) => roleApi.pageRoles(q))

const columns: DataTableColumns<RoleVO> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '角色编码', key: 'code', width: 140 },
  { title: '角色名称', key: 'name', width: 140 },
  {
    title: '数据范围',
    key: 'dataScope',
    width: 100,
    render: (row) =>
      ['全部', '本部门及以下', '仅本部门', '仅本人', '自定义'][row.dataScope ? row.dataScope - 1 : 0]
  },
  { title: '备注', key: 'remark', width: 200 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) =>
      h(NSpace, null, () => [
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
]

const dialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<RoleVO>>({ id: undefined, code: '', name: '', dataScope: 1, remark: '' })

function openCreate() {
  dialog.isEdit = false
  Object.assign(form, { id: undefined, code: '', name: '', dataScope: 1, remark: '' })
  dialog.visible = true
}

function openEdit(row: RoleVO) {
  dialog.isEdit = true
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    dataScope: row.dataScope ?? 1,
    remark: row.remark
  })
  dialog.visible = true
}

async function handleSubmit() {
  if (!form.code || !form.name) {
    message.warning('角色编码与名称必填')
    return
  }
  dialog.loading = true
  try {
    if (dialog.isEdit && form.id) {
      await roleApi.updateRole(form.id, form)
      message.success('修改成功')
    } else {
      await roleApi.createRole(form)
      message.success('新增成功')
    }
    dialog.visible = false
    fetch()
  } finally {
    dialog.loading = false
  }
}

async function handleDelete(id: number) {
  await roleApi.deleteRole(id)
  message.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <n-card>
      <n-space>
        <n-input v-model:value="query.keyword" placeholder="角色编码/名称" clearable
          @keyup.enter="handleSearch" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button type="primary" @click="openCreate">新增角色</n-button>
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
    <n-modal v-model:show="dialog.visible" preset="card"
      :title="dialog.isEdit ? '编辑角色' : '新增角色'" style="width: 480px">
      <n-form :model="form" label-placement="top">
        <n-form-item label="角色编码" required>
          <n-input v-model:value="form.code" :disabled="dialog.isEdit" />
        </n-form-item>
        <n-form-item label="角色名称" required>
          <n-input v-model:value="form.name" />
        </n-form-item>
        <n-form-item label="数据范围">
          <n-select v-model:value="form.dataScope" :options="[
            { label: '全部数据', value: 1 },
            { label: '本部门及以下', value: 2 },
            { label: '仅本部门', value: 3 },
            { label: '仅本人', value: 4 },
            { label: '自定义', value: 5 }
          ]" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" />
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
