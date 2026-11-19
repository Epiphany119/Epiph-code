<template>
  <div id="appChatPage">
    <!-- Top Workspace Navigation Bar -->
    <header class="chat-header-bar">
      <div class="header-left">
        <RouterLink to="/" class="back-home-btn" title="返回应用列表">
          <ArrowLeftOutlined />
        </RouterLink>
        <div class="header-app-info">
          <h1 class="header-app-title">{{ appInfo?.appName || '智能应用生成器' }}</h1>
          <div class="header-status-tags">
            <a-tag v-if="appInfo?.codeGenType" class="tag-codegen">
              {{ formatCodeGenType(appInfo.codeGenType) }}
            </a-tag>
            <a-tag v-if="deployedVersion" class="tag-deployed">
              <CheckCircleFilled /> 线上 V{{ deployedVersion.versionNo }}
            </a-tag>
            <a-tag v-if="isWorkflowLocked" class="tag-locked">
              <LockOutlined /> 工作流锁定
            </a-tag>
          </div>
        </div>
      </div>

      <div class="header-right">
        <!-- Versions Drawer Trigger -->
        <a-button v-if="isOwner || isAdmin" class="btn-tool" @click="versionsDrawerOpen = true">
          <template #icon><HistoryOutlined /></template>
          <span>网页版本</span>
          <span v-if="workingVersion" class="badge-version">工作 V{{ workingVersion.versionNo }}</span>
        </a-button>

        <!-- App Details Modal Trigger -->
        <a-button type="default" class="btn-tool" @click="showAppDetail">
          <template #icon><InfoCircleOutlined /></template>
          <span>应用详情</span>
        </a-button>

        <!-- Download Code -->
        <a-button
          type="default"
          class="btn-tool"
          :loading="downloading"
          :disabled="!isOwner"
          @click="downloadCode"
        >
          <template #icon><DownloadOutlined /></template>
          <span>下载源码</span>
        </a-button>

        <!-- Deploy App -->
        <a-button
          type="primary"
          class="btn-deploy"
          :loading="deploying"
          :disabled="!(isOwner || isAdmin) || !workingVersion"
          @click="deployApp"
        >
          <template #icon><CloudUploadOutlined /></template>
          <span>部署发布</span>
        </a-button>
      </div>
    </header>

    <!-- Main Dual-Pane Workspace -->
    <div class="chat-workspace">
      <!-- Left Pane: AI Conversation & Execution Timeline -->
      <section class="chat-pane">
        <!-- Message Scroll Area -->
        <div ref="messagesContainer" class="messages-scroll-area" @scroll="handleMessagesScroll">
          <!-- Load More History -->
          <div v-if="hasMoreHistory" class="history-loader-wrap">
            <a-button type="link" size="small" :loading="loadingHistory" @click="loadMoreHistory">
              加载更早的历史记录
            </a-button>
          </div>

          <!-- Message Items -->
          <div v-for="(message, index) in messages" :key="index" class="message-row">
            <!-- User Message -->
            <div v-if="message.type === 'user'" class="user-bubble-wrap">
              <div class="user-bubble">
                <div class="bubble-text">{{ message.content }}</div>
              </div>
              <a-avatar :src="loginUserStore.loginUser.userAvatar" class="avatar-user">
                {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
              </a-avatar>
            </div>

            <!-- AI Message -->
            <div v-else class="ai-bubble-wrap">
              <div class="ai-avatar-wrap">
                <img :src="aiAvatar" alt="AI Agent" class="avatar-ai-img" />
              </div>

              <div class="ai-bubble-card">
                <!-- Agent Task Plan Card (Shown on last AI turn or when plan exists) -->
                <div v-if="currentPlan.length > 0 && index === messages.length - 1" class="agent-plan-card">
                  <div class="plan-card-header">
                    <div class="plan-title-box">
                      <BranchesOutlined class="plan-head-icon" />
                      <span class="plan-card-title">生成任务推进计划</span>
                    </div>
                    <span class="plan-counter-pill">
                      {{ currentPlan.filter(i => i.status === 'completed').length }} / {{ currentPlan.length }}
                    </span>
                  </div>

                  <div class="plan-items-list">
                    <div
                      v-for="item in currentPlan"
                      :key="item.id"
                      class="plan-task-row"
                      :class="{
                        'task--done': item.status === 'completed',
                        'task--running': item.status === 'in_progress',
                        'task--pending': item.status !== 'completed' && item.status !== 'in_progress',
                      }"
                      :style="{ paddingLeft: (getPlanItemDepth(item, currentPlan) * 16 + 12) + 'px' }"
                    >
                      <span class="task-status-icon">
                        <CheckCircleFilled v-if="item.status === 'completed'" class="icon-completed" />
                        <LoadingOutlined v-else-if="item.status === 'in_progress'" class="icon-running" />
                        <ClockCircleOutlined v-else class="icon-pending" />
                      </span>
                      <span class="task-text">{{ item.text }}</span>
                    </div>
                  </div>

                  <div class="plan-progress-track">
                    <div
                      class="plan-progress-bar"
                      :style="{ width: ((currentPlan.filter(i => i.status === 'completed').length / currentPlan.length) * 100) + '%' }"
                    ></div>
                  </div>
                </div>

                <!-- Process Events (Thinking & Tools Execution) -->
                <div v-if="message.processEvents?.length" class="process-events-container">
                  <div
                    v-for="eventItem in message.processEvents"
                    :key="eventItem.id"
                    class="process-item-box"
                  >
                    <details v-if="eventItem.details" class="event-details-tag">
                      <summary class="event-summary-row">
                        <div class="summary-left">
                          <BulbOutlined v-if="eventItem.type === 'thinking'" class="event-icon icon--thinking" />
                          <CodeOutlined v-else class="event-icon icon--tool" />
                          <span class="event-type-label">{{ eventItem.type === 'thinking' ? '思考过程' : '工具调用' }}</span>
                          <span class="event-summary-text">{{ eventItem.summary || '执行详情' }}</span>
                        </div>
                        <span class="event-time-stamp">{{ eventItem.time }}</span>
                      </summary>
                      <div class="event-details-pre-wrap">
                        <pre class="event-pre-code"><code>{{ eventItem.details }}</code></pre>
                      </div>
                    </details>

                    <div v-else class="event-summary-row event-summary-row--static">
                      <div class="summary-left">
                        <BulbOutlined v-if="eventItem.type === 'thinking'" class="event-icon icon--thinking" />
                        <CodeOutlined v-else class="event-icon icon--tool" />
                        <span class="event-type-label">{{ eventItem.type === 'thinking' ? '思考过程' : '系统事件' }}</span>
                        <span class="event-summary-text">{{ eventItem.summary || '执行中' }}</span>
                      </div>
                      <span class="event-time-stamp">{{ eventItem.time }}</span>
                    </div>
                  </div>
                </div>

                <!-- Final Reply Section -->
                <div v-if="message.content" class="final-reply-box">
                  <div class="final-reply-header" @click="toggleFinalReply(index)">
                    <div class="final-reply-left">
                      <CheckCircleOutlined class="final-reply-check" />
                      <span class="final-reply-heading">生成结果与说明</span>
                    </div>
                    <span class="final-reply-caret">
                      <DownOutlined v-if="isReplyExpanded(index)" />
                      <RightOutlined v-else />
                    </span>
                  </div>

                  <div v-show="isReplyExpanded(index)" class="final-reply-content">
                    <MarkdownRenderer :content="message.content" />
                  </div>
                </div>

                <!-- Live Stream Loading -->
                <div v-if="message.loading" class="stream-loading-row">
                  <a-spin size="small" />
                  <span class="stream-loading-text">AI 正在构思与执行代码生成...</span>
                </div>

                <!-- Turn Version Generated Tag -->
                <div v-if="message.turnId && versionForTurn(message.turnId)" class="turn-version-card">
                  <span class="turn-version-icon">🚀</span>
                  <span class="turn-version-label">本次生成保存为不可变版本:</span>
                  <a-tag color="purple" class="turn-tag-link" @click="previewTurnVersion(message.turnId)">
                    V{{ versionForTurn(message.turnId)?.versionNo }} · 立即预览
                  </a-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Input Dock Area -->
        <div class="input-dock-container">
          <div class="input-dock-card">
            <!-- Top Controls Bar inside Dock -->
            <div class="dock-meta-bar">
              <!-- Agent Model Dropdown -->
              <div class="agent-switch-pill">
                <span class="pill-prefix">模型:</span>
                <a-select
                  v-model:value="selectedAgent"
                  size="small"
                  :bordered="false"
                  class="compact-agent-select"
                  :disabled="isGenerating || !isOwner"
                >
                  <a-select-option value="default">默认 Agent</a-select-option>
                  <a-select-option value="deepseek">DeepSeek Flash</a-select-option>
                </a-select>
              </div>

              <!-- Token Metrics -->
              <div v-if="isOwner || isAdmin" class="metrics-display-pill">
                <span title="最近一轮代码生成消耗 Token">
                  最近消耗: <strong>{{ formatMetricTokens(chatMetrics?.lastTurnTokenUsage?.totalTokens) }}</strong>
                  <span v-if="lastTurnAgentLabel"> ({{ lastTurnAgentLabel }})</span>
                </span>
                <span class="pill-sep">·</span>
                <span :title="contextMetricCaption">
                  上下文: <strong>{{ chatMetrics ? `~${formatMetricTokens(chatMetrics.contextTokensEstimate)}` : '计算中' }}</strong>
                </span>
              </div>

              <!-- Base Version if user modified from historic version -->
              <div v-if="isOwner && modificationBaseVersion" class="base-version-indicator">
                <span>基于 <strong>V{{ modificationBaseVersion.versionNo }}</strong> 修改</span>
                <button type="button" class="btn-change-base" @click="versionsDrawerOpen = true">更换基线</button>
              </div>
            </div>

            <!-- Selected Element Inspector Banner -->
            <div v-if="selectedElementInfo" class="inspector-selection-banner">
              <div class="inspector-info-left">
                <AimOutlined class="inspector-icon" />
                <span class="inspector-tag-name">&lt;{{ selectedElementInfo.tagName.toLowerCase() }}&gt;</span>
                <code v-if="selectedElementInfo.selector" class="inspector-selector-code">{{ selectedElementInfo.selector }}</code>
              </div>
              <button type="button" class="inspector-cancel-btn" @click="clearSelectedElement">取消元素绑定</button>
            </div>

            <!-- Input Textarea & Send Button -->
            <div class="textarea-container">
              <a-tooltip v-if="!isOwner" title="只能在自己的应用项目下发送对话指令哦" placement="top">
                <a-textarea
                  v-model:value="userInput"
                  :placeholder="getInputPlaceholder()"
                  :rows="3"
                  :maxlength="1000"
                  class="prompt-textarea"
                  :disabled="isGenerating || !isOwner"
                  @keydown.enter.exact.prevent="sendMessage"
                />
              </a-tooltip>
              <a-textarea
                v-else
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="3"
                :maxlength="1000"
                class="prompt-textarea"
                :disabled="isGenerating"
                @keydown.enter.exact.prevent="sendMessage"
              />

              <div class="textarea-footer">
                <span class="textarea-hint">Enter 发送 · Shift + Enter 换行</span>
                <a-button
                  type="primary"
                  shape="circle"
                  class="send-circle-btn"
                  :loading="isGenerating"
                  :disabled="!isOwner || !userInput.trim()"
                  @click="sendMessage"
                >
                  <template #icon><SendOutlined /></template>
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Right Pane: Real-Time Browser Live Preview -->
      <section class="preview-pane">
        <!-- Browser Mockup Window Header -->
        <div class="preview-browser-header">
          <div class="browser-header-left">
            <div class="traffic-lights">
              <span class="light light--red"></span>
              <span class="light light--yellow"></span>
              <span class="light light--green"></span>
            </div>

            <!-- Responsive Device Viewport Switcher -->
            <div class="device-switcher-tabs">
              <button
                type="button"
                class="device-tab"
                :class="{ 'device-tab--active': previewDevice === 'desktop' }"
                title="桌面端全屏预览 (100%)"
                @click="previewDevice = 'desktop'"
              >
                <DesktopOutlined />
                <span class="device-tab-label">桌面</span>
              </button>
              <button
                type="button"
                class="device-tab"
                :class="{ 'device-tab--active': previewDevice === 'tablet' }"
                title="平板端预览 (768px)"
                @click="previewDevice = 'tablet'"
              >
                <TabletOutlined />
                <span class="device-tab-label">平板</span>
              </button>
              <button
                type="button"
                class="device-tab"
                :class="{ 'device-tab--active': previewDevice === 'mobile' }"
                title="手机端预览 (375px)"
                @click="previewDevice = 'mobile'"
              >
                <MobileOutlined />
                <span class="device-tab-label">手机</span>
              </button>
            </div>

            <!-- Simulated Address Bar -->
            <div class="simulated-url-bar" :title="previewUrl">
              <span class="url-scheme">https://</span>
              <span class="url-host">preview.epiph.studio</span>
              <span class="url-route">{{ previewVersion ? `/v${previewVersion.versionNo}` : '/live' }}</span>
            </div>
          </div>

          <div class="browser-header-right">
            <!-- Visual Element Inspector Toggle -->
            <a-button
              v-if="isOwner && previewUrl"
              size="small"
              :type="isEditMode ? 'primary' : 'default'"
              :danger="isEditMode"
              class="inspector-toggle-btn"
              :class="{ 'inspector-toggle-btn--active': isEditMode }"
              @click="toggleEditMode"
            >
              <template #icon><AimOutlined /></template>
              <span>{{ isEditMode ? '退出点选修改' : '点选元素修改' }}</span>
            </a-button>

            <!-- Reload Preview -->
            <a-tooltip title="重新加载预览页面">
              <button type="button" class="preview-tool-btn" @click="refreshPreview">
                <ReloadOutlined />
              </button>
            </a-tooltip>

            <!-- Open In New Tab -->
            <a-tooltip title="在新窗口中独立访问">
              <button v-if="previewUrl" type="button" class="preview-tool-btn" @click="openInNewTab">
                <ExportOutlined />
              </button>
            </a-tooltip>
          </div>
        </div>

        <!-- Browser Screen Area -->
        <div class="preview-screen-viewport">
          <div
            v-if="previewUrl"
            class="iframe-canvas-wrap"
            :class="`device-mode--${previewDevice}`"
          >
            <iframe
              :key="previewUrl"
              :src="previewUrl"
              class="preview-iframe"
              title="生成应用实时预览"
              frameborder="0"
              @load="onIframeLoad"
              @error="onIframeError"
            ></iframe>
            <div v-if="previewLoadError" class="preview-error-overlay">
              <ReloadOutlined class="error-center-icon" />
              <h4>预览页面暂时无法加载</h4>
              <p>构建服务正在准备中或页面渲染受到安全限制，请重试。</p>
              <a-button type="primary" ghost @click="refreshPreview">重新加载</a-button>
            </div>
          </div>

          <div v-else-if="isGenerating || previewStatus === 'checking'" class="preview-placeholder-state">
            <a-spin size="large" />
            <h4 class="state-title">{{ isGenerating ? 'AI 正在全力构建并编译工程...' : '正在检索可预览网页版本...' }}</h4>
            <p class="state-desc">完成生成后，此处将自动呈现具有完整交互能力的真实网页</p>
          </div>

          <div v-else class="preview-placeholder-state">
            <div class="placeholder-icon-circle">
              <CodeOutlined />
            </div>
            <h4 class="state-title">当前暂无可预览页面</h4>
            <p class="state-desc">{{ messages.length ? '等待模型输出首个可执行版本' : '在左侧发送你的应用构想，代码将实时编译渲染' }}</p>
            <a-button size="small" @click="refreshPreview">刷新检查</a-button>
          </div>
        </div>
      </section>
    </div>

    <!-- Modals & Drawers -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />

    <!-- Versions Drawer -->
    <a-drawer v-model:open="versionsDrawerOpen" title="网页版本历史" placement="right" width="480">
      <div class="drawer-header-meta">
        <span>每次生成成功均保存为独立不可变快照，支持无缝切换与回退。</span>
        <a-button size="small" :loading="loadingVersions" @click="refreshVersions">刷新列表</a-button>
      </div>

      <a-alert v-if="modificationBaseVersion" type="info" show-icon class="current-base-banner">
        <template #message>当前修改基线：V{{ modificationBaseVersion.versionNo }}</template>
      </a-alert>

      <a-empty v-if="!versions.length && !loadingVersions" description="暂无历史版本" />
      <a-spin v-else-if="loadingVersions" />
      <div v-else class="version-cards-list">
        <div v-for="version in versions" :key="version.id" class="version-entry-card">
          <div class="version-card-top">
            <span class="version-number-tag">V{{ version.versionNo }}</span>
            <div class="version-status-pills">
              <a-tag v-if="version.working" color="blue">当前工作版本</a-tag>
              <a-tag v-if="version.deployed" color="green">已上线版本</a-tag>
              <a-tag v-if="String(previewVersionId) === String(version.id)" color="purple">正在预览</a-tag>
            </div>
          </div>

          <div class="version-meta-time">
            {{ formatVersionTime(version.createTime) }} · {{ formatVersionType(version.versionType) }}
          </div>

          <div v-if="version.userPrompt" class="version-prompt-text">
            {{ version.userPrompt }}
          </div>

          <div class="version-specs">
            <span v-if="version.parentVersionId">基线: V{{ versionNoById(version.parentVersionId) }}</span>
            <span>大小: {{ formatBytes(version.totalBytes) }}</span>
            <span v-if="version.agent">模型: {{ version.agent === 'deepseek' ? 'DeepSeek Flash' : '默认 Agent' }}</span>
          </div>

          <div class="version-actions-bar">
            <a-button size="small" @click="selectPreviewVersion(version)">预览此版</a-button>
            <a-button size="small" :disabled="isGenerating" @click="setModificationBase(version)">设为修改基线</a-button>
            <a-button size="small" :disabled="!isOwner" @click="openVersionCompare(version)">版本对比</a-button>
            <a-button
              size="small"
              type="primary"
              ghost
              :loading="deploying && deployingVersionId === version.id"
              :disabled="!(isOwner || isAdmin)"
              @click="deployVersion(version)"
            >
              部署此版
            </a-button>
          </div>
        </div>
      </div>

      <a-divider v-if="deploymentHistory.length" orientation="left">部署发布记录</a-divider>
      <div v-if="deploymentHistory.length" class="deploy-history-list">
        <div v-for="record in deploymentHistory" :key="record.id" class="deploy-history-row">
          <a-tag color="green">发布 V{{ record.versionNo }}</a-tag>
          <span class="deploy-time">{{ formatVersionTime(record.deployedTime) }}</span>
        </div>
      </div>
    </a-drawer>

    <!-- Version Diff Compare Modal -->
    <a-modal v-model:open="compareModalOpen" title="版本文件差异对比" width="840px" :footer="null">
      <div class="compare-controls-bar">
        <a-select v-model:value="compareLeftVersionId" style="min-width: 150px" placeholder="旧版本">
          <a-select-option v-for="version in versions" :key="version.id" :value="version.id">V{{ version.versionNo }}</a-select-option>
        </a-select>
        <span class="compare-vs-badge">VS</span>
        <a-select v-model:value="compareRightVersionId" style="min-width: 150px" placeholder="新版本">
          <a-select-option v-for="version in versions" :key="version.id" :value="version.id">V{{ version.versionNo }}</a-select-option>
        </a-select>
        <a-button type="primary" :loading="loadingCompare" @click="loadVersionCompare">开始比较</a-button>
      </div>

      <a-empty v-if="compareFiles.length === 0 && !loadingCompare" description="两版本间无文件差异或尚未发起对比" />
      <a-collapse v-else :bordered="false" class="compare-collapse">
        <a-collapse-panel
          v-for="file in compareFiles"
          :key="file.path"
          :header="`${file.path} · ${file.status} · ${file.oldBytes ?? '—'}B → ${file.newBytes ?? '—'}B`"
        >
          <pre class="unified-diff-view">{{ file.unifiedDiff }}</pre>
        </a-collapse-panel>
      </a-collapse>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  listAppVersions,
  listAppDeploymentHistory,
  compareAppVersions,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { listAppChatHistory, listTurnEvents } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import request from '@/request'
