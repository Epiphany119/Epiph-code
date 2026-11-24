<template>
  <article class="kaggle-card" :class="{ 'kaggle-card--featured': featured }" @click="handleViewChat">
    <!-- Top Media / Live Preview -->
    <div class="card-media">
      <div class="media-topbar" @click.stop>
        <div class="media-dots">
          <span class="m-dot"></span>
          <span class="m-dot"></span>
          <span class="m-dot"></span>
        </div>
        <span class="media-domain">epiph.studio/{{ app.deployKey || app.id || 'app' }}</span>
        <span v-if="app.deployKey" class="media-pill media-pill--live">
          <span class="live-dot"></span>LIVE
        </span>
        <span v-else-if="hasLivePreview" class="media-pill media-pill--ready">READY</span>
        <span v-else class="media-pill media-pill--draft">DRAFT</span>
      </div>

      <div class="media-stage">
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
          <div class="empty-geo-icon">
            <CodeOutlined />
          </div>
          <span class="empty-title">
            {{ previewStatus === 'checking' ? '正在加载实时画布...' : (app.appName || 'AI 应用工程') }}
          </span>
        </div>

        <!-- Hover Action Overlay -->
        <div class="card-overlay" @click.stop>
          <button type="button" class="overlay-btn overlay-btn--dark" @click="handleViewChat">
            <span>打开工作台</span>
            <ArrowRightOutlined />
          </button>
          <button v-if="app.deployKey" type="button" class="overlay-btn overlay-btn--light" @click="handleViewWork">
            <EyeOutlined />
            <span>访问站点</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Card Content Body (Kaggle Editorial Hierarchy) -->
    <div class="card-content">
      <div class="card-title-row">
        <h3 class="card-title" :title="app.appName || '未命名应用'">
          {{ app.appName || '未命名应用' }}
        </h3>
        <span v-if="codeTypeBadge" class="arch-chip" :class="codeTypeBadge.className">
          {{ codeTypeBadge.label }}
        </span>
      </div>

      <p class="card-summary" :title="app.initPrompt || ''">
        {{ app.initPrompt || '通过 Epiph-code AI 工作台生成的交互式应用工程。' }}
      </p>
    </div>

    <!-- Card Hairline Footer -->
    <div class="card-footer">
      <div class="footer-author">
        <a-avatar class="author-avatar" :src="app.user?.userAvatar" :size="24">
          {{ authorName.charAt(0).toUpperCase() }}
        </a-avatar>
        <span class="author-name">{{ authorName }}</span>
        <span class="footer-dot">·</span>
        <span v-if="app.deployKey" class="ver-text ver-text--live">线上 V{{ app.deployedVersionNo || 1 }}</span>
        <span v-else-if="app.workingVersionNo" class="ver-text">V{{ app.workingVersionNo }}</span>
        <span v-else class="ver-text">草稿</span>
      </div>

      <div v-if="!featured" class="footer-actions" @click.stop>
        <a-tooltip :title="isFeaturedApp ? '已在精选画廊中，点击取消精选' : '推荐到精选画廊'">
          <a-button
            type="text"
            size="small"
            class="icon-action-btn"
            :class="{ 'is-starred': isFeaturedApp }"
            @click.stop="handleToggleFeatured"
          >
            <template #icon>
              <StarFilled v-if="isFeaturedApp" />
              <StarOutlined v-else />
            </template>
          </a-button>
        </a-tooltip>
        <a-popconfirm
          title="删除后数据将在数据库保留 30 天，到期后将彻底清理，确认删除？"
          ok-text="确认删除"
          cancel-text="取消"
          ok-type="danger"
          @confirm="handleDelete"
        >
          <a-button type="text" size="small" danger class="icon-action-btn" title="删除应用" @click.stop>
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
  if (!previewUrl.value) return ''
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

const handleViewChat = () => emit('view-chat', props.app.id)
const handleViewWork = () => emit('view-work', props.app)
const handleDelete = () => emit('delete-app', props.app.id)
const handleToggleFeatured = () => emit('toggle-featured', props.app)

const isFeaturedApp = computed(() => (props.app.priority ?? 0) >= 99)

const codeTypeBadge = computed(() => {
  const type = props.app.codeGenType
  if (type === CodeGenTypeEnum.VUE_PROJECT) return { label: 'Vue 3', className: 'arch-chip--vue' }
  if (type === CodeGenTypeEnum.MULTI_FILE) return { label: '多文件', className: 'arch-chip--multi' }
  if (type === CodeGenTypeEnum.HTML) return { label: 'HTML', className: 'arch-chip--html' }
  return null
})
</script>

<style scoped>
.kaggle-card {
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.kaggle-card:hover {
  border-color: #202124;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(32, 33, 36, 0.08);
}

.card-media {
  display: flex;
  flex-direction: column;
  background: #F8F9FA;
  border-bottom: 1px solid #E8EAED;
}

.media-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: 32px;
  padding: 0 12px;
  background: #F8F9FA;
  border-bottom: 1px solid #E8EAED;
}

.media-dots {
  display: flex;
  align-items: center;
  gap: 4px;
}

.m-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #DADCE0;
}

.media-domain {
  flex: 1;
  font-size: 11px;
  color: #80868B;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.media-pill--live {
  background: #E6F4EA;
  color: #137333;
}

.media-pill--ready {
  background: #E6F8FF;
  color: #008ABC;
}

.media-pill--draft {
  background: #F1F3F4;
  color: #5F6368;
}

.live-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #1E8E3E;
}

.media-stage {
  position: relative;
  height: 186px;
  overflow: hidden;
  background: #FFFFFF;
}

.preview-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
}

.preview-iframe-viewport {
  width: 100%;
  height: 100%;
  overflow: hidden;
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 10px;
  background: #F8F9FA;
}

.empty-geo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  color: #202124;
  font-size: 17px;
}

.empty-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #5F6368;
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(32, 33, 36, 0.56);
  backdrop-filter: blur(2px);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.16s ease;
}

.kaggle-card:hover .card-overlay {
  opacity: 1;
  pointer-events: auto;
}

.overlay-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: transform 0.15s ease;
}

.overlay-btn:hover {
  transform: translateY(-1px);
}

.overlay-btn--dark {
  background: #202124;
  color: #FFFFFF;
}

.overlay-btn--light {
  background: #FFFFFF;
  color: #202124;
}

.card-content {
  flex: 1;
  padding: 16px 16px 12px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-title {
  margin: 0;
  font-size: 15.5px;
  font-weight: 700;
  color: #202124;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arch-chip {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.arch-chip--vue {
  background: #E6F4EA;
  color: #137333;
}

.arch-chip--multi {
  background: #E6F8FF;
  color: #008ABC;
}

.arch-chip--html {
  background: #FEF7E0;
  color: #B06000;
}

.card-summary {
  margin: 7px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #5F6368;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid #E8EAED;
  background: #FFFFFF;
}

.footer-author {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  font-size: 12px;
  color: #5F6368;
}

.author-avatar {
  background: #202124;
  color: #FFFFFF;
  font-weight: 700;
  font-size: 11px;
  flex-shrink: 0;
}

.author-name {
  font-weight: 600;
  color: #3C4043;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 90px;
}

.footer-dot {
  color: #BDC1C6;
}

.ver-text {
  font-weight: 600;
  color: #5F6368;
}

.ver-text--live {
  color: #137333;
}

.footer-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.icon-action-btn {
  color: #80868B;
}

.icon-action-btn:hover {
  color: #202124;
  background: #F1F3F4 !important;
}

.icon-action-btn.is-starred {
  color: #F9AB00;
}
</style>
