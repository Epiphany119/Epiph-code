<template>
  <article class="app-card" :class="{ 'app-card--featured': featured }">
    <!-- Browser Mockup Window -->
    <div class="card-preview">
      <!-- Browser Topbar -->
      <div class="browser-topbar">
        <div class="window-controls">
          <span class="control-dot dot--close"></span>
          <span class="control-dot dot--min"></span>
          <span class="control-dot dot--max"></span>
        </div>
        <div class="browser-address-bar">
          <span class="address-text">{{ app.appName || 'app-preview' }}</span>
        </div>
        <div class="browser-status">
          <span v-if="app.deployKey" class="status-pill status-pill--live">
            <span class="pulse-beacon"></span>
            LIVE
          </span>
          <span v-else-if="hasLivePreview" class="status-pill status-pill--preview">
            PREVIEW
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

        <iframe
          v-else-if="hasLivePreview"
          class="preview-iframe"
          :src="iframeSrc"
          :title="`${app.appName || '应用'}实时预览`"
          loading="lazy"
          sandbox="allow-scripts allow-forms allow-popups"
          tabindex="-1"
          aria-hidden="true"
        ></iframe>

        <div v-else class="preview-empty">
          <div class="empty-code-icon">
            <CodeOutlined />
          </div>
          <span class="empty-title">
            {{ previewStatus === 'checking' ? '正在连接页面...' : '等待构建首个版本' }}
          </span>
          <p class="empty-desc">{{ app.initPrompt || '生成完成后，这里会展示真实的运行页面' }}</p>
        </div>

        <!-- Hover Overlay -->
        <div class="card-overlay">
          <button type="button" class="action-btn action-btn--primary" @click="handleViewChat">
            <span>进入工作台</span>
            <ArrowRightOutlined />
          </button>
          <button v-if="app.deployKey" type="button" class="action-btn action-btn--secondary" @click="handleViewWork">
            <EyeOutlined />
            <span>访问站点</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Card Info Footer -->
    <div class="card-body">
      <a-avatar class="card-avatar" :src="app.user?.userAvatar" :size="36">
        {{ authorName.charAt(0).toUpperCase() }}
      </a-avatar>
      <div class="card-meta-wrap">
        <h3 class="card-title" :title="app.appName || '未命名应用'">
          {{ app.appName || '未命名应用' }}
        </h3>
        <div class="card-tags">
          <span class="card-author">{{ authorName }}</span>
          <span class="card-dot-sep">·</span>
          <span v-if="app.deployKey" class="version-tag version-tag--live">
            已部署 V{{ app.deployedVersionNo || 1 }}
          </span>
          <span v-else-if="app.workingVersionNo" class="version-tag version-tag--working">
            工作 V{{ app.workingVersionNo }}
          </span>
          <span v-else class="version-tag version-tag--draft">
            草稿
          </span>
        </div>
      </div>
      <a-popconfirm
        v-if="!featured"
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
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getStaticPreviewUrl, getStaticVersionPreviewUrl } from '@/config/env'
import { CodeOutlined, ArrowRightOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons-vue'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
  (e: 'delete-app', appId: string | number | undefined): void
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

const handleDelete = () => {
  emit('delete-app', props.app.id)
}
</script>

<style scoped>
.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-card:hover {
  transform: translateY(-3px);
  border-color: #A8C7FA;
  box-shadow: 0 2px 8px rgba(60, 64, 67, 0.08);
}

.card-preview {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #F8F9FA;
  border-bottom: 1px solid #E3E3E3;
}

.browser-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 34px;
  padding: 0 14px;
  background: #F8F9FA;
  border-bottom: 1px solid #E3E3E3;
}

.window-controls {
  display: flex;
  align-items: center;
  gap: 5px;
}

.control-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.dot--close {
  background: #FDA4AF;
}

.dot--min {
  background: #FDE68A;
}

.dot--max {
  background: #86EFAC;
}

.browser-address-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 160px;
  height: 20px;
  padding: 0 8px;
  background: #FFFFFF;
  border-radius: 4px;
  border: 1px solid #E2E8F0;
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
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  padding: 1px 6px;
  border-radius: 4px;
}

.status-pill--live {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}

.status-pill--preview {
  background: #E8F0FE;
  color: #0B57D0;
}

.pulse-beacon {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #10B981;
}

.preview-screen {
  position: relative;
  height: 190px;
  overflow: hidden;
  background: #FFFFFF;
}

.preview-cover,
.preview-iframe {
  width: 100%;
  height: 100%;
  border: 0;
  object-fit: cover;
  pointer-events: none;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  background: radial-gradient(circle at center, #F8FAFC 0%, #EEF2F6 100%);
}

.empty-code-icon {
  font-size: 24px;
  color: #94A3B8;
  margin-bottom: 8px;
}

.empty-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.empty-desc {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94A3B8;
  max-width: 220px;
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
  background: rgba(15, 23, 42, 0.65);
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
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.18s ease;
}

.action-btn--primary {
  background: #0B57D0;
  color: #FFFFFF;
}

.action-btn--primary:hover {
  background: #0842A0;
  transform: translateY(-1px);
}

.action-btn--secondary {
  background: rgba(255, 255, 255, 0.96);
  color: #1E293B;
}

.action-btn--secondary:hover {
  background: #FFFFFF;
  transform: translateY(-1px);
}

.card-body {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
}

.card-avatar {
  background: #0B57D0;
  color: #FFFFFF;
  font-weight: 600;
  flex-shrink: 0;
}

.card-meta-wrap {
  min-width: 0;
  flex: 1;
}

.card-title {
  margin: 0;
  font-size: 14px;
  font-weight: 650;
  color: #1F1F1F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
}

.card-author {
  color: #64748B;
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
  padding: 1px 6px;
  border-radius: 4px;
}

.version-tag--live {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.version-tag--working {
  background: rgba(99, 102, 241, 0.1);
  color: #0B57D0;
}

.version-tag--draft {
  background: #F1F5F9;
  color: #94A3B8;
}
</style>
