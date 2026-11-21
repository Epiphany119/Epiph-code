<template>
  <div class="markdown-content" v-html="renderedResult.html" @click="handleContentClick"></div>

  <!-- Gemini-style Fullscreen Code Focus Modal with Multi-File Sidebar -->
  <Teleport to="body">
    <div
      v-if="activeFullscreenBlock"
      class="code-fullscreen-backdrop"
      role="dialog"
      aria-modal="true"
      aria-label="全屏查看代码"
      @click.self="closeFullscreen"
    >
      <div class="code-fullscreen-sheet" :class="{ 'has-sidebar': showFileSidebar }">
        <header class="code-fullscreen-header">
          <div class="code-fullscreen-title">
            <span class="code-file-title">{{ activeFullscreenBlock.displayName }}</span>
            <span class="code-lang-pill">{{ activeFullscreenBlock.langLabel }}</span>
            <span class="code-line-count">{{ activeFullscreenBlock.lineCount }} 行</span>
          </div>
          <div class="code-fullscreen-actions">
            <button type="button" class="fs-action-btn" @click="copyFullscreenCode">
              <svg v-if="!fullscreenCopied" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
              </svg>
              <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              <span>{{ fullscreenCopied ? '已复制' : '复制代码' }}</span>
            </button>
            <button type="button" class="fs-action-btn fs-action-btn--close" title="退出全屏 (Esc)" @click="closeFullscreen">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="4 14 10 14 10 20"></polyline>
                <polyline points="20 10 14 10 14 4"></polyline>
                <line x1="14" y1="10" x2="21" y2="3"></line>
                <line x1="3" y1="21" x2="10" y2="14"></line>
              </svg>
              <span>退出全屏</span>
            </button>
          </div>
        </header>

        <div class="code-fullscreen-main">
          <!-- Left File Explorer Sidebar (Auto-shown for Vue / Multi-file replies) -->
          <aside v-if="showFileSidebar" class="code-fullscreen-sidebar" aria-label="生成的文件列表">
            <div class="sidebar-top">
              <span class="sidebar-heading">文件列表</span>
              <span class="sidebar-count">{{ renderedResult.blocks.length }}</span>
            </div>
            <div v-if="renderedResult.blocks.length > 5" class="sidebar-search-wrap">
              <input
                v-model="fileSearchQuery"
                type="text"
                class="sidebar-search-input"
                placeholder="筛选文件名..."
              />
            </div>
            <div class="sidebar-file-list">
              <button
                v-for="item in filteredBlocks"
                :key="item.index"
                type="button"
                class="sidebar-file-item"
                :class="{ 'sidebar-file-item--active': item.index === activeFullscreenIdx }"
                :title="item.displayName"
                @click="selectFullscreenFile(item.index)"
              >
                <svg class="file-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                </svg>
                <span class="file-item-meta">
                  <span class="file-item-name">{{ item.shortName }}</span>
                  <span v-if="item.dirPath" class="file-item-dir">{{ item.dirPath }}</span>
                </span>
                <span class="file-item-lang">{{ item.shortLang }}</span>
              </button>
              <div v-if="!filteredBlocks.length" class="sidebar-empty">无匹配文件</div>
            </div>
          </aside>

          <!-- Right Code Viewer Pane -->
          <div ref="fullscreenBodyRef" class="code-fullscreen-body">
            <pre class="hljs code-fullscreen-pre"><code v-html="activeFullscreenBlock.highlightedHtml"></code></pre>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

interface Props {
  content: string
}

interface CodeBlockEntry {
  index: number
  raw: string
  displayName: string
  shortName: string
  dirPath: string
  langLabel: string
  shortLang: string
  highlightedHtml: string
  lineCount: number
}

const props = defineProps<Props>()
const activeFullscreenIdx = ref<number | null>(null)
const fullscreenCopied = ref(false)
const fileSearchQuery = ref('')
const fullscreenBodyRef = ref<HTMLElement>()