import {
  getGlobalChatGenMode,
  isAppWorkflowModeLocked,
  lockAppWorkflowMode,
  resolveAppChatGenMode,
} from '@/utils/chatGenMode'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.svg'
import { getStaticPreviewUrl, getStaticVersionPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  ArrowLeftOutlined,
  HistoryOutlined,
  DesktopOutlined,
  TabletOutlined,
  MobileOutlined,
  ReloadOutlined,
  CheckCircleFilled,
  CheckCircleOutlined,
  ClockCircleOutlined,
  LoadingOutlined,
  DownOutlined,
  RightOutlined,
  AimOutlined,
  BranchesOutlined,
  LockOutlined,
  CodeOutlined,
  BulbOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 响应式设备切换
const previewDevice = ref<'desktop' | 'tablet' | 'mobile'>('desktop')

// 最终回复折叠状态管理（默认全部展开，点击可折叠）
const collapsedReplies = ref<Set<number>>(new Set())
const isReplyExpanded = (index: number) => !collapsedReplies.value.has(index)
const toggleFinalReply = (index: number) => {
  const s = new Set(collapsedReplies.value)
  if (s.has(index)) {
    s.delete(index)
  } else {
    s.add(index)
  }
  collapsedReplies.value = s
}

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string | number>()
const isWorkflowLocked = ref(false)
const chatMetrics = ref<ChatMetrics>()
const versions = ref<API.AppVersionVO[]>([])
const deploymentHistory = ref<API.AppDeploymentHistoryVO[]>([])
const versionsDrawerOpen = ref(false)
const loadingVersions = ref(false)
const previewVersionId = ref<string>()
const baseVersionId = ref<string>()
const deployingVersionId = ref<string>()
const compareModalOpen = ref(false)
const compareLeftVersionId = ref<string>()
const compareRightVersionId = ref<string>()
const compareFiles = ref<API.AppVersionFileChangeVO[]>([])
const loadingCompare = ref(false)

const workingVersion = computed(() => versions.value.find(version => version.working))
const deployedVersion = computed(() => versions.value.find(version => version.deployed))
const previewVersion = computed(() => versions.value.find(version => String(version.id) === String(previewVersionId.value)))
const modificationBaseVersion = computed(() => versions.value.find(version => String(version.id) === String(baseVersionId.value)))

const versionForTurn = (turnId: string) => versions.value.find(version => version.turnId === turnId)
const previewTurnVersion = (turnId?: string) => {
  if (!turnId) return
  const version = versionForTurn(turnId)
  if (version) void selectPreviewVersion(version)
}
const versionNoById = (id?: string) => versions.value.find(version => String(version.id) === String(id))?.versionNo ?? '未知'
const formatVersionTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '时间未知'
const formatVersionType = (value?: string) => value === 'GENERATED' ? '对话生成'
  : value === 'LEGACY_DEPLOYED' ? '历史部署基线' : value === 'LEGACY_BASELINE' ? '历史代码基线' : '网页版本'
const formatBytes = (value?: number) => {
  if (value === undefined || value === null) return '大小未知'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

const formatMetricTokens = (value?: number | null) => {
  if (value === undefined || value === null) return '0'
  return new Intl.NumberFormat('zh-CN').format(value)
}

const lastTurnAgentLabel = computed(() => {
  const agent = chatMetrics.value?.lastTurnTokenUsage?.agent
  if (agent === 'deepseek') return 'DeepSeek Flash'
  if (agent === 'default') return '默认 Agent'
  return ''
})

const contextProgressPercent = computed(() => {
  if (!chatMetrics.value) return 0
  const estimatedTokens = chatMetrics.value.contextTokensEstimate || 0
  const threshold = chatMetrics.value.contextTokenThreshold
  if (threshold && threshold > 0) {
    return Math.min(100, Math.round((estimatedTokens / threshold) * 100))
  }
  const messageLimit = chatMetrics.value.messageWindowLimit || 20
  const messageCount = chatMetrics.value.contextMessageCount || 0
  return Math.min(100, Math.round((messageCount / messageLimit) * 100))
})

const contextMetricCaption = computed(() => {
  const metrics = chatMetrics.value
  if (!metrics) return '正在读取上下文'
  const count = metrics.contextMessageCount || 0
  const limit = metrics.messageWindowLimit || 20
  if (metrics.contextTokenThreshold) {
    return `压缩阈值 ${contextProgressPercent.value}% · ${count}/${limit} 条消息`
  }
  return `消息窗口 ${count}/${limit}`
})

const refreshChatMetrics = async () => {
  if (!appId.value || (!isOwner.value && !isAdmin.value)) return
  try {
    const response = await request.get('/app/chat/metrics', { params: { appId: appId.value } })
    if (response.data?.code === 0 && response.data.data) {
      chatMetrics.value = response.data.data as ChatMetrics
    }
  } catch (error) {
    console.warn('读取对话指标失败：', error)
  }
}

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  turnId?: string
  processEvents?: ProcessEventItem[]
  streamBuffer?: string
}

interface ChatMetrics {
  contextTokensEstimate?: number
  contextMessageCount?: number
  contextTokenThreshold?: number | null
  messageWindowLimit?: number
  lastTurnTokenUsage?: {
    inputTokens?: number
    outputTokens?: number
    totalTokens?: number
    agent?: string
  }
}

interface ThinkingStreamChunk {
  type: 'thinking'
  data: string
}

interface ToolRequestStreamChunk {
  type: 'tool_request'
  id?: string
  name?: string
  arguments?: string
}

interface ToolExecutedStreamChunk {
  type: 'tool_executed'
  id?: string
  name?: string
  arguments?: string
  result?: string
}

type StructuredStreamChunk = ThinkingStreamChunk | ToolRequestStreamChunk | ToolExecutedStreamChunk

type ProcessEventType =
  | 'thinking'
  | 'tool'
  | 'system'
  | 'error'

interface ProcessEventItem {
  id: string
  type: ProcessEventType
  title: string
  summary: string
  details?: string
  time: string
}

const TOOL_EVENT_TYPES = new Set(['tool_request', 'tool_executed', 'tool_result'])
const TOOL_NAME_LABEL_MAP: Record<string, string> = {
  writeFile: '写入文件',
  readFile: '读取文件',
  readDir: '读取目录',
  modifyFile: '修改文件',
  deleteFile: '删除文件',
  exit: '退出工具调用',
  updatePlan: '更新计划',
}

interface PlanItem {
  id: string
  text: string
  status: string
  deps?: string[]
}

const currentPlan = ref<PlanItem[]>([])

const getPlanItemDepth = (item: PlanItem, items: PlanItem[]): number => {
  if (!item.deps || item.deps.length === 0) return 0
  const itemMap = new Map(items.map(i => [i.id, i]))
  let maxDepth = 0
  for (const depId of item.deps) {
    const dep = itemMap.get(depId)
    if (dep) {
      maxDepth = Math.max(maxDepth, getPlanItemDepth(dep, items) + 1)
    }
  }
  return maxDepth
}

const messages = ref<Message[]>([])
const userInput = ref('')
const selectedAgent = ref(
  String(route.query.agent || '') === 'deepseek' ? 'deepseek' : 'default',
)
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const shouldAutoScroll = ref(true)

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)
const previewStatus = ref<'checking' | 'ready' | 'missing'>('checking')
const previewLoadError = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
  onError: (tip: string) => {
    message.warning(tip)
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const isVueProjectMode = computed(() => {
  return appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT
})

// 应用详情相关
const appDetailVisible = ref(false)

const getCurrentGenerationMode = () => {
  if (!appId.value) {
    return getGlobalChatGenMode()
  }
  return resolveAppChatGenMode(appId.value)
}

const parseStructuredStreamChunk = (chunk: unknown): StructuredStreamChunk | null => {
  if (typeof chunk !== 'string') {
    return null
  }
  const trimmed = chunk.trim()
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
    return null
  }
  try {
    const parsed = JSON.parse(
      trimmed
        .replace(/[“”]/g, '"')
        .replace(/[‘’]/g, "'")
    )
    if (
      parsed &&
      parsed.type === 'thinking' &&
      typeof parsed.data === 'string'
    ) {
      return parsed as ThinkingStreamChunk
    }
    if (parsed && parsed.type === 'tool_request') {
      return {
        type: 'tool_request',
        id: parsed.id,
        name: parsed.name,
        arguments: parsed.arguments,
      }
    }
    if (parsed && parsed.type === 'tool_executed') {
      return {
        type: 'tool_executed',
        id: parsed.id,
        name: parsed.name,
        arguments: parsed.arguments,
        result: parsed.result,
      }
    }
    return null
  } catch {
    return null
  }
}

