<template>
  <div id="appEditPage">
    <!-- Header -->
    <div class="edit-page-header">
      <div class="header-breadcrumb">
        <a-button type="link" class="back-link" @click="goToChat">
          <ArrowLeftOutlined /> 返回工作台对话
        </a-button>
      </div>
      <div class="header-title-row">
        <div>
          <h1 class="page-title">应用设置与信息</h1>
          <p class="page-subtitle">修改应用展示信息、封面图与精选权重配置。</p>
        </div>
      </div>
    </div>

    <div class="edit-grid">
      <!-- Edit Form Card -->
      <a-card title="基本配置" :loading="loading" class="edit-card">
        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item label="应用名称" name="appName">
            <a-input
              v-model:value="formData.appName"
              placeholder="请输入应用名称"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="应用封面链接"
            name="cover"
            extra="支持图片公网链接，推荐尺寸 600x400"
          >
            <a-input v-model:value="formData.cover" placeholder="请输入封面图片地址" />
            <div v-if="formData.cover" class="cover-preview-box">
              <a-image
                :src="formData.cover"
                :width="180"
                :height="120"
                class="cover-img"
              />
            </div>
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="精选优先级"
            name="priority"
            extra="数值范围 0-99，设为 99 时自动展示在首页精选画廊"
          >
            <a-input-number
              v-model:value="formData.priority"
              :min="0"
              :max="99"
              style="width: 160px"
            />
          </a-form-item>

          <a-form-item label="初始需求提示词" name="initPrompt">
            <a-textarea
              v-model:value="formData.initPrompt"
              placeholder="初始提示词"
              :rows="3"
              disabled
            />
            <div class="field-hint">初始提示词由系统生成后固化，不可二次修改</div>
          </a-form-item>

          <a-form-item label="生成工程架构" name="codeGenType">
            <a-input
              :value="formatCodeGenType(formData.codeGenType)"
              disabled
            />
          </a-form-item>

          <a-form-item v-if="formData.deployKey" label="部署密钥 (Deploy Key)" name="deployKey">
            <a-input v-model:value="formData.deployKey" disabled />
          </a-form-item>

          <div class="form-actions-wrap">
            <a-button type="primary" html-type="submit" :loading="submitting">
              保存更改
            </a-button>
            <a-button @click="resetForm">重置</a-button>
            <a-button type="link" @click="goToChat">进入工作台</a-button>
          </div>
        </a-form>
      </a-card>

      <!-- App Metadata Card -->
      <a-card title="运行元数据" class="metadata-card">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="应用 ID">
            <code>#{{ appInfo?.id }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="创建者">
            <UserInfo :user="appInfo?.user" size="small" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatTime(appInfo?.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="最近更新">
            {{ formatTime(appInfo?.updateTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="部署状态">
            <span v-if="appInfo?.deployedTime" class="text-success">
              已部署于 {{ formatTime(appInfo.deployedTime) }}
            </span>
            <span v-else class="text-muted">未部署</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="appInfo?.deployKey" label="公网访问">
            <a-button type="link" size="small" class="p-0" @click="openPreview">
              <ExportOutlined /> 打开线上站点
            </a-button>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'
import { getDeployUrl } from '@/config/env'
import type { FormInstance } from 'ant-design-vue'
import { ArrowLeftOutlined, ExportOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appInfo = ref<API.AppVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 1, max: 50, message: '应用名称长度在 1-50 个字符', trigger: 'blur' },
  ],
  cover: [{ type: 'url', message: '请输入有效的 URL 地址', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: '优先级范围 0-99', trigger: 'blur' }],
}

const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用 ID 不存在')
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error('您没有权限编辑此应用')
        router.push('/')
        return
      }

      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
      formData.initPrompt = appInfo.value.initPrompt || ''
      formData.codeGenType = appInfo.value.codeGenType || ''
      formData.deployKey = appInfo.value.deployKey || ''
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch {
    message.error('获取应用信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!appInfo.value?.id) return

  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      res = await updateAppByAdmin({
        id: appInfo.value.id,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      res = await updateApp({
        id: appInfo.value.id,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('配置修改成功')
      await fetchAppInfo()
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch {
    message.error('修改失败，请重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  if (appInfo.value) {
    formData.appName = appInfo.value.appName || ''
    formData.cover = appInfo.value.cover || ''
    formData.priority = appInfo.value.priority || 0
  }
  formRef.value?.clearValidate()
}

const goToChat = () => {
  if (appInfo.value?.id) {
    router.push(`/app/chat/${appInfo.value.id}`)
  }
}

const openPreview = () => {
  if (appInfo.value?.deployKey) {
    window.open(getDeployUrl(appInfo.value.deployKey), '_blank')
  }
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
#appEditPage {
  max-width: 1040px;
  margin: 0 auto;
}

.edit-page-header {
  margin-bottom: 24px;
}

.back-link {
  padding: 0;
  color: #1A73E8;
  font-weight: 500;
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #0F172A;
  letter-spacing: -0.02em;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #64748B;
}

.edit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(300px, 0.7fr);
  gap: 24px;
}

.edit-card,
.metadata-card {
  border-radius: 14px;
}

.cover-preview-box {
  margin-top: 10px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #E2E8F0;
  display: inline-block;
}

.field-hint {
  font-size: 12px;
  color: #94A3B8;
  margin-top: 4px;
}

.form-actions-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #F1F5F9;
}

.text-success {
  color: #10B981;
  font-weight: 500;
}

.text-muted {
  color: #94A3B8;
}

.p-0 {
  padding: 0;
}

@media (max-width: 768px) {
  .edit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