const formatLangLabel = (lang?: string) => {
  const cleaned = (lang || '').trim().toLowerCase()
  const map: Record<string, { label: string; short: string }> = {
    html: { label: 'HTML', short: 'HTML' },
    xml: { label: 'HTML', short: 'HTML' },
    css: { label: 'CSS', short: 'CSS' },
    scss: { label: 'SCSS', short: 'SCSS' },
    less: { label: 'LESS', short: 'LESS' },
    js: { label: 'JavaScript', short: 'JS' },
    javascript: { label: 'JavaScript', short: 'JS' },
    ts: { label: 'TypeScript', short: 'TS' },
    typescript: { label: 'TypeScript', short: 'TS' },
    vue: { label: 'Vue', short: 'VUE' },
    json: { label: 'JSON', short: 'JSON' },
    java: { label: 'Java', short: 'JAVA' },
    sql: { label: 'SQL', short: 'SQL' },
    bash: { label: 'Bash', short: 'SH' },
    sh: { label: 'Shell', short: 'SH' },
  }
  if (map[cleaned]) return map[cleaned]
  const upper = cleaned ? cleaned.toUpperCase() : 'Code'
  return { label: upper, short: upper.slice(0, 4) }
}

const escapeHtmlAttr = (str: string) =>
  str
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

const FILE_PATH_REGEX =
  /([a-zA-Z0-9_@.\-/]+\.(?:vue|html|htm|css|scss|less|js|mjs|cjs|ts|tsx|jsx|json|md|svg|xml|yml|yaml|java|sql|env|txt))/i

