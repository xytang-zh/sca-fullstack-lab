<script setup lang="ts">
import { ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import java from 'highlight.js/lib/languages/java'
import xml from 'highlight.js/lib/languages/xml'
import sql from 'highlight.js/lib/languages/sql'
import bash from 'highlight.js/lib/languages/bash'
import json from 'highlight.js/lib/languages/json'
import css from 'highlight.js/lib/languages/css'
import yaml from 'highlight.js/lib/languages/yaml'
import markdown from 'highlight.js/lib/languages/markdown'
import python from 'highlight.js/lib/languages/python'
import cpp from 'highlight.js/lib/languages/cpp'
import go from 'highlight.js/lib/languages/go'
import properties from 'highlight.js/lib/languages/properties'
import DOMPurify from 'dompurify'
import type { LanguageFn } from 'highlight.js'
import type { TocItem } from '@/hooks/useArticleToc'

const props = defineProps<{
  contentMd: string
}>()

const emit = defineEmits<{
  (e: 'toc-updated', toc: TocItem[]): void
}>()

const registered: Record<string, LanguageFn> = {
  javascript,
  typescript,
  java,
  xml,
  sql,
  bash,
  json,
  css,
  yaml,
  markdown,
  python,
  cpp,
  go,
  properties
}
Object.entries(registered).forEach(([name, lang]) => hljs.registerLanguage(name, lang))

const aliasMap: Record<string, string> = {
  js: 'javascript',
  ts: 'typescript',
  html: 'xml',
  yml: 'yaml',
  py: 'python',
  sh: 'bash',
  shell: 'bash',
  ini: 'properties'
}

const headingIds = new Map<string, number>()

function slugify(text: string): string {
  const base = text
    .toLowerCase()
    .trim()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '')
  const key = base || 'section'
  const count = headingIds.get(key) ?? 0
  headingIds.set(key, count + 1)
  return count === 0 ? key : `${key}-${count + 1}`
}

const toc: TocItem[] = []
const html = ref('')

const markdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight(str: string, lang: string): string {
    const name = aliasMap[lang] ?? lang
    const code = hljs.getLanguage(name)
      ? hljs.highlight(str, { language: name }).value
      : markdownIt.utils.escapeHtml(str)
    const cls = lang ? ` class="language-${lang}"` : ''
    return `<pre class="hljs"><code${cls}>${code}</code></pre>`
  }
})

markdownIt.renderer.rules.heading_open = (tokens, idx): string => {
  const tag = tokens[idx]!.tag
  const inline = tokens[idx + 1]
  const text = inline?.content ?? ''
  const id = slugify(text)
  toc.push({ level: Number(tag.slice(1)), text, id })
  return `<${tag} id="${id}">`
}

function render(md: string): string {
  toc.length = 0
  headingIds.clear()
  const raw = markdownIt.render(md)
  return DOMPurify.sanitize(raw, {
    ALLOWED_TAGS: [
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'p', 'br', 'strong', 'em', 'del', 'u',
      'code', 'pre', 'span', 'blockquote',
      'ul', 'ol', 'li', 'a', 'img',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'hr'
    ],
    ALLOWED_ATTR: ['id', 'class', 'href', 'title', 'src', 'alt', 'target', 'rel', 'align']
  })
}

watch(
  () => props.contentMd,
  (md) => {
    if (!md) {
      html.value = ''
      emit('toc-updated', [])
      return
    }
    html.value = render(md)
    emit('toc-updated', [...toc])
  },
  { immediate: true }
)
</script>

<template>
  <div class="md-body" v-html="html" />
</template>

<style scoped>
.md-body {
  --md-text: #1f2329;
  --md-muted: #6b7280;
  --md-border: #e4e4e7;
  --md-teal: #0d9488;
  --md-teal-bg: #f0fdfa;
  --md-code-bg: #1e1e2e;
  --md-code-text: #cdd6f4;
  --md-link: #2563eb;
  color: var(--md-text);
  font-size: 16px;
  line-height: 1.8;
  word-break: break-word;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3),
