<template>
  <div v-if="parsedEvents.length" class="process-group-card">
    <!-- Grouped Header -->
    <div class="process-group-header" @click="groupExpanded = !groupExpanded">
      <div class="process-group-left">
        <span class="group-caret">{{ groupExpanded ? '▼' : '▶' }}</span>
        <span class="group-title">思考与工具调用</span>
        <span class="group-count-pill">{{ parsedEvents.length }} 步</span>
      </div>
      <div class="process-group-actions" @click.stop>
        <button
          type="button"
          class="group-fullscreen-btn"
          title="全屏查看全部思考与工具调用记录"
          @click="openFullscreen(0)"
        >
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 3 21 3 21 9"></polyline>
            <polyline points="9 21 3 21 3 15"></polyline>
            <line x1="21" y1="3" x2="14" y2="10"></line>
            <line x1="3" y1="21" x2="10" y2="14"></line>
          </svg>
          <span>全屏查看</span>
        </button>
      </div>
    </div>

    <!-- Compact Expandable Step List -->
    <div v-show="groupExpanded" class="process-steps-list">
      <div
        v-for="(item, idx) in parsedEvents"
        :key="item.id"
        class="process-step-card"
      >
        <div class="step-row-header" @click="toggleInlineStep(item.id)">
          <div class="step-row-left">
            <span class="step-kind-dot" :class="`dot--${item.kind}`"></span>
            <span class="step-action-badge" :class="`badge--${item.kind}`">{{ item.actionLabel }}</span>
            <span v-if="item.targetFile" class="step-target-file" :title="item.targetFile">
              {{ item.targetFile }}
            </span>
            <span v-else-if="item.subtitle" class="step-subtitle" :title="item.subtitle">
              {{ item.subtitle }}
            </span>
          </div>
          <div class="step-row-right">
            <span v-if="item.shortTime" class="step-time">{{ item.shortTime }}</span>
            <button
              type="button"
              class="step-fs-icon-btn"
              title="在全屏中查看此步骤"
              @click.stop="openFullscreen(idx)"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="15 3 21 3 21 9"></polyline>
                <polyline points="9 21 3 21 3 15"></polyline>
                <line x1="21" y1="3" x2="14" y2="10"></line>
                <line x1="3" y1="21" x2="10" y2="14"></line>
              </svg>
            </button>
          </div>
        </div>

        <!-- Inline Structured Body -->
        <div v-if="expandedStepIds.has(item.id)" class="step-inline-body">
          <!-- 1. Thinking -->
          <div v-if="item.kind === 'thinking'" class="formatted-prose">
            <p v-for="(para, pIdx) in item.thinkingParagraphs" :key="pIdx">{{ para }}</p>
          </div>

          <!-- 2. updatePlan -->
          <div v-else-if="item.planItems?.length" class="formatted-plan">
            <div class="plan-summary-bar">
              <span class="plan-summary-text">进度 {{ getPlanCompletedCount(item.planItems) }} / {{ item.planItems.length }}</span>
              <div class="plan-progress-track">
                <div class="plan-progress-fill" :style="{ width: getPlanProgressPercent(item.planItems) + '%' }"></div>
              </div>
            </div>
            <div
              v-for="(task, tIdx) in item.planItems"
              :key="task.id"
              class="formatted-plan-row"
            >
              <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
              <span class="plan-pill" :class="`plan-pill--${task.status}`">
                {{ formatPlanStatus(task.status) }}
              </span>
              <span class="plan-text">{{ formatCleanTaskText(task.text) }}</span>
              <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
            </div>
          </div>

          <!-- 3. writeFile -->
          <div v-else-if="item.codePreview" class="formatted-file-op">
            <div class="file-op-meta">
              <span class="meta-label">目标文件</span>
              <code class="meta-path">{{ item.targetFile || '未命名文件' }}</code>
              <span class="meta-lines">{{ item.codePreview.lineCount }} 行</span>
            </div>
            <pre class="hljs formatted-code-box"><code v-html="item.codePreview.highlightedHtml"></code></pre>
            <div v-if="item.resultText" class="tool-result-note">{{ item.resultText }}</div>
            <div v-if="item.nagReminder" class="nag-reminder-card">
              <div class="nag-reminder-header">
                <span class="nag-badge">Nag 机制 · 计划更新提醒</span>
                <span v-if="item.nagReminder.progressSummary" class="nag-progress-pill">{{ item.nagReminder.progressSummary }}</span>
              </div>
              <p v-if="item.nagReminder.headline" class="nag-headline">{{ item.nagReminder.headline }}</p>
              <div v-if="item.nagReminder.tasks.length" class="nag-task-list">
                <div v-for="(task, tIdx) in item.nagReminder.tasks" :key="task.id" class="nag-task-row">
                  <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
                  <span class="plan-pill" :class="`plan-pill--${task.status}`">{{ formatPlanStatus(task.status) }}</span>
                  <span class="plan-text">{{ task.text }}</span>
                  <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 4. modifyFile -->
          <div v-else-if="item.diffPreview" class="formatted-file-op">
            <div v-if="item.targetFile" class="file-op-meta">
              <span class="meta-label">修改文件</span>
              <code class="meta-path">{{ item.targetFile }}</code>
            </div>
            <div class="diff-block diff-block--old">
              <div class="diff-block-label">替换前</div>
              <pre class="hljs formatted-code-box"><code v-html="item.diffPreview.oldHtml"></code></pre>
            </div>
            <div class="diff-block diff-block--new">
              <div class="diff-block-label">替换后</div>
              <pre class="hljs formatted-code-box"><code v-html="item.diffPreview.newHtml"></code></pre>
            </div>
            <div v-if="item.resultText" class="tool-result-note">{{ item.resultText }}</div>
            <div v-if="item.nagReminder" class="nag-reminder-card">
              <div class="nag-reminder-header">
                <span class="nag-badge">Nag 机制 · 计划更新提醒</span>
                <span v-if="item.nagReminder.progressSummary" class="nag-progress-pill">{{ item.nagReminder.progressSummary }}</span>
              </div>
              <p v-if="item.nagReminder.headline" class="nag-headline">{{ item.nagReminder.headline }}</p>
              <div v-if="item.nagReminder.tasks.length" class="nag-task-list">
                <div v-for="(task, tIdx) in item.nagReminder.tasks" :key="task.id" class="nag-task-row">
                  <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
                  <span class="plan-pill" :class="`plan-pill--${task.status}`">{{ formatPlanStatus(task.status) }}</span>
                  <span class="plan-text">{{ task.text }}</span>
                  <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 5. Generic Tool / System / Error -->
          <div v-else class="formatted-generic">
            <div v-if="item.paramEntries.length" class="param-kv-list">
              <div v-for="kv in item.paramEntries" :key="kv.key" class="param-kv-row">
                <span class="param-k">{{ kv.key }}</span>
                <span class="param-v">{{ kv.value }}</span>
              </div>
            </div>
            <div v-if="item.resultText" class="tool-result-box">
              <div class="result-box-label">执行结果</div>
              <pre class="result-box-pre">{{ item.resultText }}</pre>
            </div>
            <div v-else-if="!item.paramEntries.length && item.rawFallback" class="formatted-prose">
              <p>{{ item.rawFallback }}</p>
            </div>
            <div v-if="item.nagReminder" class="nag-reminder-card">
              <div class="nag-reminder-header">
                <span class="nag-badge">Nag 机制 · 计划更新提醒</span>
                <span v-if="item.nagReminder.progressSummary" class="nag-progress-pill">{{ item.nagReminder.progressSummary }}</span>
              </div>
              <p v-if="item.nagReminder.headline" class="nag-headline">{{ item.nagReminder.headline }}</p>
              <div v-if="item.nagReminder.tasks.length" class="nag-task-list">
                <div v-for="(task, tIdx) in item.nagReminder.tasks" :key="task.id" class="nag-task-row">
                  <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
                  <span class="plan-pill" :class="`plan-pill--${task.status}`">{{ formatPlanStatus(task.status) }}</span>
                  <span class="plan-text">{{ task.text }}</span>
                  <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Fullscreen Process & Tool Calls Inspector -->
    <Teleport to="body">
      <div
        v-if="activeFullscreenItem"
        class="process-fs-backdrop"
        role="dialog"
        aria-modal="true"
        aria-label="全屏查看思考与工具调用"
        @click.self="closeFullscreen"
      >
        <div class="process-fs-sheet">
          <header class="process-fs-header">
            <div class="process-fs-title">
              <span class="step-action-badge" :class="`badge--${activeFullscreenItem.kind}`">
                {{ activeFullscreenItem.actionLabel }}
              </span>
              <span class="process-fs-main-name">
                {{ activeFullscreenItem.targetFile || activeFullscreenItem.subtitle || activeFullscreenItem.actionLabel }}
              </span>
              <span v-if="activeFullscreenItem.shortTime" class="process-fs-time">
                {{ activeFullscreenItem.shortTime }}
              </span>
            </div>
            <div class="process-fs-actions">
              <button type="button" class="fs-pill-btn" @click="copyActiveContent">
                <span>{{ copiedFullscreen ? '已复制' : '复制内容' }}</span>
              </button>
              <button type="button" class="fs-pill-btn fs-pill-btn--primary" title="退出全屏 (Esc)" @click="closeFullscreen">
                <span>退出全屏</span>
              </button>
            </div>
          </header>

          <div class="process-fs-main">
            <!-- Left Sidebar: All Tool Calls & Thinking Steps -->
            <aside v-if="parsedEvents.length > 1" class="process-fs-sidebar" aria-label="执行步骤列表">
              <div class="pfs-sidebar-top">
                <span class="pfs-sidebar-heading">工具与过程记录</span>
                <span class="pfs-sidebar-count">{{ parsedEvents.length }}</span>
              </div>
              <div v-if="parsedEvents.length > 5" class="pfs-search-wrap">
                <input
                  v-model="searchQuery"
                  type="text"
                  class="pfs-search-input"
                  placeholder="筛选工具或文件名..."
                />
              </div>
              <div class="pfs-step-list">
                <button
                  v-for="entry in filteredEvents"
                  :key="entry.id"
                  type="button"
                  class="pfs-step-item"
                  :class="{ 'pfs-step-item--active': entry.index === activeFullscreenIdx }"
                  :title="entry.targetFile || entry.subtitle || entry.actionLabel"
                  @click="selectFullscreenStep(entry.index)"
                >
                  <span class="step-kind-dot" :class="`dot--${entry.kind}`"></span>
                  <span class="pfs-step-meta">
                    <span class="pfs-step-title">{{ entry.sidebarTitle }}</span>
                    <span v-if="entry.sidebarSub" class="pfs-step-sub">{{ entry.sidebarSub }}</span>
                  </span>
                </button>
                <div v-if="!filteredEvents.length" class="pfs-empty">无匹配记录</div>
              </div>
            </aside>

            <!-- Right Detail Pane -->
            <div ref="fsBodyRef" class="process-fs-body">
              <!-- Thinking -->
              <div v-if="activeFullscreenItem.kind === 'thinking'" class="fs-section-card">
                <h4 class="fs-section-heading">模型思考过程</h4>
                <div class="formatted-prose formatted-prose--lg">
                  <p v-for="(para, pIdx) in activeFullscreenItem.thinkingParagraphs" :key="pIdx">{{ para }}</p>
                </div>
              </div>

              <!-- updatePlan -->
              <div v-else-if="activeFullscreenItem.planItems?.length" class="fs-section-card">
                <div class="fs-plan-header">
                  <h4 class="fs-section-heading" style="margin: 0">任务执行计划（共 {{ activeFullscreenItem.planItems.length }} 项）</h4>
                  <span class="fs-plan-percent">
                    已完成 {{ getPlanCompletedCount(activeFullscreenItem.planItems) }} / {{ activeFullscreenItem.planItems.length }}
                  </span>
                </div>
                <div class="plan-progress-track plan-progress-track--lg">
                  <div class="plan-progress-fill" :style="{ width: getPlanProgressPercent(activeFullscreenItem.planItems) + '%' }"></div>
                </div>
                <div class="formatted-plan" style="margin-top: 14px">
                  <div
                    v-for="(task, tIdx) in activeFullscreenItem.planItems"
                    :key="task.id"
                    class="formatted-plan-row formatted-plan-row--lg"
                  >
                    <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
                    <span class="plan-pill" :class="`plan-pill--${task.status}`">
                      {{ formatPlanStatus(task.status) }}
                    </span>
                    <span class="plan-text">{{ formatCleanTaskText(task.text) }}</span>
                    <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
                  </div>
                </div>
              </div>

              <!-- writeFile -->
              <div v-else-if="activeFullscreenItem.codePreview" class="fs-section-card">
                <div class="file-op-meta file-op-meta--lg">
                  <span class="meta-label">写入路径</span>
                  <code class="meta-path">{{ activeFullscreenItem.targetFile || '未命名文件' }}</code>
                  <span class="meta-lines">{{ activeFullscreenItem.codePreview.lineCount }} 行</span>
                </div>
                <pre class="hljs formatted-code-box formatted-code-box--fs"><code v-html="activeFullscreenItem.codePreview.highlightedHtml"></code></pre>
                <div v-if="activeFullscreenItem.resultText" class="tool-result-note" style="margin-top: 12px">
                  {{ activeFullscreenItem.resultText }}
                </div>
                <div v-if="activeFullscreenItem.nagReminder" class="nag-reminder-card nag-reminder-card--lg">
                  <div class="nag-reminder-header">
                    <span class="nag-badge">Nag 机制 · 计划更新提醒</span>
                    <span v-if="activeFullscreenItem.nagReminder.progressSummary" class="nag-progress-pill">{{ activeFullscreenItem.nagReminder.progressSummary }}</span>
                  </div>
                  <p v-if="activeFullscreenItem.nagReminder.headline" class="nag-headline">{{ activeFullscreenItem.nagReminder.headline }}</p>
                  <div v-if="activeFullscreenItem.nagReminder.tasks.length" class="nag-task-list">
                    <div v-for="(task, tIdx) in activeFullscreenItem.nagReminder.tasks" :key="task.id" class="nag-task-row">
                      <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
                      <span class="plan-pill" :class="`plan-pill--${task.status}`">{{ formatPlanStatus(task.status) }}</span>
                      <span class="plan-text">{{ task.text }}</span>
                      <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- modifyFile -->
              <div v-else-if="activeFullscreenItem.diffPreview" class="fs-section-card">
                <div v-if="activeFullscreenItem.targetFile" class="file-op-meta file-op-meta--lg">
                  <span class="meta-label">修改路径</span>
                  <code class="meta-path">{{ activeFullscreenItem.targetFile }}</code>
                </div>
                <div class="diff-block diff-block--old">
                  <div class="diff-block-label">替换前</div>
                  <pre class="hljs formatted-code-box formatted-code-box--fs"><code v-html="activeFullscreenItem.diffPreview.oldHtml"></code></pre>
                </div>
                <div class="diff-block diff-block--new" style="margin-top: 14px">
                  <div class="diff-block-label">替换后</div>
                  <pre class="hljs formatted-code-box formatted-code-box--fs"><code v-html="activeFullscreenItem.diffPreview.newHtml"></code></pre>
                </div>
                <div v-if="activeFullscreenItem.resultText" class="tool-result-note" style="margin-top: 12px">
                  {{ activeFullscreenItem.resultText }}
                </div>
                <div v-if="activeFullscreenItem.nagReminder" class="nag-reminder-card nag-reminder-card--lg">
                  <div class="nag-reminder-header">
                    <span class="nag-badge">Nag 机制 · 计划更新提醒</span>
                    <span v-if="activeFullscreenItem.nagReminder.progressSummary" class="nag-progress-pill">{{ activeFullscreenItem.nagReminder.progressSummary }}</span>
                  </div>
                  <p v-if="activeFullscreenItem.nagReminder.headline" class="nag-headline">{{ activeFullscreenItem.nagReminder.headline }}</p>
                  <div v-if="activeFullscreenItem.nagReminder.tasks.length" class="nag-task-list">
                    <div v-for="(task, tIdx) in activeFullscreenItem.nagReminder.tasks" :key="task.id" class="nag-task-row">
                      <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
                      <span class="plan-pill" :class="`plan-pill--${task.status}`">{{ formatPlanStatus(task.status) }}</span>
                      <span class="plan-text">{{ task.text }}</span>
                      <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Generic Tool / System / Error -->
              <div v-else class="fs-section-card">
                <h4 class="fs-section-heading">{{ activeFullscreenItem.actionLabel }} 详情</h4>
                <div v-if="activeFullscreenItem.paramEntries.length" class="param-kv-list" style="margin-bottom: 14px">
                  <div v-for="kv in activeFullscreenItem.paramEntries" :key="kv.key" class="param-kv-row">
                    <span class="param-k">{{ kv.key }}</span>
                    <span class="param-v">{{ kv.value }}</span>
                  </div>
                </div>
                <div v-if="activeFullscreenItem.resultText" class="tool-result-box">
                  <div class="result-box-label">执行结果</div>
                  <pre class="result-box-pre">{{ activeFullscreenItem.resultText }}</pre>
                </div>
                <div v-else-if="activeFullscreenItem.rawFallback" class="formatted-prose formatted-prose--lg">
                  <p>{{ activeFullscreenItem.rawFallback }}</p>
                </div>
                <div v-if="activeFullscreenItem.nagReminder" class="nag-reminder-card nag-reminder-card--lg">
                  <div class="nag-reminder-header">
                    <span class="nag-badge">Nag 机制 · 计划更新提醒</span>
                    <span v-if="activeFullscreenItem.nagReminder.progressSummary" class="nag-progress-pill">{{ activeFullscreenItem.nagReminder.progressSummary }}</span>
                  </div>
                  <p v-if="activeFullscreenItem.nagReminder.headline" class="nag-headline">{{ activeFullscreenItem.nagReminder.headline }}</p>
                  <div v-if="activeFullscreenItem.nagReminder.tasks.length" class="nag-task-list">
                    <div v-for="(task, tIdx) in activeFullscreenItem.nagReminder.tasks" :key="task.id" class="nag-task-row">
                      <span class="plan-step-num">{{ formatCleanStepNum(task.id, tIdx) }}</span>
                      <span class="plan-pill" :class="`plan-pill--${task.status}`">{{ formatPlanStatus(task.status) }}</span>
                      <span class="plan-text">{{ task.text }}</span>
                      <span v-if="task.deps?.length" class="plan-deps">前置：{{ formatCleanDeps(task.deps) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import hljs from 'highlight.js'

export interface RawProcessEvent {
  id: string
  type: 'thinking' | 'tool' | 'system' | 'error'
  title: string
  summary: string
  details?: string
  time: string
}

interface PlanTask {
  id: string
  text: string
  status: string
  deps?: string[]
}

interface NagReminderInfo {
  headline: string
  tasks: PlanTask[]
  progressSummary: string
}

interface ParsedProcessEvent {
  index: number
  id: string
  kind: 'thinking' | 'plan' | 'file' | 'tool' | 'error'
  actionLabel: string
  targetFile: string
  subtitle: string
  sidebarTitle: string
  sidebarSub: string
  shortTime: string
  thinkingParagraphs: string[]
  planItems?: PlanTask[]
  codePreview?: { highlightedHtml: string; raw: string; lineCount: number }
  diffPreview?: { oldHtml: string; newHtml: string; raw: string }
  paramEntries: Array<{ key: string; value: string }>
  resultText: string
  nagReminder?: NagReminderInfo
  rawFallback: string
  copyableText: string
}

const props = defineProps<{
  events: RawProcessEvent[]
}>()

const groupExpanded = ref(true)
const expandedStepIds = ref<Set<string>>(new Set())
const activeFullscreenIdx = ref<number | null>(null)
const copiedFullscreen = ref(false)
const searchQuery = ref('')
const fsBodyRef = ref<HTMLElement>()

const TOOL_LABELS: Record<string, string> = {
  writeFile: '写入文件',
  modifyFile: '修改文件',
  readFile: '读取文件',
  readDir: '读取目录',
  deleteFile: '删除文件',
  updatePlan: '更新计划',
  exit: '结束执行',
}

const escapeHtml = (str: string) =>
  str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

const detectLangFromFile = (filename?: string): string => {
  if (!filename) return ''
  const lower = filename.toLowerCase()
  if (lower.endsWith('.vue') || lower.endsWith('.html') || lower.endsWith('.htm')) return 'xml'
  if (lower.endsWith('.css') || lower.endsWith('.scss') || lower.endsWith('.less')) return 'css'
  if (lower.endsWith('.ts') || lower.endsWith('.tsx')) return 'typescript'
  if (lower.endsWith('.js') || lower.endsWith('.mjs') || lower.endsWith('.jsx')) return 'javascript'
  if (lower.endsWith('.json')) return 'json'
  if (lower.endsWith('.java')) return 'java'
  if (lower.endsWith('.sql')) return 'sql'
  return ''
}

const highlightCode = (code: string, filename?: string): string => {
  const lang = detectLangFromFile(filename)
  if (lang && hljs.getLanguage(lang)) {
    try {
      return hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
    } catch {
      return escapeHtml(code)
    }
  }
  return escapeHtml(code)
}

const formatShortTime = (time?: string) => {
  if (!time) return ''
  const tIdx = time.indexOf('T')
  if (tIdx > 0 && time.length >= tIdx + 9) {
    return time.slice(tIdx + 1, tIdx + 9)
  }
  const spaceIdx = time.indexOf(' ')
  if (spaceIdx > 0) {
    return time.slice(spaceIdx + 1, spaceIdx + 9)
  }
  return time
}

const formatPlanStatus = (status: string) => {
  if (status === 'completed') return '已完成'
  if (status === 'in_progress') return '进行中'
  return '待处理'
}

const formatCleanStepNum = (id: string | undefined, fallbackIdx: number) => {
  const cleaned = (id || '').replace(/#/g, '').trim()
  return cleaned || String(fallbackIdx + 1)
}

const formatCleanTaskText = (text: string | undefined) => {
  return (text || '').replace(/^#+\s*/, '').replace(/\[[^\]]*\]\s*/g, '').trim()
}

const formatCleanDeps = (deps: string[] | undefined) => {
  if (!deps?.length) return ''
  return deps
    .map((d) => `步骤 ${d.replace(/#/g, '').trim()}`)
    .join('、')
}

const getPlanCompletedCount = (items: PlanTask[]) =>
  items.filter((i) => i.status === 'completed').length

const getPlanProgressPercent = (items: PlanTask[]) => {
  if (!items.length) return 0
  return Math.round((getPlanCompletedCount(items) / items.length) * 100)
}

const parseNagReminder = (reminderBody: string): NagReminderInfo => {
  const text = reminderBody.trim()
  const progressMatch = text.match(/\((\d+\/\d+\s*已完成)\)/)
  const progressSummary = progressMatch?.[1] || ''
  const withoutProgress = text.replace(/\(\d+\/\d+\s*已完成\)/g, '').trim()

  const planIdx = withoutProgress.indexOf('当前计划：')
  const headline = (planIdx >= 0 ? withoutProgress.slice(0, planIdx) : withoutProgress).trim()
  const planSection = planIdx >= 0 ? withoutProgress.slice(planIdx + '当前计划：'.length).trim() : ''

  const tasks: PlanTask[] = []
  if (planSection) {
    const taskRegex = /\[(x|>|\s)\]\s*#+(\d+)\s*([\s\S]*?)(?=\[(?:x|>|\s)\]\s*#+\d+|$)/gi
    let match: RegExpExecArray | null
    while ((match = taskRegex.exec(planSection)) !== null) {
      const marker = match[1].toLowerCase()
      const status = marker === 'x' ? 'completed' : marker === '>' ? 'in_progress' : 'pending'
      const id = match[2]
      let body = match[3].trim()
      const deps: string[] = []
      const depMatch = body.match(/[（(]依赖[:：]\s*([^）)]+)[）)]/)
      if (depMatch?.[1]) {
        depMatch[1]
          .split(/[,，、\s]+/)
          .map((d) => d.replace(/#/g, '').trim())
          .filter(Boolean)
          .forEach((d) => deps.push(d))
        body = body.replace(depMatch[0], '').trim()
      }
      tasks.push({ id, text: body, status, deps })
    }
  }
  return { headline, tasks, progressSummary }
}

const splitResultAndReminder = (raw: string): { resultText: string; nagReminder?: NagReminderInfo } => {
  if (!raw) return { resultText: '' }
  const reminderMatch = raw.match(/<reminder>([\s\S]*?)<\/reminder>/i)
  const nagReminder = reminderMatch?.[1] ? parseNagReminder(reminderMatch[1]) : undefined
  const cleanedResult = raw
    .replace(/<reminder>[\s\S]*?<\/reminder>/gi, '')
    .replace(/^\[(?:选择工具|工具调用|工具结果|执行结束)\]\s*/gm, '')
    .replace(/\[(?:x|>|\s)\]\s*/gi, '')
    .replace(/#+(\d+)/g, '步骤 $1')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
  return { resultText: cleanedResult, nagReminder }
}

const formatThinkingParagraphs = (raw: string): string[] => {
  if (!raw) return []
  const normalized = raw
    .replace(/<reminder>[\s\S]*?<\/reminder>/gi, '')
    .replace(/([^\n\d])(\d+\.\s)/g, '$1\n$2')
    .replace(/([^\n])(-\s+#\d+\s)/g, '$1\n$2')
    .replace(/([a-z.)])((?:Now |Let me |Good, |Next, |First, |Finally, ))/g, '$1\n\n$2')
    .trim()

  return normalized
    .split(/\n{1,}/)
    .map((line) =>
      line
        .trim()
        .replace(/^[-*]\s*#+(\d+)\s*/, '步骤 $1：')
        .replace(/#+(\d+)/g, '步骤 $1')
        .replace(/\[(?:x|>|\s)\]\s*/gi, '')
        .replace(/\*\*(.*?)\*\*/g, '$1')
        .trim(),
    )
    .filter(Boolean)
}

const tryParseJson = (str: string): Record<string, any> | null => {
  if (!str) return null
  const trimmed = str.trim()
  if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) return null
  try {
    return JSON.parse(trimmed)
  } catch {
    return null
  }
}

const extractToolParts = (details: string, fallbackSummary: string) => {
  let toolName = ''
  let argsRaw = ''
  let resultRaw = ''

  const lines = details.split('\n')
  let mode: 'none' | 'args' | 'result' = 'none'
  const resultLines: string[] = []

  for (const line of lines) {
    const trimmed = line.trim()
    if (/^工具:\s*/.test(trimmed)) {
      const name = trimmed.replace(/^工具:\s*/, '').trim()
      if (!toolName) toolName = name
      mode = 'none'
      continue
    }
    if (/^参数:\s*/.test(trimmed)) {
      const candidate = trimmed.replace(/^参数:\s*/, '').trim()
      // Only take the first non-empty 参数 block to avoid TOOL_REQUEST + TOOL_RESULT duplication
      if (!argsRaw && candidate) {
        argsRaw = candidate
        mode = 'args'
      } else {
        mode = 'none'
      }
      continue
    }
    if (/^结果:\s*/.test(trimmed)) {
      const firstResult = trimmed.replace(/^结果:\s*/, '').trim()
      if (firstResult) resultLines.push(firstResult)
      mode = 'result'
      continue
    }
    if (/^\[(选择工具|工具调用|工具结果|执行结束)\]/.test(trimmed)) {
      mode = 'none'
      continue
    }
    if (mode === 'args' && argsRaw) {
      argsRaw += '\n' + line
    } else if (mode === 'result') {
      resultLines.push(line)
    }
  }

  const { resultText: parsedResult, nagReminder } = splitResultAndReminder(resultLines.join('\n').trim())
  resultRaw = parsedResult
  if (!toolName) {
    for (const key of Object.keys(TOOL_LABELS)) {
      if (fallbackSummary.includes(key) || fallbackSummary.includes(TOOL_LABELS[key])) {
        toolName = key
        break
      }
    }
  }
  return { toolName, argsRaw, resultRaw, nagReminder }
}

const parsedEvents = computed<ParsedProcessEvent[]>(() => {
  return (props.events || []).map((ev, index) => {
    const details = ev.details || ''
    const shortTime = formatShortTime(ev.time)

    // 1. Thinking
    if (ev.type === 'thinking') {
      const paragraphs = formatThinkingParagraphs(details || ev.summary || '')
      const cleanSub = (paragraphs[0] || ev.summary || '分析需求与架构').slice(0, 64)
      return {
        index,
        id: ev.id,
        kind: 'thinking',
        actionLabel: '思考过程',
        targetFile: '',
        subtitle: cleanSub,
        sidebarTitle: '思考过程',
        sidebarSub: cleanSub,
        shortTime,
        thinkingParagraphs: paragraphs,
        paramEntries: [],
        resultText: '',
        rawFallback: details,
        copyableText: paragraphs.join('\n\n'),
      }
    }

    // 2. Error
    if (ev.type === 'error') {
      return {
        index,
        id: ev.id,
        kind: 'error',
        actionLabel: '执行异常',
        targetFile: '',
        subtitle: ev.summary || '发生异常',
        sidebarTitle: '执行异常',
        sidebarSub: ev.summary || '',
        shortTime,
        thinkingParagraphs: [],
        paramEntries: [],
        resultText: details,
        rawFallback: details,
        copyableText: details || ev.summary,
      }
    }

    // 3. Tool calls
    const { toolName, argsRaw, resultRaw, nagReminder } = extractToolParts(details, ev.summary || '')
    const parsedArgs = tryParseJson(argsRaw)
    const actionLabel = TOOL_LABELS[toolName] || toolName || '工具调用'

    // Extract target file path if present
    const targetFile: string =
      parsedArgs?.relativeFilePath ||
      parsedArgs?.filePath ||
      parsedArgs?.path ||
      parsedArgs?.relativeDirPath ||
      parsedArgs?.dirPath ||
      ''

    // 3a. updatePlan
    if (toolName === 'updatePlan' || Array.isArray(parsedArgs?.items)) {
      const items: PlanTask[] = Array.isArray(parsedArgs?.items)
        ? parsedArgs.items
        : Array.isArray(parsedArgs)
          ? parsedArgs
          : []
      const doneCount = items.filter((i) => i.status === 'completed').length
      const sub = items.length ? `共 ${items.length} 项任务 · 已完成 ${doneCount}/${items.length}` : '更新任务计划'
      return {
        index,
        id: ev.id,
        kind: 'plan',
        actionLabel: '更新计划',
        targetFile: '',
        subtitle: sub,
        sidebarTitle: '更新计划',
        sidebarSub: sub,
        shortTime,
        thinkingParagraphs: [],
        planItems: items,
        paramEntries: [],
        resultText: resultRaw,
        nagReminder,
        rawFallback: details,
        copyableText: items.map((i, idx) => `步骤 ${formatCleanStepNum(i.id, idx)}（${formatPlanStatus(i.status)}）：${formatCleanTaskText(i.text)}`).join('\n') || details,
      }
    }

    // 3b. writeFile with content
    if (toolName === 'writeFile' && typeof parsedArgs?.content === 'string') {
      const codeRaw = parsedArgs.content
      const lineCount = codeRaw ? codeRaw.replace(/\n$/, '').split('\n').length : 0
      const shortName = targetFile.includes('/') ? targetFile.slice(targetFile.lastIndexOf('/') + 1) : targetFile
      return {
        index,
        id: ev.id,
        kind: 'file',
        actionLabel: '写入文件',
        targetFile: targetFile || '未命名文件',
        subtitle: `${lineCount} 行`,
        sidebarTitle: shortName || '写入文件',
        sidebarSub: targetFile && targetFile !== shortName ? targetFile : `写入文件 · ${lineCount} 行`,
        shortTime,
        thinkingParagraphs: [],
        codePreview: {
          highlightedHtml: highlightCode(codeRaw, targetFile),
          raw: codeRaw,
          lineCount,
        },
        paramEntries: [],
        resultText: resultRaw,
        nagReminder,
        rawFallback: details,
        copyableText: codeRaw,
      }
    }

    // 3c. modifyFile with old/new content
    const oldCode = parsedArgs?.oldContent ?? parsedArgs?.targetContent ?? parsedArgs?.oldText
    const newCode = parsedArgs?.newContent ?? parsedArgs?.replacementContent ?? parsedArgs?.newText
    if (toolName === 'modifyFile' && typeof oldCode === 'string' && typeof newCode === 'string') {
      const shortName = targetFile.includes('/') ? targetFile.slice(targetFile.lastIndexOf('/') + 1) : targetFile
      return {
        index,
        id: ev.id,
        kind: 'file',
        actionLabel: '修改文件',
        targetFile: targetFile || '修改目标文件',
        subtitle: '局部替换',
        sidebarTitle: shortName || '修改文件',
        sidebarSub: targetFile && targetFile !== shortName ? `修改 · ${targetFile}` : '修改文件',
        shortTime,
        thinkingParagraphs: [],
        diffPreview: {
          oldHtml: highlightCode(oldCode, targetFile),
          newHtml: highlightCode(newCode, targetFile),
          raw: `--- 替换前 ---\n${oldCode}\n\n+++ 替换后 +++\n${newCode}`,
        },
        paramEntries: [],
        resultText: resultRaw,
        nagReminder,
        rawFallback: details,
        copyableText: newCode,
      }
    }

    // 3d. Other tools (readFile, readDir, deleteFile, exit, etc.)
    const paramEntries: Array<{ key: string; value: string }> = []
    if (parsedArgs && typeof parsedArgs === 'object' && !Array.isArray(parsedArgs)) {
      for (const [k, v] of Object.entries(parsedArgs)) {
        const valStr = typeof v === 'string' ? v : JSON.stringify(v)
        paramEntries.push({ key: k, value: valStr })
      }
    }
    const shortName = targetFile
      ? (targetFile.includes('/') ? targetFile.slice(targetFile.lastIndexOf('/') + 1) : targetFile)
      : ''

    return {
      index,
      id: ev.id,
      kind: targetFile ? 'file' : 'tool',
      actionLabel,
      targetFile,
      subtitle: targetFile ? '' : (resultRaw ? resultRaw.split('\n')[0].slice(0, 48) : ev.summary || ''),
      sidebarTitle: shortName ? `${actionLabel} · ${shortName}` : actionLabel,
      sidebarSub: targetFile && targetFile !== shortName ? targetFile : (shortTime || ''),
      shortTime,
      thinkingParagraphs: [],
      paramEntries,
      resultText: resultRaw,
      nagReminder,
      rawFallback: details,
      copyableText: resultRaw || argsRaw || details,
    }
  })
})

const activeFullscreenItem = computed(() => {
  if (activeFullscreenIdx.value === null) return null
  return parsedEvents.value[activeFullscreenIdx.value] || null
})

const filteredEvents = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return parsedEvents.value
  return parsedEvents.value.filter(
    (e) =>
      e.actionLabel.toLowerCase().includes(q) ||
      e.targetFile.toLowerCase().includes(q) ||
      e.sidebarTitle.toLowerCase().includes(q) ||
      e.subtitle.toLowerCase().includes(q),
  )
})

const toggleInlineStep = (id: string) => {
  const next = new Set(expandedStepIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedStepIds.value = next
}

const openFullscreen = (idx: number) => {
  searchQuery.value = ''
  selectFullscreenStep(idx)
}

const selectFullscreenStep = (idx: number) => {
  activeFullscreenIdx.value = idx
  copiedFullscreen.value = false
  nextTick(() => {
    if (fsBodyRef.value) {
      fsBodyRef.value.scrollTop = 0
    }
  })
}

const closeFullscreen = () => {
  activeFullscreenIdx.value = null
  copiedFullscreen.value = false
  searchQuery.value = ''
}

const copyActiveContent = async () => {
  if (!activeFullscreenItem.value) return
  try {
    await navigator.clipboard.writeText(activeFullscreenItem.value.copyableText)
    copiedFullscreen.value = true
    window.setTimeout(() => {
      copiedFullscreen.value = false
    }, 1600)
  } catch {
    // ignore
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && activeFullscreenIdx.value !== null) {
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
.process-group-card {
  border: 1px solid #E3E3E3;
  border-radius: 16px;
  background: #FFFFFF;
  overflow: hidden;
}

.process-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 12px 9px 14px;
  background: #F8F9FA;
  border-bottom: 1px solid #E3E3E3;
  cursor: pointer;
  user-select: none;
}

.process-group-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.group-caret {
  font-size: 10.5px;
  color: #747775;
}

.group-title {
  font-size: 13px;
  font-weight: 600;
  color: #1F1F1F;
}

.group-count-pill {
  padding: 1px 8px;
  border-radius: 999px;
  background: #E8F0FE;
  color: #0B57D0;
  font-size: 11px;
  font-weight: 600;
}

.group-fullscreen-btn {
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

.group-fullscreen-btn:hover {
  background: #E8F0FE;
  color: #0B57D0;
}

.process-steps-list {
  display: flex;
  flex-direction: column;
  padding: 6px;
  gap: 5px;
}

.process-step-card {
  border: 1px solid #E3E3E3;
  border-radius: 12px;
  background: #FFFFFF;
  overflow: hidden;
}

.step-row-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 10px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.step-row-header:hover {
  background: #F8F9FA;
}

.step-row-left {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  flex: 1;
}

.step-kind-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot--thinking {
  background: #9B72CB;
}

.dot--plan {
  background: #0B57D0;
}

.dot--file {
  background: #146C2E;
}

.dot--tool {
  background: #444746;
}

.dot--error {
  background: #B3261E;
}

.step-action-badge {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.badge--thinking {
  background: #F3E8FD;
  color: #6B21A8;
}

.badge--plan {
  background: #E8F0FE;
  color: #0B57D0;
}

.badge--file {
  background: #E6F4EA;
  color: #146C2E;
}

.badge--tool {
  background: #F0F4F9;
  color: #444746;
}

.badge--error {
  background: #FCE8E6;
  color: #B3261E;
}

.step-target-file {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #1F1F1F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-subtitle {
  font-size: 12px;
  color: #444746;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-row-right {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.step-time {
  font-size: 11px;
  color: #747775;
}

.step-fs-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #747775;
  cursor: pointer;
}

.step-fs-icon-btn:hover {
  background: #E8F0FE;
  color: #0B57D0;
}

.step-inline-body {
  padding: 12px 14px;
  border-top: 1px solid #E3E3E3;
  background: #F8F9FA;
}

/* Formatted Content Styles */
.formatted-prose {
  font-size: 13.5px;
  line-height: 1.7;
  color: #1F1F1F;
}

.formatted-prose p {
  margin: 0 0 8px;
}

.formatted-prose p:last-child {
  margin-bottom: 0;
}

.formatted-prose--lg {
  font-size: 14.5px;
  line-height: 1.8;
}

.formatted-plan {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.formatted-plan-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 7px 10px;
  background: #FFFFFF;
  border: 1px solid #E3E3E3;
  border-radius: 10px;
  font-size: 12.5px;
}

.formatted-plan-row--lg {
  padding: 10px 14px;
  font-size: 13.5px;
}

.plan-pill {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.plan-pill--completed {
  background: #E6F4EA;
  color: #146C2E;
}

.plan-pill--in_progress {
  background: #E8F0FE;
  color: #0B57D0;
}

.plan-pill--pending {
  background: #F0F4F9;
  color: #747775;
}

.plan-summary-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.plan-summary-text {
  font-size: 12px;
  font-weight: 600;
  color: #444746;
  flex-shrink: 0;
}

.fs-plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.fs-plan-percent {
  padding: 2px 10px;
  border-radius: 999px;
  background: #E8F0FE;
  color: #0B57D0;
  font-size: 12px;
  font-weight: 600;
}

.plan-progress-track {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: #E3E3E3;
  overflow: hidden;
}

.plan-progress-track--lg {
  height: 6px;
}

.plan-progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #0B57D0, #146C2E);
  transition: width 0.25s ease;
}

.plan-step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #F0F4F9;
  color: #444746;
  font-size: 11.5px;
  font-weight: 600;
  flex-shrink: 0;
}

.plan-text {
  color: #1F1F1F;
  font-weight: 500;
}

.plan-deps {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 999px;
  background: #F8F9FA;
  border: 1px solid #E3E3E3;
  font-size: 11px;
  color: #747775;
}

.file-op-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}

.file-op-meta--lg {
  margin-bottom: 12px;
  font-size: 13px;
}

.meta-label {
  color: #747775;
  font-weight: 500;
}

.meta-path {
  padding: 2px 8px;
  border-radius: 8px;
  background: #E8F0FE;
  color: #0B57D0;
  font-size: 12px;
  font-weight: 600;
}

.meta-lines {
  margin-left: auto;
  color: #747775;
  font-size: 11.5px;
}

.formatted-code-box {
  margin: 0;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #E3E3E3;
  background: #FFFFFF;
  color: #1F1F1F;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.65;
  max-height: 260px;
  overflow: auto;
}

.formatted-code-box--fs {
  max-height: none;
  font-size: 13.5px;
  padding: 16px 20px;
}

.diff-block-label {
  font-size: 11.5px;
  font-weight: 600;
  margin-bottom: 4px;
}

.diff-block--old .diff-block-label {
  color: #B3261E;
}

.diff-block--new .diff-block-label {
  color: #146C2E;
}

.param-kv-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-kv-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 6px 10px;
  background: #FFFFFF;
  border: 1px solid #E3E3E3;
  border-radius: 10px;
  font-size: 12.5px;
}

.param-k {
  color: #747775;
  font-weight: 600;
  flex-shrink: 0;
}

.param-v {
  color: #1F1F1F;
  word-break: break-all;
}

.tool-result-note {
  margin-top: 8px;
  padding: 7px 12px;
  border-radius: 12px;
  background: #E6F4EA;
  color: #146C2E;
  font-size: 12.5px;
  font-weight: 500;
}

/* Nag 机制 · 计划更新提醒卡片 */
.nag-reminder-card {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #FEF7E0;
  border: 1px solid #FDE293;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nag-reminder-card--lg {
  margin-top: 14px;
  padding: 16px 18px;
  border-radius: 16px;
  gap: 10px;
}

.nag-reminder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.nag-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: #FEEFC3;
  color: #B06000;
  font-size: 11.5px;
  font-weight: 600;
}

.nag-progress-pill {
  padding: 2px 9px;
  border-radius: 999px;
  background: #FFFFFF;
  color: #444746;
  font-size: 11px;
  font-weight: 600;
}

.nag-headline {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: #5F370E;
}

.nag-task-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nag-task-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 10px;
  background: #FFFFFF;
  border: 1px solid #FDE293;
  font-size: 12.5px;
}

.tool-result-box {
  border: 1px solid #E3E3E3;
  border-radius: 12px;
  background: #FFFFFF;
  overflow: hidden;
}

.result-box-label {
  padding: 6px 12px;
  background: #F0F4F9;
  border-bottom: 1px solid #E3E3E3;
  font-size: 11.5px;
  font-weight: 600;
  color: #444746;
}

.result-box-pre {
  margin: 0;
  padding: 10px 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #1F1F1F;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 240px;
  overflow: auto;
}
</style>

<style>
/* Fullscreen Process Inspector (Teleported to body) */
.process-fs-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: rgba(31, 31, 31, 0.36);
  backdrop-filter: blur(4px);
}

.process-fs-sheet {
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

.process-fs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 20px;
  background: #F0F4F9;
  border-bottom: 1px solid #E3E3E3;
  flex-shrink: 0;
}

.process-fs-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.process-fs-main-name {
  font-size: 14.5px;
  font-weight: 600;
  color: #1F1F1F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-fs-time {
  font-size: 12px;
  color: #747775;
  flex-shrink: 0;
}

.process-fs-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.fs-pill-btn {
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

.fs-pill-btn:hover {
  border-color: #A8C7FA;
  background: #E8F0FE;
  color: #0B57D0;
}

.fs-pill-btn--primary {
  border-color: transparent;
  background: #0B57D0;
  color: #FFFFFF;
}

.fs-pill-btn--primary:hover {
  border-color: transparent;
  background: #0842A0;
  color: #FFFFFF;
}

.process-fs-main {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.process-fs-sidebar {
  width: 280px;
  flex: 0 0 280px;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border-right: 1px solid #E3E3E3;
  overflow: hidden;
}

.pfs-sidebar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
}

.pfs-sidebar-heading {
  font-size: 12.5px;
  font-weight: 600;
  color: #444746;
}

.pfs-sidebar-count {
  padding: 1px 8px;
  border-radius: 999px;
  background: #F0F4F9;
  color: #444746;
  font-size: 11px;
  font-weight: 600;
}

.pfs-search-wrap {
  padding: 0 12px 10px;
}

.pfs-search-input {
  width: 100%;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  background: #F8F9FA;
  color: #1F1F1F;
  font-size: 12.5px;
  outline: none;
}

.pfs-search-input:focus {
  border-color: #0B57D0;
  background: #FFFFFF;
}

.pfs-step-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pfs-step-item {
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

.pfs-step-item:hover {
  background: #F0F4F9;
}

.pfs-step-item--active {
  background: #E8F0FE;
  color: #0B57D0;
}

.pfs-step-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.pfs-step-title {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pfs-step-item--active .pfs-step-title {
  font-weight: 600;
}

.pfs-step-sub {
  font-size: 11px;
  color: #747775;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pfs-empty {
  padding: 20px 12px;
  text-align: center;
  font-size: 12.5px;
  color: #747775;
}

.process-fs-body {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
  background: #F8F9FA;
}

.fs-section-card {
  background: #FFFFFF;
  border: 1px solid #E3E3E3;
  border-radius: 18px;
  padding: 18px 20px;
}

.fs-section-heading {
  margin: 0 0 14px;
  font-size: 14.5px;
  font-weight: 600;
  color: #1F1F1F;
}

@media (max-width: 768px) {
  .process-fs-sidebar {
    width: 190px;
    flex: 0 0 190px;
  }
}
</style>
