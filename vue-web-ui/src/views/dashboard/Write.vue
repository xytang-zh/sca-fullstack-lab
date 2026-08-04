<script setup lang="ts">
/**
 * 撰写/编辑文章页（用户中心）：Markdown 编辑器，支持保存草稿或直接发布。
 * - 通过 ?id= 参数进入编辑态，加载已有文章内容
 */
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDiscreteApi } from 'naive-ui'
import { articleApi } from '@sca/api'
import type { ColumnVO } from '@sca/types'

const { message } = createDiscreteApi(['message'])
const route = useRoute()
const router = useRouter()

/** 编辑态文章 ID（来自 ?id=，存在即编辑否则新建） */
const editId = route.query.id as string | undefined
/** 我的专栏下拉选项（文章可归属某专栏） */
const columns = ref<ColumnVO[]>([])
/** 提交中标识（防重复提交） */
const submitting = ref(false)
/** 文章表单 */
const form = reactive({
  title: '',
  summary: '',
  contentMd: '',
  columnId: undefined as string | undefined
})

onMounted(async () => {
  // 加载我的专栏供下拉选择；编辑态则回填文章内容
  const columnPage = await articleApi.myColumns(1, 100)
  columns.value = columnPage.records
  if (editId) {
    const article = await articleApi.getArticleForEdit(editId)
    form.title = article.title
    form.summary = article.summary ?? ''
    form.contentMd = article.contentMd
    form.columnId = article.columnId
  }
})

/** 保存（1=草稿 3=发布）：校验标题/正文非空后提交，成功后跳转对应列表页 */
async function save(status: 1 | 3) {
  if (!form.title.trim()) {
    message.warning('请输入标题')
    return
  }
  if (!form.contentMd.trim()) {
    message.warning('请输入正文')
    return
  }
  submitting.value = true
  try {
    const dto = {
      title: form.title.trim(),
      summary: form.summary.trim(),
      contentMd: form.contentMd,
      columnId: form.columnId,
      status
    }
    if (editId) {
      await articleApi.updateArticle(editId, dto)
    } else {
      await articleApi.createArticle(dto)
    }
    message.success(status === 3 ? '发布成功' : '已保存草稿')
    router.push(status === 3 ? '/dashboard/articles' : '/dashboard/drafts')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="max-w-3xl mx-auto space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-800">{{ editId ? '编辑文章' : '撰写文章' }}</h2>
    </div>

    <n-card class="rounded-2xl">
      <div class="space-y-4">
        <n-input v-model:value="form.title" size="large" placeholder="请输入标题" maxlength="128" />
        <n-input v-model:value="form.summary" placeholder="请输入摘要（可选）" maxlength="512" />
        <n-select
          v-model:value="form.columnId"
          placeholder="选择专栏（可选）"
          clearable
          :options="columns.map((c) => ({ label: c.name, value: c.id }))"
        />
        <n-input
          v-model:value="form.contentMd"
          type="textarea"
          :autosize="{ minRows: 12, maxRows: 30 }"
          placeholder="支持 Markdown 语法，开始写作…"
        />
        <div class="flex justify-end gap-3">
          <n-button :loading="submitting" @click="save(1)">保存草稿</n-button>
          <n-button type="primary" :loading="submitting" @click="save(3)">发布</n-button>
        </div>
      </div>
    </n-card>
  </div>
</template>