const extractFilenameFromContext = (info: string, prevText: string): string => {
  // 1. Check fence info string (e.g. ```vue src/App.vue or ```html title="index.html")
  const infoRest = info.trim().split(/\s+/).slice(1).join(' ')
  if (infoRest) {
    const infoMatch = infoRest.match(FILE_PATH_REGEX)
    if (infoMatch?.[1]) return infoMatch[1].replace(/^\.\//, '')
  }

  // 2. Scan last 4 non-empty lines before the code block
  if (prevText) {
    const lines = prevText
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
      .slice(-4)
      .reverse()

    for (const line of lines) {
      // e.g. [工具调用] 写入文件 src/App.vue / 写入文件 package.json / 修改文件 index.html
      const toolMatch = line.match(/(?:写入文件|修改文件|创建文件|更新文件|文件[:：]?)\s*[`"'《「]?([^`"'》」\s]+)/)
      if (toolMatch?.[1] && FILE_PATH_REGEX.test(toolMatch[1])) {
        const clean = toolMatch[1].match(FILE_PATH_REGEX)?.[1]
        if (clean) return clean.replace(/^\.\//, '')
      }
      const genericMatch = line.match(FILE_PATH_REGEX)
      if (genericMatch?.[1]) {
        return genericMatch[1].replace(/^\.\//, '')
      }
    }
  }
  return ''
}

const splitPathName = (fullPath: string) => {
  const normalized = fullPath.replace(/\\/g, '/')
  const slashIdx = normalized.lastIndexOf('/')
  if (slashIdx === -1) {
    return { shortName: normalized, dirPath: '' }
  }
  return {
    shortName: normalized.slice(slashIdx + 1),
    dirPath: normalized.slice(0, slashIdx),
  }
}

const renderMarkdownWithBlocks = (source: string): { html: string; blocks: CodeBlockEntry[] } => {
  const blocks: CodeBlockEntry[] = []

  const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: false,
  })

  md.renderer.rules.fence = (tokens, idx) => {
    const token = tokens[idx]
    const info = token.info ? md.utils.unescapeAll(token.info).trim() : ''
    const langName = info ? info.split(/\s+/g)[0] : ''
    const { label: langLabel, short: shortLang } = formatLangLabel(langName)
    const raw = token.content || ''

    // Gather text from preceding inline tokens (up to 3 tokens back) to detect filename
    let prevText = ''
    for (let i = idx - 1; i >= Math.max(0, idx - 6); i -= 1) {
      const prev = tokens[i]
      if (prev.type === 'fence') break
      if (prev.type === 'inline' && prev.content) {
        prevText = `${prev.content}\n${prevText}`
      }
    }

    let highlightedHtml = ''
    if (langName && hljs.getLanguage(langName)) {
      try {
        highlightedHtml = hljs.highlight(raw, { language: langName, ignoreIllegals: true }).value
      } catch {
        highlightedHtml = md.utils.escapeHtml(raw)
      }
    } else {
      highlightedHtml = md.utils.escapeHtml(raw)
    }

    const blockIdx = blocks.length
    const detectedName = extractFilenameFromContext(info, prevText)
    const fallbackName =
      langName.toLowerCase() === 'html'
        ? 'index.html'
        : langName.toLowerCase() === 'css'
          ? 'style.css'
          : langName.toLowerCase() === 'javascript' || langName.toLowerCase() === 'js'
            ? 'script.js'
            : `${langLabel} #${blockIdx + 1}`

    const displayName = detectedName || fallbackName
    const { shortName, dirPath } = splitPathName(displayName)
    const lineCount = raw ? raw.replace(/\n$/, '').split('\n').length : 0

    blocks.push({
      index: blockIdx,
      raw,
      displayName,
      shortName,
      dirPath,
      langLabel,
      shortLang,
      highlightedHtml,
      lineCount,
    })

    const headerTitleHtml = detectedName
      ? `<span class="code-card-filename" title="${escapeHtmlAttr(detectedName)}">${escapeHtmlAttr(detectedName)}</span><span class="code-card-lang-badge">${escapeHtmlAttr(langLabel)}</span>`
      : `<span class="code-card-lang">${escapeHtmlAttr(langLabel)}</span>`

    return (
      `<div class="code-card" data-code-idx="${blockIdx}">` +
      `<div class="code-card-header">` +
      `<div class="code-card-title-group">${headerTitleHtml}</div>` +
      `<div class="code-card-actions">` +
      `<button type="button" class="code-header-btn code-copy-btn" data-code-idx="${blockIdx}" title="复制代码">` +
      `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>` +
      `<span>复制</span>` +
      `</button>` +
      `<button type="button" class="code-header-btn code-fullscreen-btn" data-code-idx="${blockIdx}" title="全屏查看代码">` +
      `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 3 21 3 21 9"></polyline><polyline points="9 21 3 21 3 15"></polyline><line x1="21" y1="3" x2="14" y2="10"></line><line x1="3" y1="21" x2="10" y2="14"></line></svg>` +
      `<span>全屏</span>` +
      `</button>` +
      `</div>` +
      `</div>` +
      `<pre class="hljs"><code>${highlightedHtml}</code></pre>` +
      `</div>`
    )
  }

  return {
    html: md.render(source),
    blocks,
  }
}

const renderedResult = computed(() => {
  const raw = props.content || ''
  if (!raw.trim()) {
    return { html: '', blocks: [] as CodeBlockEntry[] }
  }
  if (raw.includes('```')) {
    return renderMarkdownWithBlocks(raw)
  }
  const normalized = raw
    .replace(/[“”]/g, '"')
    .replace(/[‘’]/g, "'")
    .trim()
  if (
    (normalized.startsWith('{') && normalized.endsWith('}')) ||
    (normalized.startsWith('[') && normalized.endsWith(']'))
  ) {
    try {
      const parsed = JSON.parse(normalized)
      return renderMarkdownWithBlocks(`\`\`\`json\n${JSON.stringify(parsed, null, 2)}\n\`\`\``)
    } catch {
      return renderMarkdownWithBlocks(`\`\`\`json\n${normalized}\n\`\`\``)
    }
  }
  return renderMarkdownWithBlocks(raw)
})

const showFileSidebar = computed(() => renderedResult.value.blocks.length > 1)

const activeFullscreenBlock = computed(() => {
  if (activeFullscreenIdx.value === null) return null
  return renderedResult.value.blocks[activeFullscreenIdx.value] || null
})

const filteredBlocks = computed(() => {
  const q = fileSearchQuery.value.trim().toLowerCase()
  if (!q) return renderedResult.value.blocks
  return renderedResult.value.blocks.filter(
    (b) => b.displayName.toLowerCase().includes(q) || b.langLabel.toLowerCase().includes(q),
  )
})

const copyText = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}

const selectFullscreenFile = (index: number) => {
  activeFullscreenIdx.value = index
  fullscreenCopied.value = false
  nextTick(() => {
    if (fullscreenBodyRef.value) {
      fullscreenBodyRef.value.scrollTop = 0
      fullscreenBodyRef.value.scrollLeft = 0
    }
  })
}

const handleContentClick = async (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  if (!target) return

  const fullscreenBtn = target.closest('.code-fullscreen-btn') as HTMLElement | null
  if (fullscreenBtn) {
    const idx = Number(fullscreenBtn.dataset.codeIdx)
    if (renderedResult.value.blocks[idx]) {
      fileSearchQuery.value = ''
      selectFullscreenFile(idx)
    }
    return
  }

  const copyBtn = target.closest('.code-copy-btn') as HTMLElement | null
  if (copyBtn) {
    const idx = Number(copyBtn.dataset.codeIdx)
    const entry = renderedResult.value.blocks[idx]
    if (entry) {
      const ok = await copyText(entry.raw)
      if (ok) {
        const label = copyBtn.querySelector('span')
        if (label) {
          const prev = label.textContent
          label.textContent = '已复制'
          window.setTimeout(() => {
            label.textContent = prev || '复制'
          }, 1600)
        }
      }
    }
  }
}

const copyFullscreenCode = async () => {
  if (!activeFullscreenBlock.value) return
  const ok = await copyText(activeFullscreenBlock.value.raw)
  if (ok) {
    fullscreenCopied.value = true
    window.setTimeout(() => {
      fullscreenCopied.value = false
    }, 1600)
  }
}

const closeFullscreen = () => {
  activeFullscreenIdx.value = null
  fullscreenCopied.value = false
  fileSearchQuery.value = ''
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && activeFullscreenIdx.value !== null) {
    closeFullscreen()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.markdown-content {
  color: #1F1F1F;
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 1.2em 0 0.5em;
  color: #1F1F1F;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.015em;
}

.markdown-content :deep(h1) {
  font-size: 1.45em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #E3E3E3;
}

.markdown-content :deep(h2) {
  font-size: 1.25em;
}

.markdown-content :deep(h3) {
  font-size: 1.12em;
}

.markdown-content :deep(p) {
  margin: 0.6em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.6em 0;
  padding-left: 1.4em;
}

.markdown-content :deep(li) {
  margin: 0.3em 0;
}

.markdown-content :deep(code:not(pre code)) {
  padding: 2px 7px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.88em;
  background: #F0F4F9;
  color: #0B57D0;
  border-radius: 8px;
  border: 1px solid #E3E3E3;
}

/* Gemini-style Code Card */
.markdown-content :deep(.code-card) {
  margin: 0.85em 0;
  border-radius: 16px;
  border: 1px solid #E3E3E3;
  background: #F8F9FA;
  overflow: hidden;
}

.markdown-content :deep(.code-card-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 12px 7px 14px;
  background: #F0F4F9;
  border-bottom: 1px solid #E3E3E3;
}

.markdown-content :deep(.code-card-title-group) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.markdown-content :deep(.code-card-filename) {
  font-size: 12px;
  font-weight: 600;
  color: #1F1F1F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.markdown-content :deep(.code-card-lang-badge) {
  padding: 1px 7px;
  border-radius: 999px;
  background: #FFFFFF;
  color: #444746;
  font-size: 10.5px;
  font-weight: 600;
  flex-shrink: 0;
}

.markdown-content :deep(.code-card-lang) {
  font-size: 11.5px;
  font-weight: 600;
  color: #444746;
  letter-spacing: 0.02em;
}

.markdown-content :deep(.code-card-actions) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.markdown-content :deep(.code-header-btn) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #444746;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.markdown-content :deep(.code-header-btn:hover) {
  background: #E2E9F3;
  color: #0B57D0;
}

.markdown-content :deep(pre.hljs) {
  margin: 0;
  padding: 14px 16px;
  border-radius: 0;
  background: #F8F9FA;
  color: #1F1F1F;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.65;
  overflow-x: auto;
  border: none;
  box-shadow: none;
}

.markdown-content :deep(pre.hljs code) {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: inherit;
}

.markdown-content :deep(blockquote) {
  margin: 0.8em 0;
  padding: 10px 16px;
  border-left: 3px solid #0B57D0;
  background: #F8F9FA;
  border-radius: 0 12px 12px 0;
  color: #444746;
}

.markdown-content :deep(table) {
  width: 100%;
  margin: 0.8em 0;
  border-collapse: collapse;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 8px 12px;
  border: 1px solid #E3E3E3;
  font-size: 13px;
}

.markdown-content :deep(th) {
  background: #F8F9FA;
  font-weight: 600;
  color: #1F1F1F;
}

.markdown-content :deep(a) {
  color: #0B57D0;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.markdown-content :deep(hr) {
  margin: 1.5em 0;
  border: 0;
  border-top: 1px solid #E3E3E3;
}
</style>

<style>
/* Fullscreen Code Focus Sheet (Teleported to body) */
.code-fullscreen-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: rgba(31, 31, 31, 0.36);
  backdrop-filter: blur(4px);
}

.code-fullscreen-sheet {
  display: flex;
  flex-direction: column;
  width: min(1320px, 96vw);
  height: min(900px, 93vh);
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 24px;
  box-shadow: 0 20px 56px rgba(60, 64, 67, 0.22);
  overflow: hidden;
}

.code-fullscreen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 20px;
  background: #F0F4F9;
  border-bottom: 1px solid #E3E3E3;
  flex-shrink: 0;
}