.md-body :deep(h4) {
  font-weight: 700;
  line-height: 1.4;
  margin: 2em 0 0.8em;
  scroll-margin-top: 88px;
}
.md-body :deep(h1) {
  font-size: 1.75em;
  border-bottom: 1px solid var(--md-border);
  padding-bottom: 0.4em;
}
.md-body :deep(h2) {
  font-size: 1.45em;
  border-bottom: 1px solid var(--md-border);
  padding-bottom: 0.35em;
}
.md-body :deep(h3) {
  font-size: 1.2em;
}
.md-body :deep(h4) {
  font-size: 1.05em;
}
.md-body :deep(p) {
  margin: 0.9em 0;
}
.md-body :deep(a) {
  color: var(--md-link);
}
.md-body :deep(a):hover {
  text-decoration: underline;
}
.md-body :deep(strong) {
  font-weight: 600;
}
.md-body :deep(blockquote) {
  border-left: 3px solid var(--md-teal);
  background: var(--md-teal-bg);
  margin: 1em 0;
  padding: 0.6em 1em;
  border-radius: 0 6px 6px 0;
  color: var(--md-muted);
}
.md-body :deep(ul),
.md-body :deep(ol) {
  padding-left: 1.6em;
  margin: 0.8em 0;
}
.md-body :deep(li) {
  margin: 0.3em 0;
}
.md-body :deep(li::marker) {
  color: var(--md-teal);
}
.md-body :deep(code):not(pre code) {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  font-size: 0.875em;
  background: var(--md-teal-bg);
  color: var(--md-teal);
  padding: 0.15em 0.4em;
  border-radius: 4px;
}
.md-body :deep(pre.hljs) {
  background: var(--md-code-bg);
  color: var(--md-code-text);
  border-radius: 8px;
  padding: 1em 1.2em;
  overflow-x: auto;
  margin: 1.2em 0;
  font-size: 0.9em;
  line-height: 1.6;
}
.md-body :deep(pre code) {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
  background: transparent;
  color: inherit;
  padding: 0;
}
.md-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.2em 0;
  font-size: 0.92em;
}
.md-body :deep(th),
.md-body :deep(td) {
  border: 1px solid var(--md-border);
  padding: 0.5em 0.9em;
}
.md-body :deep(th) {
  background: #fafaf9;
  font-weight: 600;
}
.md-body :deep(tr:nth-child(2n)) {
  background: #fafaf9;
}
.md-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}
.md-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--md-border);
  margin: 2em 0;
}

/* highlight.js token 配色（编辑器深色主题） */
.md-body :deep(.hljs-keyword),
.md-body :deep(.hljs-selector-tag),
.md-body :deep(.hljs-built_in) {
  color: #c792ea;
}
.md-body :deep(.hljs-string),
.md-body :deep(.hljs-regexp) {
  color: #9ece6a;
}
.md-body :deep(.hljs-number),
.md-body :deep(.hljs-literal) {
  color: #f78c6c;
}
.md-body :deep(.hljs-comment) {
  color: #6b7089;
  font-style: italic;
}
.md-body :deep(.hljs-title),
.md-body :deep(.hljs-title.function_),
.md-body :deep(.hljs-title.class_) {
  color: #82aaff;
}
.md-body :deep(.hljs-attr),
.md-body :deep(.hljs-attribute) {
  color: #c3e88d;
}
.md-body :deep(.hljs-meta),
.md-body :deep(.hljs-doctag) {
  color: #89ddff;
}
.md-body :deep(.hljs-variable),
.md-body :deep(.hljs-params) {
  color: #ffcb6b;
}
.md-body :deep(.hljs-tag) {
  color: #89ddff;
}
.md-body :deep(.hljs-name) {
  color: #c792ea;
}
</style>
