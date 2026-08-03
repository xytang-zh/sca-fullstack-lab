<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { authApi } from '@sca/api'
import type { CaptchaVO } from '@sca/types'

const props = defineProps<{ show: boolean }>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success', checkToken: string): void
}>()

const EXPIRED_MS = 2 * 60 * 1000
const THUMB_DISPLAY_WIDTH = 40

const captcha = ref<CaptchaVO | null>(null)
const bgBoxRef = ref<HTMLElement | null>(null)
const barRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const errorMsg = ref('')
const dragging = ref(false)
const offsetX = ref(0)
const startTime = ref(0)
const trackList = ref<Array<{ x: number; y: number; t: number }>>([])

let expiredTimer: number | undefined

const bgWidth = computed(() => captcha.value?.backgroundImageWidth ?? 300)
const bgHeight = computed(() => captcha.value?.backgroundImageHeight ?? 150)
const templateWidth = computed(() => captcha.value?.templateImageWidth ?? 50)
const templateHeight = computed(() => captcha.value?.templateImageHeight ?? 50)

/** 背景图实际显示宽度（容器缩放后） */
const displayWidth = ref(0)
/** 显示坐标 → 原始坐标 缩放比例 */
const scale = computed(() =>
  displayWidth.value > 0 && bgWidth.value > 0 ? bgWidth.value / displayWidth.value : 1
)

const maxOffset = computed(() => Math.max(displayWidth.value - THUMB_DISPLAY_WIDTH, 0))

async function load() {
  loading.value = true
  errorMsg.value = ''
  resetDrag()
  try {
    captcha.value = await authApi.getCaptcha()
    clearTimeout(expiredTimer)
    expiredTimer = window.setTimeout(() => {
      errorMsg.value = '验证已超时，请重新获取'
      close()
    }, EXPIRED_MS)
  } finally {
    loading.value = false
  }
}

function resetDrag() {
  dragging.value = false
  offsetX.value = 0
  startTime.value = 0
  trackList.value = []
}

function onBgLoad(e: Event) {
  displayWidth.value = (e.target as HTMLImageElement).clientWidth || 0
}

function close() {
  emit('update:show', false)
}

function onPointerDown(e: PointerEvent) {
  if (loading.value || !captcha.value || e.button !== 0) {
    return
  }
  dragging.value = true
  startTime.value = Date.now()
  trackList.value = []
  // 指针可能移出滑动条，事件改挂 window 监听，保证 move/up 不丢失
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
  onPointerMove(e)
}

function onPointerMove(e: PointerEvent) {
  if (!dragging.value || !captcha.value || !barRef.value) {
    return
  }
  const barRect = barRef.value.getBoundingClientRect()
  const raw = e.clientX - barRect.left
  offsetX.value = Math.min(Math.max(raw, 0), maxOffset.value)
  // 轨迹坐标需映射回原始图片坐标系，后端按缺口位置校验
  trackList.value.push({ x: offsetX.value * scale.value, y: 0, t: Date.now() - startTime.value })
}

async function onPointerUp() {
  if (!dragging.value || !captcha.value) {
    return
  }
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  const data = {
    trackList: trackList.value,
    bgImageWidth: bgWidth.value,
    bgImageHeight: bgHeight.value,
    templateImageWidth: templateWidth.value,
    templateImageHeight: templateHeight.value,
    startTime: startTime.value,
    stopTime: Date.now()
  }
  try {
    const result = await authApi.checkCaptcha(captcha.value.id, data)
    clearTimeout(expiredTimer)
    emit('success', result.checkToken)
    close()
  } catch {
    errorMsg.value = '验证失败，请重试'
    load()
  }
}

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      load()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  clearTimeout(expiredTimer)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
})
</script>

<template>
  <n-modal
    :show="props.show"
    :mask-closable="false"
    :close-on-esc="false"
    transform-origin="center"
    @update:show="close"
  >
    <div class="w-[380px] bg-white rounded-lg p-5 shadow-xl">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-semibold text-gray-800">安全验证</h3>
        <button class="text-gray-400 hover:text-gray-600 text-lg leading-none" @click="close">×</button>
      </div>

      <n-spin :show="loading">
        <div v-if="captcha" ref="bgBoxRef"
             class="relative overflow-hidden rounded-md mb-4 select-none"
             :style="{ aspectRatio: `${bgWidth} / ${bgHeight}` }">
          <img :src="captcha.backgroundImage" alt="验证码背景"
               class="w-full h-full object-cover" draggable="false"
               @load="onBgLoad" />
          <img :src="captcha.templateImage" alt="缺口拼图"
               class="absolute left-0 top-0 pointer-events-none"
               :style="{
                 width: `${templateWidth * scale}px`,
                 height: `${templateHeight * scale}px`,
                 transform: `translateX(${offsetX}px)`
               }"
               draggable="false" />
        </div>
      </n-spin>

      <p v-if="errorMsg" class="text-xs text-red-500 mb-2">{{ errorMsg }}</p>

      <div
        ref="barRef"
        class="relative bg-gray-100 rounded-full h-10 overflow-hidden cursor-pointer"
        :style="{ width: displayWidth > 0 ? `${displayWidth}px` : '100%' }"
        @pointerdown="onPointerDown"
      >
        <div class="absolute inset-y-0 left-0 bg-blue-100 transition-colors"
             :style="{ width: `${offsetX}px` }" />
        <span class="absolute inset-y-0 left-1/2 -translate-x-1/2 flex items-center text-xs text-gray-400">
          {{ dragging ? '拖动验证中' : '向右拖动滑块完成验证' }}
        </span>
        <div class="absolute top-0 bottom-0 w-10 bg-white border border-gray-300 rounded-full shadow flex items-center justify-center"
             :style="{ left: `${offsetX}px` }">
          <span class="text-gray-500 text-sm">▶</span>
        </div>
      </div>

      <p class="text-xs text-gray-300 mt-3 text-center">2 分钟内未完成验证将自动失效</p>
    </div>
  </n-modal>
</template>