.code-fullscreen-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.code-file-title {
  font-size: 14.5px;
  font-weight: 600;
  color: #1F1F1F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-lang-pill {
  padding: 2px 10px;
  border-radius: 999px;
  background: #E8F0FE;
  color: #0B57D0;
  font-size: 11.5px;
  font-weight: 600;
  flex-shrink: 0;
}

.code-line-count {
  font-size: 12px;
  color: #747775;
  flex-shrink: 0;
}

.code-fullscreen-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.fs-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  background: #FFFFFF;
  color: #1F1F1F;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.fs-action-btn:hover {
  border-color: #A8C7FA;
  background: #E8F0FE;
  color: #0B57D0;
}

.fs-action-btn--close {
  border-color: transparent;
  background: #0B57D0;
  color: #FFFFFF;
}

.fs-action-btn--close:hover {
  background: #0842A0;
  border-color: transparent;
  color: #FFFFFF;
}

.code-fullscreen-main {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

/* Left File Explorer Sidebar */
.code-fullscreen-sidebar {
  width: 268px;
  flex: 0 0 268px;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border-right: 1px solid #E3E3E3;
  overflow: hidden;
}

.sidebar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
}

.sidebar-heading {
  font-size: 12.5px;
  font-weight: 600;
  color: #444746;
}

