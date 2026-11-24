<template>
  <article class="app-card" :class="{ 'app-card--featured': featured }" @click="handleViewChat">
    <!-- Browser Mockup Window -->
    <div class="card-preview">
      <!-- Browser Topbar -->
      <div class="browser-topbar" @click.stop>
        <div class="window-controls">
          <span class="control-dot dot--close"></span>
          <span class="control-dot dot--min"></span>
          <span class="control-dot dot--max"></span>
        </div>
        <div class="browser-address-bar" :title="previewUrl || app.appName || 'studio.epiph.ai'">
          <span class="address-lock">🔒</span>
          <span class="address-text">studio.epiph.ai/{{ app.deployKey || app.id || 'preview' }}</span>
        </div>
        <div class="browser-status">
          <span v-if="app.deployKey" class="status-pill status-pill--live">
            <span class="pulse-beacon"></span>
            LIVE
          </span>
          <span v-else-if="hasLivePreview" class="status-pill status-pill--preview">
            READY
          </span>
          <span v-else class="status-pill status-pill--draft">
            DRAFT
          </span>
        </div>
      </div>

      <!-- Preview Screen -->
      <div class="preview-screen">
        <img
          v-if="!hasLivePreview && app.cover"
          class="preview-cover"
          :src="app.cover"
          :alt="`${app.appName || '应用'}封面`"
          loading="lazy"
        />

        <div v-else-if="hasLivePreview" class="preview-iframe-viewport">
          <iframe
            class="preview-iframe"
            :src="iframeSrc"
            :title="`${app.appName || '应用'}实时预览`"
            loading="lazy"
            sandbox="allow-scripts allow-forms allow-popups"
            tabindex="-1"
            aria-hidden="true"
          ></iframe>
        </div>

        <div v-else class="preview-empty">
          <div class="blueprint-grid" aria-hidden="true"></div>
          <div class="empty-code-icon">
            <CodeOutlined />
          </div>
          <span class="empty-title">
            {{ previewStatus === 'checking' ? '正在同步预览画布...' : (app.appName || 'AI 应用工程') }}
          </span>
          <p class="empty-desc">{{ app.initPrompt || '点击进入工作台，实时查看与迭代生成页面' }}</p>
        </div>

        <!-- Hover Overlay -->
        <div class="card-overlay" @click.stop>
          <button type="button" class="action-btn action-btn--primary" @click="handleViewChat">
            <span>进入工作台</span>
            <ArrowRightOutlined />
          </button>
          <button v-if="app.deployKey" type="button" class="action-btn action-btn--secondary" @click="handleViewWork">
            <EyeOutlined />
            <span>访问线上站点</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Card Info Footer -->
    <div class="card-body">
      <a-avatar class="card-avatar" :src="app.user?.userAvatar" :size="38">
        {{ authorName.charAt(0).toUpperCase() }}
      </a-avatar>
      <div class="card-meta-wrap">
        <div class="card-title-row">
          <h3 class="card-title" :title="app.appName || '未命名应用'">
            {{ app.appName || '未命名应用' }}
          </h3>
        </div>
        <p v-if="app.initPrompt" class="card-prompt-excerpt" :title="app.initPrompt">
          {{ app.initPrompt }}
        </p>
        <div class="card-tags">
          <span class="card-author">{{ authorName }}</span>
          <span class="card-dot-sep">·</span>
          <span v-if="codeTypeBadge" class="type-badge" :class="codeTypeBadge.className">
            {{ codeTypeBadge.label }}
          </span>
          <span v-if="app.deployKey" class="version-tag version-tag--live">
            线上 V{{ app.deployedVersionNo || 1 }}
          </span>
          <span v-else-if="app.workingVersionNo" class="version-tag version-tag--working">
            工作 V{{ app.workingVersionNo }}
          </span>
          <span v-else class="version-tag version-tag--draft">
            草稿
          </span>
        </div>
      </div>
      <div v-if="!featured" class="card-footer-actions" @click.stop>
        <a-tooltip :title="isFeaturedApp ? '已在精选画廊中，点击取消精选' : '推荐到精选应用画廊'">
          <a-button
            type="text"
            size="small"
            class="card-star-btn"
            :class="{ 'card-star-btn--active': isFeaturedApp }"
            @click.stop="handleToggleFeatured"
          >
            <template #icon>
              <StarFilled v-if="isFeaturedApp" />
              <StarOutlined v-else />
            </template>
          </a-button>
        </a-tooltip>
        <a-popconfirm
          title="删除后数据将在数据库保留 30 天，到期后将彻底清理数据库记录及本地产物，确认删除？"
          ok-text="确认删除"
          cancel-text="取消"
          ok-type="danger"
          @confirm="handleDelete"
        >
          <a-button
            type="text"
            size="small"
            danger
            class="card-delete-btn"
            title="删除应用"
            @click.stop
          >
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </a-popconfirm>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getStaticPreviewUrl, getStaticVersionPreviewUrl } from '@/config/env'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import {
  CodeOutlined,
  ArrowRightOutlined,
  EyeOutlined,
  DeleteOutlined,
  StarOutlined,
  StarFilled,
} from '@ant-design/icons-vue'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
  (e: 'delete-app', appId: string | number | undefined): void
  (e: 'toggle-featured', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()
const previewStatus = ref<'checking' | 'ready' | 'missing'>('checking')
const previewRevision = ref(Date.now())
let checkId = 0

const authorName = computed(() => {
  const name = props.app.user?.userName?.trim()
  if (name && name !== '无名') return name
  return props.app.user?.userAccount || (props.featured ? '社区创作者' : '我的作品')
})

const previewUrl = computed(() => {
  if (props.app.deployedVersionId) {
    return getStaticVersionPreviewUrl(props.app.deployedVersionId)
  }
  if (props.app.workingVersionId) {
    return getStaticVersionPreviewUrl(props.app.workingVersionId)
  }
  if (!props.app.id || !props.app.codeGenType) {
    return ''
  }
  return getStaticPreviewUrl(props.app.codeGenType, String(props.app.id))
})

const iframeSrc = computed(() => {
  if (!previewUrl.value) {
    return ''
  }
  return `${previewUrl.value}?v=${previewRevision.value}`
})

const hasLivePreview = computed(() => previewStatus.value === 'ready' && !!previewUrl.value)

watch(
  previewUrl,
  async (url) => {
    const currentCheck = ++checkId
    previewRevision.value = Date.now()
    previewStatus.value = 'checking'
    if (!url) {
      previewStatus.value = 'missing'
      return
    }

    try {
      const response = await fetch(url, { method: 'HEAD', cache: 'no-store' })
      if (currentCheck === checkId) {
        previewStatus.value = response.ok ? 'ready' : 'missing'
      }
    } catch {
      if (currentCheck === checkId) {
        previewStatus.value = 'ready'
      }
    }
  },
  { immediate: true },
)

watch(
  () => [props.app.updateTime, props.app.deployKey],
  () => {
    previewRevision.value = Date.now()
  },
)

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}

