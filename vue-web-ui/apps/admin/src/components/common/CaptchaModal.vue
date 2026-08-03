<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { NCard, NModal, NButton, useMessage } from 'naive-ui'
import { getCaptcha, checkCaptcha } from '@/api/auth'
import type { CaptchaVO } from '@sca/types'

const props = defineProps<{ show: boolean }>()
const emit = defineEmits<{
  (e: 'success', checkToken: string): void
  (e: 'close'): void
}>()

const message = useMessage()
const loading = ref(false)
const captchaData = ref<CaptchaVO | null>(null)
const containerRef = ref<HTMLDivElement>()
const sliderRef = ref<HTMLDivElement>()
const trackRef = ref<HTMLDivElement>()

const sliderWidth = ref(280)
const bgImage = ref('')
const sliderHeight = 40
const offsetX = ref(0)
const dragging = ref(false)
const startX = ref(0)
const verified = ref(false)

// 轨迹数据
const points: Array<{ x: number; y: number; t: number }> = []

async function loadCaptcha() {
  loading.value = true
  verified.value = false
  offsetX.value = 0
  points.length = 0
  try {
    const data = await getCaptcha()
    captchaData.value = data
    bgImage.value = data?.backgroundImage ?? ''
  } catch {
    message.error('获取验证码失败')
  } finally {
    loading.value = false
  }
}

function getClientX(e: MouseEvent | TouchEvent): number {
  if ('touches' in e) {
    return (e as TouchEvent).touches[0]?.clientX ?? 0
  }
  return (e as MouseEvent).clientX
}

function onDragStart(e: MouseEvent | TouchEvent) {
  if (verified.value) return
  dragging.value = true
  const clientX = getClientX(e)
  startX.value = clientX
  points.length = 0
  recordPoint(clientX)
}

function onDragMove(e: MouseEvent | TouchEvent) {
  if (!dragging.value) return
  const clientX = getClientX(e)
  const rect = (e.target as HTMLElement).closest('.captcha-slider')?.getBoundingClientRect()
  if (!rect) return
  let newOffset = clientX - rect.left - 20
  if (newOffset < 0) newOffset = 0
  if (newOffset > sliderWidth.value - sliderHeight) {
    newOffset = sliderWidth.value - sliderHeight
  }
  offsetX.value = newOffset
  recordPoint(clientX)
}

function onDragEnd() {
  if (!dragging.value) return
  dragging.value = false
  // 记录最后一个点
  const now = Date.now()
  const lastPoint = points.at(-1)
  const lastT = lastPoint ? lastPoint.t : now
  points.push({ x: offsetX.value, y: 0, t: now - lastT })
  // 提交校验
  submitCheck()
}

function recordPoint(clientX: number) {
  const now = Date.now()
  const last = points.at(-1)
  if (last && now - last.t < 16) return
  points.push({ x: clientX, y: 0, t: now })
}

async function submitCheck() {
  const data = captchaData.value
  if (!data) return
  loading.value = true
  try {
    const track = {
      trackList: points.map((p) => ({
        x: p.x,
        y: p.y,
        t: p.t
      })),
      type: 'SLIDER'
    }
    const result = await checkCaptcha(data.id, track)
    if (result?.checkToken) {
      verified.value = true
      emit('success', result.checkToken)
    } else {
      message.error('验证失败，请重试')
      loadCaptcha()
    }
  } catch {
    message.error('验证失败，请重试')
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

watch(() => props.show, (val) => {
  if (val) {
    nextTick(() => loadCaptcha())
  }
})
</script>

<template>
  <n-modal :show="props.show" :mask-closable="false" @update:show="(v: boolean) => !v && emit('close')">
    <n-card style="width: 380px" :bordered="false" closable @close="emit('close')">
      <template #header>
        <span>安全验证</span>
      </template>
      <div class="captcha-body">
        <div v-if="loading && !bgImage" class="captcha-loading">
          加载中...
        </div>
        <div v-else ref="containerRef" class="captcha-container">
          <!-- 背景图 -->
          <div class="captcha-bg" :style="{ backgroundImage: bgImage ? `url(${bgImage})` : 'none' }">
            <!-- 缺口罩片 -->
            <div class="captcha-gap" :style="{ left: '40px' }" />
          </div>
          <!-- 滑块 -->
          <div class="captcha-slider" ref="sliderRef"
            @mousedown="onDragStart" @mousemove="onDragMove" @mouseup="onDragEnd" @mouseleave="onDragEnd"
            @touchstart.prevent="onDragStart" @touchmove="onDragMove" @touchend="onDragEnd">
            <div class="slider-track" ref="trackRef" :style="{ width: offsetX + 20 + 'px' }" />
            <div class="slider-btn" :class="{ verified: verified }" :style="{ left: offsetX + 'px' }">
              <span v-if="verified">✓</span>
              <span v-else>▶</span>
            </div>
            <span class="slider-text" v-if="!dragging && !verified">拖动滑块完成验证</span>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="captcha-footer">
          <n-button size="small" quaternary @click="loadCaptcha">刷新</n-button>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<style scoped>
.captcha-body {
  min-height: 200px;
}
.captcha-loading {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}
.captcha-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.captcha-bg {
  width: 280px;
  height: 160px;
  background-size: cover;
  background-position: center;
  border-radius: 4px;
  position: relative;
  overflow: hidden;
  margin: 0 auto;
}
.captcha-gap {
  position: absolute;
  width: 40px;
  height: 40px;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.5);
  border: 2px solid #fff;
}
.captcha-slider {
  width: 280px;
  height: 40px;
  background: #f0f2f5;
  border-radius: 4px;
  position: relative;
  cursor: pointer;
  margin: 0 auto;
  user-select: none;
}
.slider-track {
  height: 100%;
  background: #d9ecff;
  border-radius: 4px;
  transition: none;
}
.slider-btn {
  position: absolute;
  top: 0;
  width: 40px;
  height: 40px;
  background: #fff;
  border: 1px solid #c0c4cc;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 2;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.slider-btn.verified {
  background: #67c23a;
  border-color: #67c23a;
  color: #fff;
}
.slider-btn:active {
  cursor: grabbing;
}
.slider-text {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 13px;
  color: #909399;
  z-index: 1;
  pointer-events: none;
}
.captcha-footer {
  text-align: right;
}
</style>