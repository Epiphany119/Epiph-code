<template>
  <div class="markdown-content" v-html="renderedMarkdown"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

interface Props {
  content: string
}

const props = defineProps<Props>()

const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: false,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch {
        // Fallback
      }
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

const renderedMarkdown = computed(() => {
  const raw = props.content || ''
  if (!raw.trim()) {
    return ''
  }
  if (raw.includes('```')) {
    return md.render(raw)
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
      return md.render(`\`\`\`json\n${JSON.stringify(parsed, null, 2)}\n\`\`\``)
    } catch {
      return md.render(`\`\`\`json\n${normalized}\n\`\`\``)
    }
  }
  return md.render(raw)
})
</script>

<style scoped>
.markdown-content {
  color: #1E293B;
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
  color: #0F172A;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.015em;
}

.markdown-content :deep(h1) {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #E2E8F0;
}

.markdown-content :deep(h2) {
  font-size: 1.3em;
}

.markdown-content :deep(h3) {
  font-size: 1.15em;
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

.markdown-content :deep(pre.hljs) {
  margin: 0.8em 0;
  padding: 14px 18px;
  border-radius: 16px;
  background: #F8F9FA;
  color: #1E293B;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.65;
  overflow-x: auto;
  border: 1px solid #E3E3E3;
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
  color: #475569;
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
  color: #1E293B;
}

.markdown-content :deep(a) {
  color: #0B57D0;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.markdown-content :deep(hr) {
  margin: 1.5em 0;
  border: 0;
  border-top: 1px solid #E2E8F0;
}
</style>
