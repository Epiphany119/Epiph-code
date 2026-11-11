<template>
  <article class="app-card" :class="{ 'app-card--featured': featured }">
    <div class="app-preview" :class="{ 'app-preview--live': hasLivePreview }">
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
        <div class="preview-empty-icon"><span>&lt;/&gt;</span></div>
        <span class="preview-empty-title">
          {{ previewStatus === 'checking' ? '正在查找页面' : '等待首个版本' }}
        </span>
        <p>{{ app.initPrompt || '生成完成后，这里会显示应用的真实页面。' }}</p>
      </div>

      <div class="app-overlay">
        <a-space>
          <a-button type="primary" @click="handleViewChat">打开工作台</a-button>
          <a-button v-if="app.deployKey" class="published-button" @click="handleViewWork">
            查看线上版本
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="app-info">
      <a-avatar class="app-avatar" :src="app.user?.userAvatar" :size="38">
        {{ app.user?.userName?.charAt(0) || 'U' }}
      </a-avatar>
      <div class="app-info-main">
        <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
        <div class="app-meta">
          <span class="app-author">{{ app.user?.userName || (featured ? '官方团队' : '我的作品') }}</span>
          <span class="meta-divider"></span>
          <span class="app-state" :class="{ 'app-state--published': app.deployKey }">
            <span class="app-state-dot"></span>
            {{ app.deployKey ? '已部署' : hasLivePreview ? '可预览' : '进行中' }}
          </span>
        </div>
      </div>
      <span class="card-arrow" aria-hidden="true">↗</span>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getStaticPreviewUrl } from '@/config/env'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()
const previewStatus = ref<'checking' | 'ready' | 'missing'>('checking')
const previewRevision = ref(Date.now())
let checkId = 0

const previewUrl = computed(() => {
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
      // 跨域部署环境可能不允许 HEAD；此时仍尝试直接加载 iframe。
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
</script>

<style scoped>
.app-card {
  overflow: hidden;
  min-width: 0;
  border: 1px solid #e8e9ef;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(33, 38, 56, 0.045);
  transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
}

.app-card:hover {
  transform: translateY(-4px);
  border-color: #d7c4b7;
  box-shadow: 0 18px 40px rgba(33, 38, 56, 0.12);
}

.app-card--featured {
  border-color: #e4d9d1;
}

.app-preview {
  position: relative;
  height: 205px;
  overflow: hidden;
  background:
    radial-gradient(circle at 80% 15%, rgba(244, 205, 174, 0.8), transparent 34%),
    linear-gradient(135deg, #f2f1f8 0%, #f8f3ef 100%);
}

.app-preview--live {
  background: #f6f7fa;
}

.preview-cover,
.preview-iframe {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
}

.preview-cover {
  object-fit: cover;
}

.preview-iframe {
  background: #fff;
  pointer-events: none;
}

.preview-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 42px 28px 20px;
  text-align: center;
}

.preview-empty-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.85);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 8px 20px rgba(53, 58, 82, 0.08);
  color: #9d6a50;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 15px;
  font-weight: 700;
}

.preview-empty-title {
  margin-top: 10px;
  color: #4c4f5c;
  font-size: 12px;
  font-weight: 650;
}

.preview-empty p {
  width: min(100%, 330px);
  margin: 5px 0 0;
  overflow: hidden;
  color: #858a97;
  font-size: 11px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-state-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #b7bbc5;
}

.app-state--published .app-state-dot {
  background: #65ad78;
  box-shadow: 0 0 0 3px rgba(101, 173, 120, 0.13);
}

.app-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(28, 32, 46, 0.28);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-card:hover .app-overlay,
.app-card:focus-within .app-overlay {
  opacity: 1;
}

.published-button {
  border-color: rgba(255, 255, 255, 0.8);
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.published-button:hover {
  border-color: #fff !important;
  color: #fff !important;
  background: rgba(255, 255, 255, 0.22) !important;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 15px 16px;
}

.app-avatar {
  flex: 0 0 auto;
  background: #eee9e5;
  color: #89715f;
}

.app-info-main {
  min-width: 0;
  flex: 1;
}

.app-title {
  margin: 0;
  overflow: hidden;
  color: #292c37;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 5px;
  color: #8c919f;
  font-size: 11px;
}

.app-author {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-divider {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #c6c8d0;
}

.app-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.app-state--published {
  color: #548663;
}

.card-arrow {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid #ececf1;
  border-radius: 10px;
  color: #838896;
  font-size: 14px;
  transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.app-card:hover .card-arrow {
  border-color: #e9d7ca;
  background: #fbf5f0;
  color: #a56543;
}

@media (max-width: 520px) {
  .app-preview {
    height: 190px;
  }

  .app-overlay {
    opacity: 1;
    inset: auto 10px 10px auto;
    width: auto;
    height: auto;
    background: transparent;
  }

  .app-overlay :deep(.ant-space) {
    gap: 6px !important;
  }

  .app-overlay :deep(.ant-btn) {
    min-height: 30px;
    padding: 0 10px;
    border: 0;
    border-radius: 9px;
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 3px 12px rgba(32, 36, 49, 0.12);
    color: #4c4f5c;
    font-size: 11px;
  }
}
</style>