const formatEventTime = () => {
  return new Date().toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

const sanitizeEventLine = (line: string) => {
  return line
    .replace(/^\[(选择工具|工具调用|工具结果|执行结束)\]\s*/, '')
    .replace(/^(工具:|参数:|结果:)\s*/, '')
    .trim()
}

const buildEventPreview = (details: string, type: ProcessEventType) => {
  if (!details) {
    return type === 'error' ? '执行异常' : '过程'
  }
  const lines = details
    .split('\n')
    .map((line) => sanitizeEventLine(line))
    .filter(Boolean)
  if (!lines.length) {
    return type === 'error' ? '执行异常' : '过程'
  }
  const toolLine = lines.find((line) =>
    /(写入文件|读取文件|修改文件|删除文件|创建目录|执行命令|运行命令|search|read|write)/i.test(line)
  )
  const first = toolLine || lines[0]
  return first.length > 56 ? `${first.slice(0, 56)}...` : first
}

const ensureProcessEvents = (msg: Message) => {
  if (!msg.processEvents) {
    msg.processEvents = []
  }
  return msg.processEvents
}

const appendProcessEvent = (
  msg: Message,
  event: Omit<ProcessEventItem, 'id' | 'time'>,
  mergeIfSameType = false
) => {
  const events = ensureProcessEvents(msg)
  const last = events[events.length - 1]
  if (mergeIfSameType && last && last.type === event.type) {
    last.summary = event.summary || last.summary
    if (event.details) {
      last.details = last.details ? `${last.details}\n${event.details}` : event.details
    }
    return
  }
  events.push({
    id: `${Date.now()}_${events.length}_${Math.random().toString(36).slice(2, 8)}`,
    time: formatEventTime(),
    ...event,
  })
}

const upsertThinkingEvent = (msg: Message, thinking: string) => {
  const events = ensureProcessEvents(msg)
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const item = events[i]
    if (item.type === 'thinking') {
      item.details = thinking
      item.summary = buildEventPreview(thinking, 'thinking')
      return
    }
  }
  events.push({
    id: `${Date.now()}_${events.length}_thinking`,
    type: 'thinking',
    title: 'thinking',
    summary: buildEventPreview(thinking, 'thinking'),
    details: thinking,
    time: formatEventTime(),
  })
}

