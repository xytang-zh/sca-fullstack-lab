<script setup lang="ts">
/**
 * 我的专栏页（用户中心）：分页展示当前用户的专栏，支持新建/编辑/删除（弹窗表单）。
 */
import { onMounted, reactive, ref } from 'vue'
import { createDiscreteApi } from 'naive-ui'
import { articleApi } from '@sca/api'
import type { ColumnVO } from '@sca/types'

const { message } = createDiscreteApi(['message'])

/** 我的专栏列表 */
const columns = ref<ColumnVO[]>([])
/** 总数（分页用） */
const total = ref(0)
/** 加载中标识 */
const loading = ref(false)
/** 当前页码 */
const page = ref(1)
/** 每页条数 */
const size = 10
/** 新建/编辑弹窗是否显示 */
const showModal = ref(false)
/** 正在编辑的专栏（null 表示新建） */
const editing = ref<ColumnVO | null>(null)
/** 提交中标识（防重复提交） */
const submitting = ref(false)

/** 专栏表单（新建/编辑共用） */
const form = reactive({ name: '', description: '', coverImage: '' })

/** 分页加载我的专栏 */
async function load() {
  loading.value = true
  try {
    const data = await articleApi.myColumns(page.value, size)
    columns.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 打开新建弹窗：清空编辑态与表单 */
function openCreate() {
  editing.value = null
  form.name = ''
  form.description = ''
  form.coverImage = ''
  showModal.value = true
}

/** 打开编辑弹窗：填充当前专栏数据到表单 */
function openEdit(column: ColumnVO) {
  editing.value = column
  form.name = column.name
  form.description = column.description ?? ''
  form.coverImage = column.coverImage ?? ''
  showModal.value = true
}

/** 提交表单：编辑态走更新，否则走创建，成功后关闭弹窗并刷新列表 */
async function submit() {
  if (!form.name.trim()) {
    message.warning('请输入专栏名称')
    return
  }
  submitting.value = true
  try {
    const dto = {
      name: form.name.trim(),
      description: form.description.trim(),
      coverImage: form.coverImage
    }
    if (editing.value) {
      await articleApi.updateColumn(editing.value.id, dto)
      message.success('专栏已更新')
    } else {
      await articleApi.createColumn(dto)
      message.success('专栏已创建')
    }
    showModal.value = false
    load()
  } finally {
    submitting.value = false
  }
}

/** 删除专栏并刷新列表 */
async function handleDelete(column: ColumnVO) {
  await articleApi.deleteColumn(column.id)
  message.success('专栏已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-xl font-semibold text-gray-800">我的专栏</h2>
      <n-button type="primary" size="small" @click="openCreate">新建专栏</n-button>
    </div>

    <n-spin :show="loading">
      <div v-if="columns.length" class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div
          v-for="column in columns"
          :key="column.id"
          class="bg-white rounded-xl border border-gray-100 p-5"
        >
          <h3 class="text-lg font-semibold text-gray-800">{{ column.name }}</h3>
          <n-text v-if="column.description" depth="2" class="text-sm line-clamp-2 mt-2 block">
            {{ column.description }}
          </n-text>
          <div class="flex items-center gap-4 text-xs text-gray-400 mt-4">
            <span>{{ column.articleCount }} 篇文章</span>
            <span>{{ column.subscribeCount }} 订阅</span>
          </div>
          <div class="flex gap-2 mt-4">
            <n-button size="tiny" @click="openEdit(column)">编辑</n-button>
            <n-button size="tiny" type="error" quaternary @click="handleDelete(column)">删除</n-button>
          </div>
        </div>
      </div>
      <n-empty v-else description="还没有专栏，点击右上角新建" class="py-16" />
    </n-spin>

    <n-modal v-model:show="showModal" preset="card" :title="editing ? '编辑专栏' : '新建专栏'" style="max-width: 480px">
      <div class="space-y-4">
        <n-input v-model:value="form.name" placeholder="专栏名称" maxlength="64" />
        <n-input v-model:value="form.description" type="textarea" placeholder="专栏简介" maxlength="512" />
        <n-input v-model:value="form.coverImage" placeholder="封面图 URL（可选）" />
        <div class="flex justify-end gap-3">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submit">保存</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>