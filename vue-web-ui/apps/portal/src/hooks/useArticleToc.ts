import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

export interface TocItem {
  /** 标题层级：2=h2, 3=h3 */
  level: number
  text: string
  id: string
}

/** 滚动高亮区域：视口纵向 20%~30% 的窄窗口 */
const ROOT_MARGIN = '-20% 0px -70% 0px'

/**
 * 监听文章标题元素，计算当前阅读章节。
 * 通过 IntersectionObserver 观察标题元素，离开窗口时兜底重算，
 * 保证向上滚动时高亮能正确回退到上一章节。
 */
export function useArticleToc(toc: Ref<TocItem[]>) {
  const activeId = ref('')
  let observer: IntersectionObserver | null = null
  const elements = new Map<string, Element>()

  function recompute() {
    const winH = window.innerHeight
    const topBound = winH * 0.2
    const bottomBound = winH * 0.3
    let bestId = ''
    for (const [id, el] of elements) {
      const rect = el.getBoundingClientRect()
      if (rect.bottom >= topBound && rect.top <= bottomBound) {
        const bestEl = bestId === '' ? null : (elements.get(bestId) ?? null)
        if (!bestEl || rect.top > bestEl.getBoundingClientRect().top) {
          bestId = id
        }
      }
    }
    activeId.value = bestId
  }

  function onObserve(entries: IntersectionObserverEntry[]) {
    const leaving = entries.some((e) => !e.isIntersecting)
    if (leaving) {
      recompute()
      return
    }
    const entering = entries.filter((e) => e.isIntersecting)
    if (entering.length) {
      const latest = entering.reduce((a, b) =>
        b.boundingClientRect.top > a.boundingClientRect.top ? b : a
      )
      activeId.value = latest.target.id
    }
  }

  function observe() {
    disconnect()
    const items = toc.value
    if (!items.length) {
      return
    }
    observer = new IntersectionObserver(onObserve, { rootMargin: ROOT_MARGIN })
    items.forEach((item) => {
      const el = document.getElementById(item.id)
      if (el) {
        elements.set(item.id, el)
        observer!.observe(el)
      }
    })
  }

  function disconnect() {
    observer?.disconnect()
    observer = null
    elements.clear()
  }

  watch(toc, observe, { immediate: true })
  onBeforeUnmount(disconnect)

  return { activeId }
}