const mergeStreamSnapshotOrDelta = (previous: string, incoming: string) => {
  if (!incoming) {
    return {
      next: previous,
      delta: '',
    }
  }
  if (!previous) {
    return {
      next: incoming,
      delta: incoming,
    }
  }
  if (incoming.startsWith(previous)) {
    return {
      next: incoming,
      delta: incoming.slice(previous.length),
    }
  }
  if (previous === incoming || previous.startsWith(incoming)) {
    return {
      next: previous,
      delta: '',
    }
  }
  if (incoming.startsWith(previous.slice(-Math.min(previous.length, 64)))) {
    return {
      next: previous + incoming,
      delta: incoming,
    }
  }
  return {
    next: previous + incoming,
    delta: incoming,
  }
}

const sanitizeAssistantContent = (raw: string) => {
  if (!raw) return ''
  const normalizedRaw = raw
    .replace(/[“”]/g, '"')
    .replace(/[‘’]/g, "'")
    .replace(/}\s*{/g, '}\n{')
  const lines = normalizedRaw.split('\n')
  const filtered: string[] = []
  let suppressToolResultBlock = false
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try {
        const obj = JSON.parse(trimmed)
        if (obj && typeof obj.type === 'string' && TOOL_EVENT_TYPES.has(obj.type)) {
          continue
        }
      } catch {
        // ignore
      }
    }
    if (/"type"\s*:\s*"(tool_request|tool_executed|tool_result)"/.test(trimmed)) {
      continue
    }
    if (/^\[(选择工具|工具调用|执行结束)\]/.test(trimmed)) {
      suppressToolResultBlock = false
      continue
    }
    if (/^\[工具结果\]/.test(trimmed) || /^结果:\s*/.test(trimmed)) {
      suppressToolResultBlock = true
      continue
    }
    if (/^(工具:|参数:)\s*/.test(trimmed)) continue
    if (/^(替换前|替换后)[:：]?$/.test(trimmed)) continue
    if (suppressToolResultBlock) continue
    filtered.push(line)
  }
  return filtered.join('\n').trim()
}

const appendToolDetail = (msg: Message, toolEventIndex: number, detail: string) => {
  const events = ensureProcessEvents(msg)
  const event = events[toolEventIndex]
  if (!event) return
  event.details = event.details ? `${event.details}\n${detail}` : detail
  event.summary = buildEventPreview(event.details || '', 'tool')
}

