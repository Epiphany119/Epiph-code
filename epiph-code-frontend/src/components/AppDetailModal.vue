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
          <a-tag v-if="app?.codeGenType" class="type-tag">
            {{ formatCodeGenType(app.codeGenType) }}
          </a-tag>
          <span v-else class="info-val text-dim">未知类型</span>
        </div>
      </div>

      <!-- Initial Prompt Display -->
      <div v-if="app?.initPrompt" class="prompt-preview-box">
        <span class="prompt-box-label">初始提示词需求</span>
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
          title="删除后数据将在数据库保留 30 天，到期后将彻底清理数据库记录及本地产物，确认删除？"
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
  gap: 18px;
  padding: 12px 0 4px;
}

.app-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px 18px;
  background: #F8F9FA;
  border-radius: 16px;
  border: 1px solid #E3E3E3;
}

.info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #747775;
}

.info-val {
  font-size: 14px;
  color: #1F1F1F;
}

.info-val.font-bold {
  font-weight: 600;
}

.text-dim {
  color: #747775;
}

.type-tag {
  align-self: flex-start;
  background: #E8F0FE;
  color: #0B57D0;
  border-radius: 999px;
  padding: 1px 10px;
  font-weight: 500;
}

.prompt-preview-box {
  padding: 14px 18px;
  background: #F0F4F9;
  border-radius: 16px;
}

.prompt-box-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #444746;
  margin-bottom: 6px;
}

.prompt-box-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.65;
  color: #1F1F1F;
  max-height: 140px;
  overflow-y: auto;
}

.modal-actions-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid #E3E3E3;
}
</style>
