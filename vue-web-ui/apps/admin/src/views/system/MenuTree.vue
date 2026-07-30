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
  NPopconfirm,
  NSelect,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import * as menuApi from '@/api/system/menu'
import type { MenuVO } from '@sca/types'

const message = useMessage()
const loading = ref(false)
const list = ref<MenuVO[]>([])

async function fetch() {
  loading.value = true
  try {
    list.value = await menuApi.getMenuTree()
  } finally {
    loading.value = false
  }
}

const columns: DataTableColumns<MenuVO> = [
  { title: '菜单名称', key: 'name', width: 200 },
  { title: '类型', key: 'type', width: 80,
    render: (row) => ['目录', '菜单', '按钮'][row.type - 1] ?? row.type },
  { title: '路径', key: 'path', width: 160 },
  { title: '组件', key: 'component', width: 200 },
  { title: '权限标识', key: 'perms', width: 200 },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '操作', key: 'actions', width: 200,
    render: (row) => h(NSpace, null, () => [
      h(NButton, { size: 'small', onClick: () => openEdit(row) }, () => '编辑'),
      h(NButton, { size: 'small', type: 'primary',
        onClick: () => openCreate(row.id) }, () => '新增子菜单'),
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除')
      })
    ])
  }
]

const rowKey = (row: MenuVO) => row.id

const dialog = reactive({ visible: false, isEdit: false, loading: false })
const form = reactive<Partial<MenuVO>>({
  id: undefined, parentId: 0, name: '', type: 1, path: '', component: '',
  icon: '', perms: '', sort: 0, visible: 1
})

function openCreate(parentId = 0) {
  dialog.isEdit = false
  Object.assign(form, {
    id: undefined, parentId, name: '', type: 1, path: '', component: '',
    icon: '', perms: '', sort: 0, visible: 1
  })
  dialog.visible = true
}

function openEdit(row: MenuVO) {
  dialog.isEdit = true
  Object.assign(form, {
    id: row.id, parentId: row.parentId, name: row.name, type: row.type,
    path: row.path, component: row.component, icon: row.icon, perms: row.perms,
    sort: row.sort, visible: row.visible
  })
  dialog.visible = true
}

async function handleSubmit() {
  if (!form.name) {
    message.warning('菜单名称必填')
    return
  }
  dialog.loading = true
  try {
    if (dialog.isEdit && form.id) {
      await menuApi.updateMenu(form.id, form)
      message.success('修改成功')
    } else {
      await menuApi.createMenu(form)
      message.success('新增成功')
    }
    dialog.visible = false
    fetch()
  } finally {
    dialog.loading = false
  }
}

async function handleDelete(id: number) {
  await menuApi.deleteMenu(id)
  message.success('删除成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="page">
    <n-card>
      <n-space>
        <n-button type="primary" @click="openCreate(0)">新增根菜单</n-button>
        <n-button @click="fetch">刷新</n-button>
      </n-space>
    </n-card>
    <n-card>
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :row-key="rowKey"
        default-expand-all
      />
    </n-card>
    <n-modal v-model:show="dialog.visible" preset="card"
      :title="dialog.isEdit ? '编辑菜单' : '新增菜单'" style="width: 560px">
      <n-form :model="form" label-placement="top">
        <n-form-item label="菜单类型" required>
          <n-select v-model:value="form.type" :options="[
            { label: '目录', value: 1 },
            { label: '菜单', value: 2 },
            { label: '按钮', value: 3 }
          ]" />
        </n-form-item>
        <n-form-item label="菜单名称" required>
          <n-input v-model:value="form.name" />
        </n-form-item>
        <n-form-item label="路径">
          <n-input v-model:value="form.path" placeholder="如 /system/users" />
        </n-form-item>
        <n-form-item label="组件">
          <n-input v-model:value="form.component" placeholder="如 system/UserList" />
        </n-form-item>
        <n-form-item label="图标">
          <n-input v-model:value="form.icon" />
        </n-form-item>
        <n-form-item label="权限标识">
          <n-input v-model:value="form.perms" placeholder="如 system:user:list" />
        </n-form-item>
        <n-form-item label="排序">
          <n-input :value="form.sort !== undefined ? String(form.sort) : ''" @update:value="(v: string) => form.sort = Number(v) || 0" />
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