const findLatestToolEventIndexByCallId = (msg: Message, toolCallId?: string) => {
  if (!toolCallId) return null
  const events = ensureProcessEvents(msg)
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const item = events[i]
    if (item.type === 'tool' && item.id.includes(`tool_${toolCallId}`)) {
      return i
    }
  }
  return null
}

const appendStructuredToolEvent = (msg: Message, chunk: ToolRequestStreamChunk | ToolExecutedStreamChunk) => {
  if (chunk.name === 'updatePlan') {
    const argStr = chunk.arguments || ''
    if (argStr) {
      try {
        const parsed = JSON.parse(argStr)
        const items: PlanItem[] = parsed.items || parsed
        if (Array.isArray(items)) {
          currentPlan.value = items
        }
      } catch {
        // ignore
      }
    }
    return
  }
  const toolDisplayName = TOOL_NAME_LABEL_MAP[chunk.name || ''] || chunk.name || '工具'
  if (chunk.type === 'tool_request') {
    const events = ensureProcessEvents(msg)
    events.push({
      id: `tool_${chunk.id || `${Date.now()}_${events.length}`}`,
      type: 'tool',
      title: 'tool',
      summary: toolDisplayName,
      details: chunk.arguments ? `参数: ${chunk.arguments}` : toolDisplayName,
      time: formatEventTime(),
    })
    return
  }
  const targetIndex = findLatestToolEventIndexByCallId(msg, chunk.id)
  const detailParts: string[] = []
  detailParts.push(`工具: ${toolDisplayName}`)
  if (chunk.arguments) detailParts.push(`参数: ${chunk.arguments}`)
  if (chunk.result) detailParts.push(`结果: ${chunk.result}`)
  const mergedDetails = detailParts.join('\n')
  if (targetIndex !== null) {
    appendToolDetail(msg, targetIndex, mergedDetails)
    return
  }
  const events = ensureProcessEvents(msg)
  events.push({
    id: `tool_${chunk.id || `${Date.now()}_${events.length}`}`,
    type: 'tool',
    title: 'tool',
    summary: toolDisplayName,
    details: mergedDetails,
    time: formatEventTime(),
  })
}

const appendStructuredStreamChunk = (msg: Message, chunk: StructuredStreamChunk) => {
  if (chunk.type === 'thinking') {
    upsertThinkingEvent(msg, chunk.data)
    return
  }
  if (chunk.type === 'tool_request' || chunk.type === 'tool_executed') {
    appendStructuredToolEvent(msg, chunk)
  }
}

const appendRawProcessChunk = (msg: Message, chunk: string) => {
  const structured = parseStructuredStreamChunk(chunk)
  if (structured) {
    appendStructuredStreamChunk(msg, structured)
    return
  }

  const thinkingMatch = chunk.match(/^\[thinking\]\s*([\s\S]*)$/i)
  if (thinkingMatch) {
    const thinking = thinkingMatch[1].trim()
    if (thinking) {
      upsertThinkingEvent(msg, thinking)
    }
    return
  }

  const toolRequestMatch = chunk.match(/^\[(选择工具|工具调用)\]\s*([\s\S]*)$/)
  if (toolRequestMatch) {
    appendProcessEvent(msg, {
      type: 'tool',
      title: 'tool',
      summary: sanitizeEventLine(toolRequestMatch[2]) || '工具调用',
      details: chunk,
    })
    return
  }

  const toolResultMatch = chunk.match(/^\[(工具结果|执行结束)\]\s*([\s\S]*)$/)
  if (toolResultMatch) {
    appendProcessEvent(msg, {
      type: 'tool',
      title: 'tool',
      summary: sanitizeEventLine(toolResultMatch[2]) || '工具结果',
      details: chunk,
    })
    return
  }

  if (/^(工具|参数|结果):/.test(chunk.trim())) {
    appendProcessEvent(msg, {
      type: 'tool',
      title: 'tool',
      summary: sanitizeEventLine(chunk) || '工具详情',
      details: chunk,
    })
  }
}

const consumeStreamBufferAndAppend = (
  msg: Message,
  rawChunk: string,
  onAssistantChunk: (chunk: string) => void
) => {
  const incoming = (msg.streamBuffer || '') + rawChunk
  const lines = incoming.split('\n')
  msg.streamBuffer = lines.pop() || ''

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue
    const structured = parseStructuredStreamChunk(trimmed)
    if (structured) {
      appendStructuredStreamChunk(msg, structured)
      continue
    }
    if (
      trimmed.startsWith('[thinking]') ||
      trimmed.startsWith('[选择工具]') ||
      trimmed.startsWith('[工具调用]') ||
      trimmed.startsWith('[工具结果]') ||
      trimmed.startsWith('[执行结束]') ||
      /^(工具|参数|结果):/.test(trimmed)
    ) {
      appendRawProcessChunk(msg, trimmed)
      continue
    }
    onAssistantChunk(`${line}\n`)
  }
}

const flushStreamBuffer = (msg: Message, onAssistantChunk: (chunk: string) => void) => {
  if (!msg.streamBuffer) return
  const leftover = msg.streamBuffer
  msg.streamBuffer = ''
  const trimmed = leftover.trim()
  if (!trimmed) return
  const structured = parseStructuredStreamChunk(trimmed)
  if (structured) {
    appendStructuredStreamChunk(msg, structured)
    return
  }
  if (
    trimmed.startsWith('[thinking]') ||
    trimmed.startsWith('[选择工具]') ||
    trimmed.startsWith('[工具调用]') ||
    trimmed.startsWith('[工具结果]') ||
    trimmed.startsWith('[执行结束]') ||
    /^(工具|参数|结果):/.test(trimmed)
  ) {
    appendRawProcessChunk(msg, trimmed)
    return
  }
  onAssistantChunk(leftover)
}

const toMultilineEventContent = (eventItem: API.ChatEventLog) => {
  const lines: string[] = []
  if (eventItem.toolName) lines.push(`工具: ${eventItem.toolName}`)
  if (eventItem.toolArguments) lines.push(`参数: ${eventItem.toolArguments}`)
  if (eventItem.toolResult) lines.push(`结果: ${eventItem.toolResult}`)
  if (eventItem.reasoningContent) lines.push(eventItem.reasoningContent)
  if (eventItem.content) lines.push(eventItem.content)
  return lines.join('\n')
}

const mapTurnEventType = (eventType?: string) => {
  const map: Record<string, { type: ProcessEventType; title: string }> = {
    THINKING_PARTIAL: { type: 'thinking', title: '思考中' },
    THINKING_FINAL: { type: 'thinking', title: '思考中' },
    TOOL_REQUEST: { type: 'tool', title: '调用工具中' },
    TOOL_RESULT: { type: 'tool', title: '工具调用完毕' },
    ERROR: { type: 'error', title: '错误' },
  }
  return map[eventType || ''] || null
}

const hydrateTurnEventsForMessage = async (msg: Message) => {
  if (!msg.turnId || !isVueProjectMode.value) return
  try {
    const res = await listTurnEvents({ turnId: msg.turnId })
    if (res.data?.code !== 0 || !res.data?.data?.length) return
    const sorted = [...res.data.data].sort((a, b) => (a.seq || 0) - (b.seq || 0))
    const events: ProcessEventItem[] = []
    let activeToolIndex: number | null = null

    sorted.forEach((item, idx) => {
      const mapped = mapTurnEventType(item.eventType)
      if (!mapped) return
      const detail = toMultilineEventContent(item)
      const time = item.createTime || formatEventTime()
      if (item.eventType === 'TOOL_REQUEST') {
        events.push({
          id: `${msg.turnId}_${item.id || idx}`,
          type: 'tool',
          title: 'tool',
          summary: buildEventPreview(detail, 'tool'),
          details: detail,
          time,
        })
        activeToolIndex = events.length - 1
        return
      }
      if (item.eventType === 'TOOL_RESULT') {
        if (activeToolIndex !== null && events[activeToolIndex]) {
          events[activeToolIndex].title = 'tool'
          if (detail) {
            events[activeToolIndex].details = events[activeToolIndex].details
              ? `${events[activeToolIndex].details}\n${detail}`
              : detail
            events[activeToolIndex].summary = buildEventPreview(events[activeToolIndex].details || '', 'tool')
          }
          activeToolIndex = null
        } else {
          events.push({
            id: `${msg.turnId}_${item.id || idx}`,
            type: 'tool',
            title: 'tool',
            summary: buildEventPreview(detail, 'tool'),
            details: detail,
            time,
          })
        }
        return
      }
      if (mapped.type === 'thinking') {
        const thinkingDetails = detail || item.reasoningContent || ''
        events.push({
          id: `${msg.turnId}_${item.id || idx}`,
          type: 'thinking',
          title: 'thinking',
          summary: buildEventPreview(thinkingDetails, 'thinking'),
          details: thinkingDetails,
          time,
        })
        return
      }
      if (mapped.type === 'error') {
        events.push({
          id: `${msg.turnId}_${item.id || idx}`,
          type: 'error',
          title: 'error',
          summary: buildEventPreview(detail, 'error'),
          details: detail,
          time,
        })
      }
    })

    msg.processEvents = events
  } catch (e) {
    console.error('加载轮次事件失败:', e)
  }
}