const isFeaturedApp = computed(() => (props.app.priority ?? 0) >= 99)

const codeTypeBadge = computed(() => {
  const type = props.app.codeGenType
  if (type === CodeGenTypeEnum.VUE_PROJECT) {
    return { label: 'Vue 工程', className: 'type-badge--vue' }
  }
  if (type === CodeGenTypeEnum.MULTI_FILE) {
    return { label: '多文件', className: 'type-badge--multi' }
  }
  if (type === CodeGenTypeEnum.HTML) {
    return { label: 'HTML', className: 'type-badge--html' }
  }
  return null
})

const handleDelete = () => {
  emit('delete-app', props.app.id)
}

const handleToggleFeatured = () => {
  emit('toggle-featured', props.app)
}
</script>

<style scoped>
.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all 0.24s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-card:hover {
  transform: translateY(-4px);
  border-color: rgba(37, 99, 235, 0.35);
  box-shadow: 0 14px 32px -8px rgba(15, 23, 42, 0.1), 0 4px 12px -2px rgba(37, 99, 235, 0.08);
}

.card-preview {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
  border-bottom: 1px solid #EEF2F6;
}

.browser-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  height: 34px;
  padding: 0 12px;
  background: #F1F5F9;
  border-bottom: 1px solid #E2E8F0;
}