.sidebar-count {
  padding: 1px 8px;
  border-radius: 999px;
  background: #F0F4F9;
  color: #444746;
  font-size: 11px;
  font-weight: 600;
}

.sidebar-search-wrap {
  padding: 0 12px 10px;
}

.sidebar-search-input {
  width: 100%;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  background: #F8F9FA;
  color: #1F1F1F;
  font-size: 12.5px;
  outline: none;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.sidebar-search-input:focus {
  border-color: #0B57D0;
  background: #FFFFFF;
}

.sidebar-file-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-file-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #1F1F1F;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.sidebar-file-item:hover {
  background: #F0F4F9;
}

.sidebar-file-item--active {
  background: #E8F0FE;
  color: #0B57D0;
}

.sidebar-file-item .file-icon {
  flex-shrink: 0;
  color: #747775;
}

.sidebar-file-item--active .file-icon {
  color: #0B57D0;
}

.file-item-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.file-item-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-file-item--active .file-item-name {
  font-weight: 600;
}

.file-item-dir {
  font-size: 11px;
  color: #747775;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-item-lang {
  font-size: 10px;
  font-weight: 600;
  color: #747775;
  background: #F0F4F9;
  padding: 1px 6px;
  border-radius: 999px;
  flex-shrink: 0;
}

.sidebar-file-item--active .file-item-lang {
  background: #FFFFFF;
  color: #0B57D0;
}

.sidebar-empty {
  padding: 20px 12px;
  text-align: center;
  font-size: 12.5px;
  color: #747775;
}

.code-fullscreen-body {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  background: #F8F9FA;
}

.code-fullscreen-pre {
  margin: 0;
  min-height: 100%;
  padding: 22px 26px;
  background: #F8F9FA;
  color: #1F1F1F;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13.5px;
  line-height: 1.7;
  tab-size: 2;
}

@media (max-width: 768px) {
  .code-fullscreen-sidebar {
    width: 190px;
    flex: 0 0 190px;
  }
}
</style>