const syncAppWorkflowLockState = () => {
  if (!appId.value) return
  isWorkflowLocked.value = isAppWorkflowModeLocked(appId.value)
}

const refreshVersions = async () => {
  if (!appId.value || (!isOwner.value && !isAdmin.value)) return
  loadingVersions.value = true
  try {
    const res = await listAppVersions({ appId: appId.value })
    if (res.data?.code === 0 && res.data.data) {
      versions.value = res.data.data
      if (!previewVersionId.value) {
        const defaultVersion = workingVersion.value || deployedVersion.value || versions.value[0]
        if (defaultVersion?.id) {
          previewVersionId.value = String(defaultVersion.id)
        }
      }
      if (!baseVersionId.value && workingVersion.value?.id) {
        baseVersionId.value = String(workingVersion.value.id)
      }
    }
    const historyRes = await listAppDeploymentHistory({ appId: appId.value })
    if (historyRes.data?.code === 0 && historyRes.data.data) {
      deploymentHistory.value = historyRes.data.data
    }
  } catch (error) {
    console.warn('获取版本列表失败：', error)
  } finally {
    loadingVersions.value = false
  }
}

const selectPreviewVersion = async (version: API.AppVersionVO) => {
  if (!version.id) return
  previewVersionId.value = String(version.id)
  const targetUrl = getStaticVersionPreviewUrl(version.id)
  const separator = targetUrl.includes('?') ? '&' : '?'
  previewUrl.value = `${targetUrl}${separator}v=${Date.now()}`
  previewStatus.value = 'ready'
  previewReady.value = false
  previewLoadError.value = false
  message.success(`已切换预览 V${version.versionNo}`)
}

const setModificationBase = (version: API.AppVersionVO) => {
  if (!version.id) return
  baseVersionId.value = String(version.id)
  message.success(`后续修改将基于 V${version.versionNo}`)
}

const openVersionCompare = (version: API.AppVersionVO) => {
  compareModalOpen.value = true
  compareRightVersionId.value = String(version.id)
  if (version.parentVersionId) {
    compareLeftVersionId.value = String(version.parentVersionId)
  } else {
    const other = versions.value.find(item => String(item.id) !== String(version.id))
    compareLeftVersionId.value = other?.id ? String(other.id) : undefined
  }
  if (compareLeftVersionId.value && compareRightVersionId.value) {
    void loadVersionCompare()
  }
}

const loadVersionCompare = async () => {
  if (!appId.value || !compareLeftVersionId.value || !compareRightVersionId.value) {
    message.warning('请选择需要对比的两个版本')
    return
  }
  loadingCompare.value = true
  try {
    const res = await compareAppVersions({
      appId: String(appId.value),
      leftVersionId: String(compareLeftVersionId.value),
      rightVersionId: String(compareRightVersionId.value),
    })
    if (res.data?.code === 0 && res.data.data) {
      compareFiles.value = res.data.data.files || []
    } else {
      message.error(res.data?.message || '版本比对失败')
    }
  } catch {
    message.error('版本比对请求失败')
  } finally {
    loadingCompare.value = false
  }
}

const deployVersion = async (version: API.AppVersionVO) => {
  if (!appId.value || !version.id) return
  deploying.value = true
  deployingVersionId.value = String(version.id)
  try {
    const res = await deployAppApi({
      appId: appId.value,
      versionId: version.id as unknown as number,
    })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success(`V${version.versionNo} 部署成功`)
      await fetchAppInfo()
      await refreshVersions()
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch {
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
    deployingVersionId.value = undefined
  }
}

const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id
  syncAppWorkflowLockState()

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      await refreshVersions()
      await updatePreview(3)
      await refreshChatMetrics()
      if (!historyLoaded.value) {
        await loadChatHistory()
      }
    } else {
      message.error('获取应用信息失败：' + res.data.message)
      router.push('/')
    }
  } catch {
    message.error('获取应用信息失败')
    router.push('/')
  }
}

const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: Number(appId.value),
      pageSize: 10,
    }
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        const historyMessages: Message[] = chatHistories
          .map((chat: API.ChatHistory) => ({
            type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: chat.messageType === 'assistant' ? sanitizeAssistantContent(chat.message || '') : (chat.message || ''),
            createTime: chat.createTime,
            turnId: chat.turnId,
            processEvents: [],
          }))
          .reverse()
        if (isLoadMore) {
          messages.value.unshift(...historyMessages)
        } else {
          messages.value = historyMessages
        }
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        hasMoreHistory.value = chatHistories.length === 10
        for (const msg of historyMessages) {
          if (msg.type === 'ai' && msg.turnId) {
            await hydrateTurnEventsForMessage(msg)
          }
        }
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
  } finally {
    loadingHistory.value = false
  }
}

const loadMoreHistory = async () => {
  if (!hasMoreHistory.value || loadingHistory.value) return
  const currentContainer = messagesContainer.value
  const oldScrollHeight = currentContainer?.scrollHeight || 0
  await loadChatHistory()
  nextTick(() => {
    if (currentContainer) {
      const newScrollHeight = currentContainer.scrollHeight
      currentContainer.scrollTop = newScrollHeight - oldScrollHeight
    }
  })
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) return
  if (!isOwner.value) {
    message.warning('无法在别人的作品下对话哦~')
    return
  }

  const content = userInput.value.trim()
  userInput.value = ''
  shouldAutoScroll.value = true

  let elementContextPrompt = ''
  if (selectedElementInfo.value) {
    elementContextPrompt = `【当前正在针对页面局部元素进行修改】\n- 标签: <${selectedElementInfo.value.tagName.toLowerCase()}>\n- CSS 选择器: ${selectedElementInfo.value.selector}\n`
    if (selectedElementInfo.value.id) elementContextPrompt += `- ID: #${selectedElementInfo.value.id}\n`
    if (selectedElementInfo.value.className) elementContextPrompt += `- 类名: .${selectedElementInfo.value.className.split(' ').join('.')}\n`
    if (selectedElementInfo.value.textContent) elementContextPrompt += `- 元素文本摘要: ${selectedElementInfo.value.textContent.substring(0, 100)}\n`
    if (selectedElementInfo.value.pagePath) elementContextPrompt += `- 页面路径: ${selectedElementInfo.value.pagePath}\n`
    elementContextPrompt += '\n请重点针对上述指定元素及周边相关结构执行修改。\n'
  }

  messages.value.push({
    type: 'user',
    content: content,
  })

  nextTick(() => {
    scrollToBottom(true)
  })

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  isGenerating.value = true

  const clientRequestId = `req_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  const currentMode = getCurrentGenerationMode()
  if (currentMode === 'workflow' && appId.value) {
    lockAppWorkflowMode(appId.value)
    syncAppWorkflowLockState()
  }

  const API_BASE_URL = request.defaults.baseURL || ''
  const chatEndpoint = currentMode === 'workflow' ? '/app/chat/workflow' : '/app/chat'
  const finalMessage = elementContextPrompt ? `${elementContextPrompt}\n用户修改诉求: ${content}` : content

  const requestBody: Record<string, unknown> = {
    appId: appId.value,
    message: finalMessage,
    clientRequestId,
    agent: selectedAgent.value,
  }

  if (baseVersionId.value) {
    requestBody.baseVersionId = baseVersionId.value
  }

  clearSelectedElement()

  try {
    const response = await fetch(`${API_BASE_URL}${chatEndpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
      credentials: 'include',
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is null')
    }

    const decoder = new TextDecoder()
    let buffer = ''
    let assistantRaw = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const rawData = line.slice(5).trim()
          if (!rawData) continue

          if (rawData === '[DONE]') {
            messages.value[aiMessageIndex].loading = false
            continue
          }

          try {
            const parsed = JSON.parse(rawData)
            if (parsed.d) {
              const currentAiMsg = messages.value[aiMessageIndex]
              consumeStreamBufferAndAppend(currentAiMsg, parsed.d, (textChunk) => {
                const merged = mergeStreamSnapshotOrDelta(assistantRaw, textChunk)
                assistantRaw = merged.next
                currentAiMsg.content = sanitizeAssistantContent(assistantRaw)
              })

              nextTick(() => {
                scrollToBottom()
              })
            }
          } catch {
            const currentAiMsg = messages.value[aiMessageIndex]
            consumeStreamBufferAndAppend(currentAiMsg, rawData, (textChunk) => {
              const merged = mergeStreamSnapshotOrDelta(assistantRaw, textChunk)
              assistantRaw = merged.next
              currentAiMsg.content = sanitizeAssistantContent(assistantRaw)
            })

            nextTick(() => {
              scrollToBottom()
            })
          }
        }
      }
    }

    const currentAiMsg = messages.value[aiMessageIndex]
    flushStreamBuffer(currentAiMsg, (textChunk) => {
      const merged = mergeStreamSnapshotOrDelta(assistantRaw, textChunk)
      assistantRaw = merged.next
      currentAiMsg.content = sanitizeAssistantContent(assistantRaw)
    })

    currentAiMsg.content = sanitizeAssistantContent(assistantRaw || currentAiMsg.content)
    messages.value[aiMessageIndex].loading = false

    await updatePreview(4)
    await refreshVersions()
    await refreshChatMetrics()
  } catch (error) {
    console.error('发送消息失败：', error)
    messages.value[aiMessageIndex].loading = false
    messages.value[aiMessageIndex].content = '抱歉，生成过程中遇到网络或服务异常，请稍后重试。'
  } finally {
    isGenerating.value = false
    nextTick(() => {
      scrollToBottom()
    })
  }
}