.window-controls {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}

.control-dot {
  width: 8.5px;
  height: 8.5px;
  border-radius: 50%;
}

.dot--close {
  background: #FB7185;
}

.dot--min {
  background: #FBBF24;
}

.dot--max {
  background: #34D399;
}

.browser-address-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex: 1;
  max-width: 210px;
  height: 21px;
  padding: 0 9px;
  background: #FFFFFF;
  border-radius: 6px;
  border: 1px solid rgba(15, 23, 42, 0.07);
}

.address-lock {
  font-size: 9px;
  opacity: 0.7;
}

.address-text {
  font-size: 11px;
  color: #64748B;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-status {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  padding: 1px 7px;
  border-radius: 999px;
}

.status-pill--live {
  background: #DCFCE7;
  color: #059669;
}

.status-pill--preview {
  background: #DBEAFE;
  color: #2563EB;
}

.status-pill--draft {
  background: #E2E8F0;
  color: #64748B;
}

.pulse-beacon {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #10B981;
}

.preview-screen {
  position: relative;
  height: 204px;
  overflow: hidden;
  background: #FFFFFF;
}

.preview-cover {
  width: 100%;
  height: 100%;
  border: 0;
  object-fit: cover;
  pointer-events: none;
  transition: transform 0.35s ease;
}

.app-card:hover .preview-cover {
  transform: scale(1.03);
}

.preview-iframe-viewport {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative;
  background: #FFFFFF;
}

.preview-iframe {
  width: 200%;
  height: 200%;
  border: 0;
  transform: scale(0.5);
  transform-origin: top left;
  pointer-events: none;
}

.preview-empty {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%);
}

.blueprint-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(37, 99, 235, 0.05) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(37, 99, 235, 0.05) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}

.empty-code-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #FFFFFF;
  border: 1px solid rgba(37, 99, 235, 0.16);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  font-size: 18px;
  color: #2563EB;
  margin-bottom: 10px;
}

.empty-title {
  position: relative;
  font-size: 13px;
  font-weight: 700;
  color: #1E293B;
}

.empty-desc {
  position: relative;
  margin: 5px 0 0;
  font-size: 11.5px;
  line-height: 1.5;
  color: #64748B;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(15, 23, 42, 0.62);
  backdrop-filter: blur(4px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

.app-card:hover .card-overlay {
  opacity: 1;
  pointer-events: auto;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.18s ease;
}

.action-btn--primary {
  background: linear-gradient(180deg, #3B82F6 0%, #2563EB 100%);
  color: #FFFFFF;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}

.action-btn--primary:hover {
  background: #1D4ED8;
  transform: translateY(-1px);
}

.action-btn--secondary {
  background: rgba(255, 255, 255, 0.96);
  color: #0F172A;
}

.action-btn--secondary:hover {
  background: #FFFFFF;
  transform: translateY(-1px);
}

.card-body {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px 16px;
}

.card-avatar {
  background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
  color: #FFFFFF;
  font-weight: 600;
  flex-shrink: 0;
  margin-top: 2px;
}

.card-meta-wrap {
  min-width: 0;
  flex: 1;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-prompt-excerpt {
  margin: 3px 0 6px;
  font-size: 12px;
  color: #64748B;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
}

.card-author {
  color: #64748B;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 90px;
}

.card-dot-sep {
  color: #CBD5E1;
}

.version-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 6px;
}

.version-tag--live {
  background: #ECFDF5;
  color: #059669;
}

.version-tag--working {
  background: #EFF6FF;
  color: #2563EB;
}

.version-tag--draft {
  background: #F1F5F9;
  color: #64748B;
}

.type-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 6px;
  flex-shrink: 0;
}

.type-badge--vue {
  background: #ECFDF5;
  color: #047857;
}

.type-badge--multi {
  background: #EFF6FF;
  color: #1D4ED8;
}

.type-badge--html {
  background: #FFFBEB;
  color: #B45309;
}

.card-footer-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.card-star-btn {
  color: #94A3B8;
}

.card-star-btn:hover {
  color: #F59E0B;
  background: #FEF3C7 !important;
}

.card-star-btn--active {
  color: #F59E0B;
}
</style>
