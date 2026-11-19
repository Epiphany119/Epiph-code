<template>
  <a-modal v-model:open="visible" title="应用详情" :footer="null" width="520px">
    <div class="app-detail-content">
      <!-- App Basic Info Grid -->
      <div class="app-info-grid">
        <div class="info-cell">
          <span class="info-label">应用名称</span>
          <span class="info-val font-bold">{{ app?.appName || '未命名应用' }}</span>
        </div>
        <div class="info-cell">
          <span class="info-label">创建者</span>
          <UserInfo :user="app?.user" size="small" />
        </div>
        <div class="info-cell">
          <span class="info-label">创建时间</span>
          <span class="info-val">{{ formatTime(app?.createTime) }}</span>
        </div>
        <div class="info-cell">
          <span class="info-label">生成类型</span>
          <a-tag v-if="app?.codeGenType" color="indigo" class="type-tag">
            {{ formatCodeGenType(app.codeGenType) }}
          </a-tag>
          <span v-else class="info-val text-dim">未知类型</span>
        </div>
      </div>

      <!-- Initial Prompt Display -->
      <div v-if="app?.initPrompt" class="prompt-preview-box">
        <span class="prompt-box-label">初始提示词需求:</span>
        <p class="prompt-box-text">{{ app.initPrompt }}</p>
      </div>

      <!-- Actions -->
      <div v-if="showActions" class="modal-actions-bar">
        <a-button type="primary" @click="handleEdit">
          <template #icon>
            <EditOutlined />
          </template>
          编辑信息
        </a-button>
        <a-popconfirm
          title="确定要彻底删除该应用吗？此操作无法撤销。"
          @confirm="handleDelete"
          ok-text="确认删除"
          cancel-text="取消"
          ok-type="danger"
        >
          <a-button danger>
            <template #icon>
              <DeleteOutlined />
            </template>
            删除应用
          </a-button>
        </a-popconfirm>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import { formatTime } from '@/utils/time'
import { formatCodeGenType } from '../utils/codeGenTypes.ts'

interface Props {
  open: boolean
  app?: API.AppVO
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'edit'): void
  (e: 'delete'): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: false,
})

const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.app-detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 12px 0 4px;
}

.app-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px;
  background: #F8FAFC;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
}

.info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #64748B;
}

.info-val {
  font-size: 14px;
  color: #0F172A;
}

.info-val.font-bold {
  font-weight: 600;
}

.text-dim {
  color: #94A3B8;
}

.type-tag {
  align-self: flex-start;
  background: rgba(99, 102, 241, 0.1);
  color: #4F46E5;
  border-radius: 4px;
  font-weight: 600;
}

.prompt-preview-box {
  padding: 14px 16px;
  background: #F8FAFC;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
}

.prompt-box-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.prompt-box-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #334155;
  max-height: 120px;
  overflow-y: auto;
}

.modal-actions-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid #F1F5F9;
}
</style>