const showAppDetail = () => {
  appDetailVisible.value = true
}

const deployApp = async () => {
  if (!appId.value) return
  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value,
      versionId: workingVersion.value?.id as unknown as number,
    })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
      await fetchAppInfo()
      await refreshVersions()
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch {
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

const updatePreview = async (retries = 1) => {
  const codeGenType = appInfo.value?.codeGenType
  const currentAppId = appId.value

  if (!codeGenType || !currentAppId) {
    previewStatus.value = 'missing'
    return
  }

  const preferredVersionId = previewVersionId.value || workingVersion.value?.id || deployedVersion.value?.id
  const sourceUrl = preferredVersionId
    ? getStaticVersionPreviewUrl(preferredVersionId)
    : getStaticPreviewUrl(codeGenType, String(currentAppId))

  if (!sourceUrl) {
    previewStatus.value = 'missing'
    return
  }

  previewStatus.value = 'checking'
  const hadWorkingPreview = Boolean(previewUrl.value)

  for (let attempt = 1; attempt <= retries; attempt += 1) {
    try {
      const response = await fetch(sourceUrl, { method: 'HEAD', cache: 'no-store' })
      if (response.ok) {
        const separator = sourceUrl.includes('?') ? '&' : '?'
        previewUrl.value = `${sourceUrl}${separator}v=${Date.now()}`
        previewStatus.value = 'ready'
        previewLoadError.value = false
        previewReady.value = false
        return
      }
    } catch (error) {
      console.warn('检查应用预览失败：', error)
      const separator = sourceUrl.includes('?') ? '&' : '?'
      previewUrl.value = `${sourceUrl}${separator}v=${Date.now()}`
      previewStatus.value = 'ready'
      previewLoadError.value = false
      previewReady.value = false
      return
    }

    if (attempt < retries) {
      await new Promise((resolve) => window.setTimeout(resolve, 800))
    }
  }

  previewStatus.value = hadWorkingPreview ? 'ready' : 'missing'
}

const refreshPreview = () => {
  void updatePreview(2)
}

const isNearBottom = () => {
  if (!messagesContainer.value) {
    return true
  }
  const container = messagesContainer.value
  const threshold = 80
  return container.scrollHeight - (container.scrollTop + container.clientHeight) <= threshold
}

const handleMessagesScroll = () => {
  shouldAutoScroll.value = isNearBottom()
}

const scrollToBottom = (force = false) => {
  if (!force && !shouldAutoScroll.value) {
    return
  }
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    if (force) {
      shouldAutoScroll.value = true
    }
  }
}

