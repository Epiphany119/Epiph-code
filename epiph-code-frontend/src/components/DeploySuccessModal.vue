<template>
  <a-modal v-model:open="visible" :footer="null" width="520px" :closable="true" centered>
    <div class="deploy-success-modal">
      <div class="success-icon-wrap">
        <CheckCircleFilled class="success-icon" />
      </div>

      <div class="success-header">
        <span class="success-kicker">DEPLOYMENT READY</span>
        <h3 class="success-title">应用已成功部署上线</h3>
        <p class="success-desc">你的网站已发布至稳定访问地址，现在即可打开或分享给他人。</p>
      </div>

      <div class="url-copy-box">
        <span class="url-label">访问地址</span>
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
          在新窗口打开
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
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #E6F4EA;
  margin-bottom: 16px;
}

.success-icon {
  font-size: 34px;
  color: #146C2E;
}

.success-header {
  margin-bottom: 22px;
}

.success-kicker {
  display: block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #146C2E;
  margin-bottom: 4px;
}

.success-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1F1F1F;
}

.success-desc {
  margin: 6px 0 0;
  font-size: 13.5px;
  color: #444746;
}

.url-copy-box {
  width: 100%;
  text-align: left;
  padding: 14px 16px;
  background: #F0F4F9;
  border-radius: 16px;
  margin-bottom: 22px;
}

.url-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #444746;
  margin-bottom: 8px;
}

.url-input {
  border-radius: 999px !important;
  color: #0B57D0 !important;
  font-weight: 500;
}

.copy-btn {
  color: #0B57D0;
}

.copy-btn:hover {
  color: #0842A0;
}

.success-modal-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
}

.primary-btn {
  min-width: 160px;
}
</style>
