<template>
  <a-modal v-model:open="visible" :footer="null" width="520px" :closable="true" centered>
    <div class="deploy-success-modal">
      <div class="success-icon-wrap">
        <CheckCircleFilled class="success-icon" />
      </div>

      <div class="success-header">
        <span class="success-kicker">DEPLOYMENT SUCCESSFUL</span>
        <h3 class="success-title">应用已成功部署上线！</h3>
        <p class="success-desc">你的网站已发布到高可用公网环境，支持全球极速访问。</p>
      </div>

      <div class="url-copy-box">
        <span class="url-label">访问地址 (Public URL)</span>
        <div class="url-input-wrap">
          <a-input :value="deployUrl" readonly class="url-input">
            <template #suffix>
              <a-tooltip title="复制链接">
                <a-button type="text" size="small" class="copy-btn" @click="handleCopyUrl">
                  <CopyOutlined />
                </a-button>
              </a-tooltip>
            </template>
          </a-input>
        </div>
      </div>

      <div class="success-modal-actions">
        <a-button type="primary" size="large" class="primary-btn" @click="handleOpenSite">
          <template #icon><ExportOutlined /></template>
          立即在新窗口打开
        </a-button>
        <a-button size="large" @click="handleClose">完成</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleFilled, CopyOutlined, ExportOutlined } from '@ant-design/icons-vue'

interface Props {
  open: boolean
  deployUrl: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(props.deployUrl)
    message.success('部署链接已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动选取复制')
  }
}

const handleOpenSite = () => {
  emit('open-site')
  visible.value = false
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.deploy-success-modal {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 16px 8px 8px;
}

.success-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.1);
  margin-bottom: 16px;
}

.success-icon {
  font-size: 38px;
  color: #10B981;
}

.success-header {
  margin-bottom: 24px;
}

.success-kicker {
  display: block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #10B981;
  margin-bottom: 4px;
}

.success-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #0F172A;
}

.success-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748B;
}

.url-copy-box {
  width: 100%;
  padding: 14px 16px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  text-align: left;
  margin-bottom: 24px;
}

.url-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #64748B;
  margin-bottom: 6px;
  text-transform: uppercase;
}

.url-input {
  border-radius: 8px !important;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  color: #4F46E5 !important;
  font-weight: 500;
}

.copy-btn {
  color: #6366F1;
}

.copy-btn:hover {
  color: #4F46E5;
}

.success-modal-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.primary-btn {
  flex: 1;
}
</style>