const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch {
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

const onIframeLoad = () => {
  previewReady.value = true
  previewLoadError.value = false
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

const onIframeError = () => {
  previewReady.value = false
  previewLoadError.value = true
}

const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

const deleteApp = async () => {
  if (!appInfo.value?.id) return
  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('删除失败')
  }
}

const toggleEditMode = () => {
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe || !previewReady.value) {
    message.warning('请等待页面渲染完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `针对选中元素 <${selectedElementInfo.value.tagName.toLowerCase()}> 描述您的调整要求...`
  }
  return '输入你的迭代诉求（如：修改导航条颜色、新增数据统计卡片、优化排版...）'
}

onMounted(() => {
  fetchAppInfo()
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

onUnmounted(() => {
  // cleanup
})
</script>

<style scoped>
#appChatPage {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: #F8FAFC;
  overflow: hidden;
}

/* Header Bar */
.chat-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 54px;
  padding: 0 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  flex-shrink: 0;
  z-index: 20;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-home-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #64748B;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
  transition: all 0.15s ease;
}

.back-home-btn:hover {
  color: #0F172A;
  background: #E2E8F0;
}

.header-app-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-app-title {
  margin: 0;
  font-size: 15px;
  font-weight: 750;
  color: #0F172A;
  letter-spacing: -0.01em;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-status-tags {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tag-codegen {
  background: rgba(99, 102, 241, 0.08);
  color: #6366F1;
  border-radius: 4px;
  font-weight: 500;
}

.tag-deployed {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
  border-radius: 4px;
  font-weight: 600;
}

.tag-locked {
  background: rgba(245, 158, 11, 0.1);
  color: #D97706;
  border-radius: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-tool {
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
}

.badge-version {
  margin-left: 6px;
  padding: 1px 6px;
  background: rgba(99, 102, 241, 0.1);
  color: #4F46E5;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.btn-deploy {
  border-radius: 8px;
  font-weight: 600;
  background: linear-gradient(135deg, #6366F1, #4F46E5);
}

/* Dual Pane Workspace */
.chat-workspace {
  display: grid;
  grid-template-columns: minmax(420px, 460px) minmax(0, 1fr);
  flex: 1;
  height: calc(100% - 54px);
  overflow: hidden;
}

/* Left Pane */
.chat-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #FFFFFF;
  border-right: 1px solid #E2E8F0;
  overflow: hidden;
}

.messages-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.history-loader-wrap {
  text-align: center;
  margin-bottom: 8px;
}

/* User Message */
.user-bubble-wrap {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.user-bubble {
  max-width: 82%;
  padding: 12px 16px;
  background: #6366F1;
  color: #FFFFFF;
  border-radius: 16px 4px 16px 16px;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
}

.bubble-text {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.avatar-user {
  flex-shrink: 0;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: #FFFFFF;
  font-weight: 600;
}

/* AI Message */
.ai-bubble-wrap {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.ai-avatar-wrap {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
}

.avatar-ai-img {
  width: 34px;
  height: 34px;
  object-fit: contain;
}

.ai-bubble-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: calc(100% - 46px);
  width: 100%;
}

/* Agent Plan Card */
.agent-plan-card {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.plan-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.plan-title-box {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #1E293B;
}

.plan-head-icon {
  color: #6366F1;
}

.plan-counter-pill {
  font-size: 11px;
  font-weight: 700;
  color: #4F46E5;
  background: rgba(99, 102, 241, 0.1);
  padding: 1px 8px;
  border-radius: 999px;
}

.plan-items-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.plan-task-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
}

.task-status-icon {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  flex-shrink: 0;
}

.icon-completed {
  color: #10B981;
}

.icon-running {
  color: #6366F1;
}

.icon-pending {
  color: #CBD5E1;
}

.task--done .task-text {
  color: #64748B;
  text-decoration: line-through;
  text-decoration-color: #CBD5E1;
}

.task--running .task-text {
  color: #4F46E5;
  font-weight: 600;
}

.plan-progress-track {
  width: 100%;
  height: 4px;
  background: #E2E8F0;
  border-radius: 999px;
  overflow: hidden;
}

.plan-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #6366F1, #10B981);
  border-radius: 999px;
  transition: width 0.3s ease;
}

/* Process Events */
.process-events-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.process-item-box {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
}

.event-details-tag {
  width: 100%;
}

.event-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  list-style: none;
}

.event-summary-row::-webkit-details-marker {
  display: none;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.event-icon {
  font-size: 13px;
  flex-shrink: 0;
}

.icon--thinking {
  color: #F59E0B;
}

.icon--tool {
  color: #6366F1;
}

.event-type-label {
  font-size: 11px;
  font-weight: 700;
  color: #64748B;
  text-transform: uppercase;
}

.event-summary-text {
  font-size: 12px;
  color: #334155;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-time-stamp {
  font-size: 11px;
  color: #94A3B8;
  flex-shrink: 0;
}

.event-details-pre-wrap {
  padding: 10px 12px;
  background: #0F172A;
  border-top: 1px solid #1E293B;
  overflow-x: auto;
}

.event-pre-code {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  line-height: 1.5;
  color: #E2E8F0;
  white-space: pre-wrap;
}

/* Final Reply */
.final-reply-box {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.final-reply-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #F8FAFC;
  border-bottom: 1px solid #F1F5F9;
  cursor: pointer;
  user-select: none;
}

.final-reply-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.final-reply-check {
  color: #10B981;
  font-size: 14px;
}

.final-reply-heading {
  font-size: 13px;
  font-weight: 700;
  color: #1E293B;
}

.final-reply-caret {
  color: #94A3B8;
  font-size: 11px;
}

.final-reply-content {
  padding: 14px 16px;
}

.stream-loading-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #F8FAFC;
  border-radius: 8px;
}

.stream-loading-text {
  font-size: 12px;
  color: #6366F1;
  font-weight: 500;
}

.turn-version-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(139, 92, 246, 0.06);
  border: 1px solid rgba(139, 92, 246, 0.2);
  border-radius: 8px;
  font-size: 12px;
}

.turn-version-label {
  color: #6D28D9;
  font-weight: 500;
}

.turn-tag-link {
  cursor: pointer;
  font-weight: 600;
}

/* Input Dock Area */
.input-dock-container {
  padding: 12px 16px 16px;
  background: #FFFFFF;
  border-top: 1px solid #E2E8F0;
  flex-shrink: 0;
}

.input-dock-card {
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input-dock-card:focus-within {
  border-color: #818CF8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
  background: #FFFFFF;
}

.dock-meta-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-bottom: 1px solid #F1F5F9;
  background: #FFFFFF;
}

.agent-switch-pill {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pill-prefix {
  font-size: 11px;
  color: #94A3B8;
  font-weight: 600;
}

.compact-agent-select {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.metrics-display-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748B;
}

.pill-sep {
  color: #CBD5E1;
}

.base-version-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #6366F1;
  background: rgba(99, 102, 241, 0.08);
  padding: 1px 8px;
  border-radius: 4px;
}

.btn-change-base {
  background: transparent;
  border: none;
  color: #4F46E5;
  font-size: 11px;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
}

/* Inspector Selection Banner */
.inspector-selection-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: rgba(16, 185, 129, 0.08);
  border-bottom: 1px solid rgba(16, 185, 129, 0.2);
}

.inspector-info-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #065F46;
}

.inspector-icon {
  color: #10B981;
}

.inspector-tag-name {
  font-weight: 700;
}

.inspector-selector-code {
  font-family: ui-monospace, SFMono-Regular, monospace;
  font-size: 11px;
  background: rgba(255, 255, 255, 0.8);
  padding: 1px 5px;
  border-radius: 4px;
}

.inspector-cancel-btn {
  background: transparent;
  border: none;
  font-size: 11px;
  color: #DC2626;
  cursor: pointer;
  font-weight: 600;
}

/* Textarea Container */
.textarea-container {
  display: flex;
  flex-direction: column;
  padding: 10px 14px 10px;
}

.prompt-textarea {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  font-size: 14px;
  line-height: 1.55;
  resize: none;
  padding: 0;
  color: #0F172A !important;
}

.textarea-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.textarea-hint {
  font-size: 11px;
  color: #94A3B8;
}

.send-circle-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.35);
}

/* Right Pane: Live Preview */
.preview-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #F1F5F9;
  overflow: hidden;
}

.preview-browser-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  flex-shrink: 0;
}

.browser-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.traffic-lights {
  display: flex;
  align-items: center;
  gap: 6px;
}

.light {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.light--red {
  background: #EF4444;
}

.light--yellow {
  background: #F59E0B;
}

.light--green {
  background: #10B981;
}

.device-switcher-tabs {
  display: flex;
  align-items: center;
  background: #F1F5F9;
  border-radius: 6px;
  padding: 2px;
}

.device-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: #64748B;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.device-tab:hover {
  color: #0F172A;
}

.device-tab--active {
  background: #FFFFFF;
  color: #4F46E5;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.simulated-url-bar {
  display: flex;
  align-items: center;
  padding: 3px 10px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  font-size: 12px;
  max-width: 220px;
}

.url-scheme {
  color: #94A3B8;
}

.url-host {
  color: #334155;
  font-weight: 500;
}

.url-route {
  color: #6366F1;
}

.browser-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inspector-toggle-btn {
  font-size: 12px;
  border-radius: 6px;
  font-weight: 500;
}

.inspector-toggle-btn--active {
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2);
}

.preview-tool-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: transparent;
  border: 1px solid #E2E8F0;
  color: #64748B;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preview-tool-btn:hover {
  background: #F1F5F9;
  color: #0F172A;
}

/* Screen Viewport */
.preview-screen-viewport {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 12px;
  position: relative;
}

.iframe-canvas-wrap {
  position: relative;
  height: 100%;
  background: #FFFFFF;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.device-mode--desktop {
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #E2E8F0;
}

.device-mode--tablet {
  width: 768px;
  max-width: 100%;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.1);
  border: 1px solid #CBD5E1;
}

.device-mode--mobile {
  width: 375px;
  max-width: 100%;
  border-radius: 20px;
  box-shadow: 0 14px 38px rgba(15, 23, 42, 0.12);
  border: 2px solid #CBD5E1;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: inherit;
}

.preview-error-overlay,
.preview-placeholder-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 32px;
  text-align: center;
}

.error-center-icon {
  font-size: 36px;
  color: #EF4444;
  margin-bottom: 12px;
}

.placeholder-icon-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  font-size: 24px;
  color: #6366F1;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.state-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1E293B;
}

.state-desc {
  margin: 6px 0 16px;
  font-size: 13px;
  color: #64748B;
  max-width: 320px;
}

/* Drawer & Modal Styles */
.drawer-header-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: #64748B;
  margin-bottom: 16px;
}

.current-base-banner {
  margin-bottom: 16px;
  border-radius: 8px;
}

.version-cards-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-entry-card {
  padding: 14px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
}

.version-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.version-number-tag {
  font-size: 15px;
  font-weight: 800;
  color: #0F172A;
}

.version-status-pills {
  display: flex;
  gap: 4px;
}

.version-meta-time {
  font-size: 12px;
  color: #64748B;
  margin-bottom: 6px;
}

.version-prompt-text {
  font-size: 13px;
  color: #334155;
  background: #FFFFFF;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
  margin-bottom: 8px;
  line-height: 1.5;
}

.version-specs {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #94A3B8;
  margin-bottom: 12px;
}

.version-actions-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.deploy-history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.deploy-history-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #F8FAFC;
  border-radius: 6px;
}

.deploy-time {
  font-size: 12px;
  color: #64748B;
}

.compare-controls-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.compare-vs-badge {
  font-size: 12px;
  font-weight: 800;
  color: #6366F1;
}

.unified-diff-view {
  background: #0F172A;
  color: #E2E8F0;
  padding: 14px;
  border-radius: 8px;
  font-family: ui-monospace, SFMono-Regular, monospace;
  font-size: 12px;
  line-height: 1.6;
  max-height: 380px;
  overflow: auto;
}

@media (max-width: 900px) {
  .chat-workspace {
    grid-template-columns: 1fr;
  }

  .preview-pane {
    display: none;
  }
}
</style>
