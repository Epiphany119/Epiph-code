<template>
  <div id="appChatPage">
    <!-- 顶部 IDE 工作台导航栏 -->
    <div class="header-bar">
      <div class="header-left">
        <button type="button" class="back-workspace-btn" title="返回工作台首页" @click="goBackWorkspace">
          <ArrowLeftOutlined />
          <span>工作台</span>
        </button>
        <span class="header-sep"></span>
        <div class="app-title-group" @click="showAppDetail">
          <h1 class="app-name" :title="appInfo?.appName || '网站生成器'">
            {{ appInfo?.appName || '网站生成器' }}
          </h1>
          <a-tag v-if="appInfo?.codeGenType" class="code-gen-type-tag">
            {{ formatCodeGenType(appInfo.codeGenType) }}
          </a-tag>
          <a-tag v-if="deployedVersion" color="green" class="status-meta-tag">
            线上 V{{ deployedVersion.versionNo }}
          </a-tag>
          <a-tag v-if="isWorkflowLocked" color="gold" class="status-meta-tag">
            工作流锁定
          </a-tag>
        </div>
      </div>

      <div class="header-right">
        <a-button v-if="isOwner || isAdmin" class="toolbar-btn" @click="versionsDrawerOpen = true">
          <template #icon><HistoryOutlined /></template>
          <span>版本历史</span>
          <span v-if="workingVersion" class="btn-version-pill">V{{ workingVersion.versionNo }}</span>
        </a-button>
        <a-button type="default" class="toolbar-btn" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用信息
        </a-button>
        <a-button
          type="default"
          class="toolbar-btn"
          @click="downloadCode"
          :loading="downloading"
          :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载源码
        </a-button>
        <a-button
          type="primary"
          class="deploy-primary-btn"
          @click="deployApp"
          :loading="deploying"
          :disabled="!(isOwner || isAdmin) || !workingVersion"
        >
          <template #icon>
            <CloudUploadOutlined />
          </template>
          发布上线
        </a-button>
      </div>
    </div>

    <!-- 主要双栏 IDE 工作区域 -->
    <div ref="mainContentRef" class="main-content" :class="{ 'is-resizing': isResizingSplit }">
      <!-- 左侧 AI Copilot 对话区域 -->
      <div v-show="!isPreviewFullscreen" class="chat-section" :style="chatPaneStyle">
        <div class="chat-pane-header">
          <div class="chat-pane-title">
            <span class="copilot-dot"></span>
            <span>AI Copilot 对话流</span>
          </div>
          <span class="chat-pane-sub">{{ messages.length }} 条消息记录</span>
        </div>

        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer" @scroll="handleMessagesScroll">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              ↑ 加载更早的历史对话
            </a-button>
          </div>

          <div v-if="!messages.length && !loadingHistory" class="chat-welcome-card">
            <div class="welcome-badge">AI ENGINEER READY</div>
            <h4>开始迭代你的应用</h4>
            <p>你可以用自然语言描述新增页面、调整配色排版，或开启右侧「可视化点选」直接指定任意 DOM 元素进行局部修改。</p>
          </div>

          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32" class="user-msg-avatar">
                  {{ displayUserName.charAt(0).toUpperCase() }}
                </a-avatar>
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" :size="32" />
              </div>
              <div class="message-content">
                <div class="ai-msg-meta">
                  <span class="ai-author-name">Epiph AI</span>
                  <span class="ai-role-pill">Full-Stack Agent</span>
                </div>

                <!-- 计划进度面板：仅在最后一条AI消息上展示 -->
                <div v-if="currentPlan.length > 0 && index === messages.length - 1" class="plan-panel">
                  <div class="plan-header">
                    <span class="plan-title">TODO 构建计划</span>
                    <span class="plan-progress">
                      {{ currentPlan.filter(i => i.status === 'completed').length }}/{{ currentPlan.length }}
                    </span>
                  </div>
                  <div class="plan-items">
                    <div
                      v-for="item in currentPlan"
                      :key="item.id"
                      class="plan-item"
                      :class="{
                        'plan-item-completed': item.status === 'completed',
                        'plan-item-active': item.status === 'in_progress',
                      }"
                      :style="{ paddingLeft: getPlanItemDepth(item, currentPlan) * 20 + 'px' }"
                    >
                      <span class="plan-item-icon">
                        <template v-if="item.status === 'completed'">&#10003;</template>
                        <template v-else-if="item.status === 'in_progress'">
                          <span class="plan-item-spinner"></span>
                        </template>
                        <template v-else>
                          <span class="plan-item-dot"></span>
                        </template>
                      </span>
                      <span class="plan-item-text">{{ item.text }}</span>
                    </div>
                  </div>
                  <div class="plan-bar">
                    <div
                      class="plan-bar-fill"
                      :style="{
                        width: (currentPlan.filter(i => i.status === 'completed').length / currentPlan.length * 100) + '%',
                      }"
                    ></div>
                  </div>
                </div>

                <ProcessTimeline
                  v-if="message.processEvents?.length"
                  :events="message.processEvents"
                />

                <div v-if="message.content" class="final-reply">
                  <div class="final-reply-header" @click="toggleFinalReply(index)">
                    <span class="final-reply-toggle">{{ !collapsedReplies.has(index) ? '▼' : '▶' }}</span>
                    <span class="final-reply-title">最终回复与交付说明</span>
                  </div>
                  <div v-show="!collapsedReplies.has(index)" class="final-reply-body">
                    <MarkdownRenderer :content="message.content" />
                  </div>
                </div>

                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在规划与编写代码...</span>
                </div>

                <a-tag
                  v-if="message.turnId && versionForTurn(message.turnId)"
                  color="green"
                  class="turn-version-tag"
                  @click="previewTurnVersion(message.turnId)"
                >
                  ✨ 已生成快照 V{{ versionForTurn(message.turnId)?.versionNo }} · 点击预览
                </a-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
          v-if="selectedElementInfo"
          class="selected-element-alert"
          type="info"
          closable
          @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  已锁定元素：&lt;{{ selectedElementInfo.tagName.toLowerCase() }}&gt;
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  文本: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <div class="input-dock-meta">
              <div class="input-agent-bar">
                <span class="input-agent-label">模型</span>
                <a-select
                  v-model:value="selectedAgent"
                  size="small"
                  :bordered="false"
                  :disabled="isGenerating || !isOwner"
                >
                  <a-select-option value="default">默认 Agent</a-select-option>
                  <a-select-option value="deepseek">DeepSeek Flash</a-select-option>
                </a-select>
              </div>

              <div v-if="isOwner || isAdmin" class="chat-metrics-compact" aria-label="对话用量指标">
                <a-tooltip
                  :title="`输入 ${formatMetricTokens(chatMetrics?.lastTurnTokenUsage?.inputTokens)} · 输出 ${formatMetricTokens(chatMetrics?.lastTurnTokenUsage?.outputTokens)}${lastTurnAgentLabel ? ' (' + lastTurnAgentLabel + ')' : ''}`"
                >
                  <span class="metric-pill">
                    Token: <strong>{{ formatMetricTokens(chatMetrics?.lastTurnTokenUsage?.totalTokens) }}</strong>
                  </span>
                </a-tooltip>
                <a-tooltip :title="`${contextMetricCaption}（按约 3 字符 = 1 token 估算）`">
                  <span class="metric-pill metric-pill--context">
                    <span>
                      上下文:
                      <strong>{{
                        chatMetrics ? `~${formatMetricTokens(chatMetrics.contextTokensEstimate)}` : '读取中'
                      }}</strong>
                    </span>
                    <span
                      class="context-meter"
                      role="progressbar"
                      :aria-valuenow="contextProgressPercent"
                      aria-valuemin="0"
                      aria-valuemax="100"
                    >
                      <span :style="{ width: `${contextProgressPercent}%` }"></span>
                    </span>
                  </span>
                </a-tooltip>
              </div>

              <div v-if="isOwner && modificationBaseVersion" class="version-base-bar">
                <span>基线 <strong>V{{ modificationBaseVersion.versionNo }}</strong></span>
                <a-button type="link" size="small" @click="versionsDrawerOpen = true">切换</a-button>
              </div>
            </div>

            <a-tooltip v-if="!isOwner" title="仅支持在自己创建的应用中发起对话迭代" placement="top">
              <a-textarea
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="3"
                :maxlength="1000"
                @keydown.enter.exact.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              :placeholder="getInputPlaceholder()"
              :rows="3"
              :maxlength="1000"
              @keydown.enter.exact.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间可拖拽分栏调节线（双击恢复默认比例） -->
      <div
        v-show="!isPreviewFullscreen"
        class="split-resizer"
        role="separator"
        aria-orientation="vertical"
        title="左右拖拽调整对话与预览比例，双击恢复默认"
        @mousedown="startSplitResize"
        @dblclick="resetSplitRatio"
      >
        <span class="split-resizer-handle"></span>
      </div>

      <!-- 右侧网页实时沙箱预览区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <div class="preview-heading">
            <div class="browser-dots" aria-hidden="true">
              <span class="b-dot b-dot--red"></span>
              <span class="b-dot b-dot--amber"></span>
              <span class="b-dot b-dot--green"></span>
            </div>
            <div class="preview-title-meta">
              <h3>实时沙箱</h3>
              <span class="preview-heading-caption">
                {{ previewVersion ? `V${previewVersion.versionNo}` : '工作区' }}
                <template v-if="previewVersion?.deployed"> · 线上</template>
              </span>
            </div>
            <span v-if="previewUrl" class="preview-status preview-status--ready">
              <i></i> {{ isGenerating || deploying ? '同步中' : '运行中' }}
            </span>
            <span v-else-if="isGenerating" class="preview-status">
              <i></i> 构建中
            </span>
          </div>

          <!-- 中部：设备视口切换器 & 模拟地址栏 -->
          <div class="preview-center-bar">
            <div class="device-switcher" role="group" aria-label="切换预览设备尺寸">
              <button
                type="button"
                class="device-btn"
                :class="{ 'device-btn--active': previewDevice === 'desktop' }"
                title="桌面视口 (100%)"
                @click="previewDevice = 'desktop'"
              >
                <DesktopOutlined />
              </button>
              <button
                type="button"
                class="device-btn"
                :class="{ 'device-btn--active': previewDevice === 'tablet' }"
                title="平板视口 (768px)"
                @click="previewDevice = 'tablet'"
              >
                <TabletOutlined />
              </button>
              <button
                type="button"
                class="device-btn"
                :class="{ 'device-btn--active': previewDevice === 'mobile' }"
                title="手机视口 (390px)"
                @click="previewDevice = 'mobile'"
              >
                <MobileOutlined />
              </button>
            </div>

            <div class="preview-url-bar">
              <span class="url-host-badge">LOCAL</span>
              <span class="url-path-text" :title="previewUrl || '等待生成可运行版本'">
                {{ previewUrl ? `studio.epiph.ai/app/${appId || ''}` : '等待构建输出...' }}
              </span>
              <button
                v-if="previewUrl"
                type="button"
                class="url-refresh-btn"
                title="刷新预览画面"
                @click="refreshPreview"
              >
                <ReloadOutlined />
              </button>
            </div>
          </div>

          <!-- 右侧操作按钮 -->
          <div class="preview-actions">
            <button
              v-if="isOwner && previewUrl"
              type="button"
              class="preview-tool-pill"
              :class="{ 'preview-tool-pill--active': isEditMode }"
              @click="toggleEditMode"
            >
              <AimOutlined v-if="isEditMode" />
              <EditOutlined v-else />
              <span>{{ isEditMode ? '退出点选' : '点选修改' }}</span>
            </button>
            <button
              type="button"
              class="preview-tool-pill"
              @click="togglePreviewFullscreen"
            >
              <FullscreenExitOutlined v-if="isPreviewFullscreen" />
              <FullscreenOutlined v-else />
              <span>{{ isPreviewFullscreen ? '退出全屏' : '全屏' }}</span>
            </button>
            <button
              v-if="previewUrl"
              type="button"
              class="preview-tool-pill"
              title="在新浏览器标签页中打开"
              @click="openInNewTab"
            >
              <ExportOutlined />
              <span>新窗口</span>
            </button>
          </div>
        </div>

        <div class="preview-content" :class="`preview-content--${previewDevice}`">
          <div v-if="previewUrl" class="preview-frame-wrap" :class="`frame-wrap--${previewDevice}`">
            <iframe
              :key="previewUrl"
              :src="previewUrl"
              class="preview-iframe"
              title="生成应用实时预览"
              frameborder="0"
              @load="onIframeLoad"
              @error="onIframeError"
            ></iframe>
            <div v-if="previewLoadError" class="preview-frame-error">
              <span class="placeholder-icon">⚠</span>
              <p>预览页面暂时无法加载</p>
              <a-button type="primary" ghost @click="refreshPreview">重新加载</a-button>
            </div>
          </div>
          <div v-else-if="isGenerating || previewStatus === 'checking'" class="preview-loading">
            <a-spin size="large" />
            <p>{{ isGenerating ? 'AI 正在编译与生成页面文件，完成后将自动渲染预览...' : '正在检查可预览版本…' }}</p>
          </div>
          <div v-else class="preview-placeholder">
            <div class="placeholder-icon">&lt;/&gt;</div>
            <h4>画布准备就绪</h4>
            <p>{{ messages.length ? '当前还没有可运行的页面版本' : '在左侧输入需求后，生成的真实网页将在这里实时运行' }}</p>
            <a-button size="small" @click="refreshPreview">重新检查</a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />

    <a-drawer v-model:open="versionsDrawerOpen" title="网页版本快照与回溯" placement="right" width="460">
      <div class="version-drawer-intro">
        <span>每次成功生成都会保存为不可变版本快照。切换预览不会更改修改基线。</span>
        <a-button size="small" @click="refreshVersions" :loading="loadingVersions">刷新</a-button>
      </div>
      <a-alert v-if="modificationBaseVersion" type="info" show-icon class="version-current-base">
        <template #message>当前修改基线：V{{ modificationBaseVersion.versionNo }}</template>
      </a-alert>
      <a-empty v-if="!versions.length && !loadingVersions" description="还没有网页版本" />
      <a-spin v-else-if="loadingVersions" />
      <div v-else class="version-list">
        <div v-for="version in versions" :key="version.id" class="version-item">
          <div class="version-item-heading">
            <strong>V{{ version.versionNo }}</strong>
            <a-tag v-if="version.working" color="blue">工作版本</a-tag>
            <a-tag v-if="version.deployed" color="green">线上版本</a-tag>
            <a-tag v-if="String(previewVersionId) === String(version.id)" color="purple">正在预览</a-tag>
          </div>
          <div class="version-item-time">
            {{ formatVersionTime(version.createTime) }} · {{ formatVersionType(version.versionType) }}
          </div>
          <div v-if="version.userPrompt" class="version-item-prompt">{{ version.userPrompt }}</div>
          <div class="version-item-meta">
            <span v-if="version.parentVersionId">基于 V{{ versionNoById(version.parentVersionId) }}</span>
            <span>{{ formatBytes(version.totalBytes) }}</span>
            <span v-if="version.agent">{{ version.agent === 'deepseek' ? 'DeepSeek Flash' : '默认 Agent' }}</span>
          </div>
          <div class="version-item-actions">
            <a-button size="small" @click="selectPreviewVersion(version)">预览</a-button>
            <a-button size="small" :disabled="isGenerating" @click="setModificationBase(version)">基于此版本修改</a-button>
            <a-button size="small" :disabled="!isOwner" @click="openVersionCompare(version)">Diff 对比</a-button>
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
      <a-divider v-if="deploymentHistory.length" orientation="left">部署记录</a-divider>
      <div v-if="deploymentHistory.length" class="deployment-history-list">
        <div v-for="record in deploymentHistory" :key="record.id" class="deployment-history-item">
          <a-tag color="green">发布 V{{ record.versionNo }}</a-tag>
          <span>{{ formatVersionTime(record.deployedTime) }}</span>
        </div>
      </div>
    </a-drawer>

    <a-modal v-model:open="compareModalOpen" title="版本代码 Diff 比较" width="820px" :footer="null">
      <div class="version-compare-controls">
        <a-select v-model:value="compareLeftVersionId" style="min-width: 150px" placeholder="旧版本">
          <a-select-option v-for="version in versions" :key="version.id" :value="version.id">
            V{{ version.versionNo }}
          </a-select-option>
        </a-select>
        <span>对比</span>
        <a-select v-model:value="compareRightVersionId" style="min-width: 150px" placeholder="新版本">
          <a-select-option v-for="version in versions" :key="version.id" :value="version.id">
            V{{ version.versionNo }}
          </a-select-option>
        </a-select>
        <a-button type="primary" :loading="loadingCompare" @click="loadVersionCompare">比较</a-button>
      </div>
      <a-empty v-if="compareFiles.length === 0 && !loadingCompare" description="没有文件差异或尚未比较" />
      <a-collapse v-else :bordered="false">
        <a-collapse-panel
          v-for="file in compareFiles"
          :key="file.path"
          :header="`${file.path} · ${file.status} · ${file.oldBytes ?? '—'}B → ${file.newBytes ?? '—'}B`"
        >
          <pre class="version-diff">{{ file.unifiedDiff }}</pre>
        </a-collapse-panel>
      </a-collapse>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
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
import ProcessTimeline from '@/components/ProcessTimeline.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.svg'
import { API_BASE_URL, getStaticPreviewUrl, getStaticVersionPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  ArrowLeftOutlined,
  HistoryOutlined,
  ReloadOutlined,
  DesktopOutlined,
  TabletOutlined,
  MobileOutlined,
  AimOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()
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
  if (value === undefined || value === null) return '暂无数据'
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

// 当前生成计划状态（跨消息保持，用于面板渲染）
const currentPlan = ref<PlanItem[]>([])

// 根据依赖关系计算每个任务的缩进层级
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

const displayUserName = computed(() => {
  const user = loginUserStore.loginUser
  const name = user?.userName?.trim()
  if (name && name !== '无名') return name
  return user?.userAccount || '创作者'
})

const messages = ref<Message[]>([])
const collapsedReplies = ref<Set<number>>(new Set())
const toggleFinalReply = (index: number) => {
  const s = new Set(collapsedReplies.value)
  if (s.has(index)) { s.delete(index) } else { s.add(index) }
  collapsedReplies.value = s
}
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
  // 收到完全重复或更短前缀，视为无增量
  if (previous === incoming || previous.startsWith(incoming)) {
    return {
      next: previous,
      delta: '',
    }
  }
  // 常见流式增量：新分片刚好接在尾部
  if (incoming.startsWith(previous.slice(-Math.min(previous.length, 64)))) {
    return {
      next: previous + incoming,
      delta: incoming,
    }
  }
  // 兜底：当无法可靠判定为“快照”时，按增量追加，优先保证内容不丢失
  return {
    next: previous + incoming,
    delta: incoming,
  }
}

const sanitizeAssistantContent = (raw: string) => {
  if (!raw) return ''
  const normalizedRaw = raw
    // 兼容部分模型返回的中文引号 JSON 片段
    .replace(/[“”]/g, '"')
    .replace(/[‘’]/g, "'")
    // 将拼接在一起的 JSON 对象拆开，便于逐行过滤
    .replace(/}\s*{/g, '}\n{')
  const lines = normalizedRaw.split('\n')
  const filtered: string[] = []
  let suppressToolResultBlock = false
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue
    // 过滤结构化工具事件 JSON，避免被展示在“最终回复”
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try {
        const obj = JSON.parse(trimmed)
        if (obj && typeof obj.type === 'string' && TOOL_EVENT_TYPES.has(obj.type)) {
          continue
        }
      } catch {
        // ignore JSON parse error
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
  // updatePlan 拦截：解析 items 更新计划面板，不创建 processEvent
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

const startToolEvent = (msg: Message) => {
  const events = ensureProcessEvents(msg)
  events.push({
    id: `${Date.now()}_${events.length}_tool`,
    type: 'tool',
    title: 'tool',
    summary: '工具调用',
    details: '',
    time: formatEventTime(),
  })
  return events.length - 1
}

const appendStreamChunkEvents = (
  msg: Message,
  chunk: string,
  toolState: { activeIndex: number | null; collectingResult: boolean }
) => {
  msg.streamBuffer = (msg.streamBuffer || '') + chunk
  const lines = msg.streamBuffer.split('\n')
  msg.streamBuffer = lines.pop() || ''
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue
    if (trimmed.startsWith('[选择工具]')) {
      // 仅作为中间态提示，不单独展示，避免出现“写入文件”这类无效卡片
      continue
    }
    if (trimmed.startsWith('[工具调用]')) {
      // 每次工具调用都单独生成一条事件，避免多次调用混在同一块中
      toolState.activeIndex = startToolEvent(msg)
      toolState.collectingResult = false
      appendToolDetail(
        msg,
        toolState.activeIndex,
        trimmed.replace(/^\[工具调用\]\s*/, '')
      )
      continue
    }
    if (trimmed.startsWith('[工具结果]')) {
      if (toolState.activeIndex === null) {
        toolState.activeIndex = startToolEvent(msg)
      }
      const events = ensureProcessEvents(msg)
      const current = events[toolState.activeIndex]
      if (current) {
        current.title = 'tool'
      }
      toolState.collectingResult = true
      appendToolDetail(
        msg,
        toolState.activeIndex,
        trimmed.replace(/^\[工具结果\]\s*/, '')
      )
      continue
    }
    if (trimmed.startsWith('[执行结束]')) {
      toolState.activeIndex = null
      toolState.collectingResult = false
      continue
    }
    if (
      toolState.activeIndex !== null &&
      !/^\[(选择工具|工具调用|工具结果|执行结束)\]/.test(trimmed)
    ) {
      appendToolDetail(msg, toolState.activeIndex, line)
    }
  }
}

const flushStreamBufferEvents = (
  msg: Message,
  toolState: { activeIndex: number | null; collectingResult: boolean }
) => {
  const rest = (msg.streamBuffer || '').trim()
  if (rest) {
    appendStreamChunkEvents(msg, `${rest}\n`, toolState)
  }
  msg.streamBuffer = ''
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
    if (res.data.code !== 0 || !res.data.data?.length) return
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

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
            .map((chat) => ({
              type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
              content: chat.messageType === 'assistant' ? sanitizeAssistantContent(chat.message || '') : (chat.message || ''),
              turnId: chat.turnId,
              createTime: chat.createTime,
              processEvents:
                chat.messageType === 'assistant' && chat.reasoningContent
                  ? [
                      {
                        id: `${chat.turnId || chat.createTime || 'history'}_thinking`,
                        type: 'thinking' as const,
                        title: 'thinking',
                        summary: buildEventPreview(chat.reasoningContent, 'thinking'),
                        details: chat.reasoningContent,
                        time: chat.createTime || formatEventTime(),
                      },
                    ]
                  : [],
            }))
            .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
        for (const msg of historyMessages) {
          if (msg.type === 'ai' && msg.turnId) {
            // 优先展示数据库中的完整执行事件
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
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

const refreshVersions = async (selectWorking = false, versionIdsBeforeTurn?: Set<string>) => {
  if (!appId.value || (!isOwner.value && !isAdmin.value)) return
  loadingVersions.value = true
  try {
    const [response, historyResponse] = await Promise.all([
      listAppVersions({ appId: String(appId.value) }),
      listAppDeploymentHistory({ appId: String(appId.value) }),
    ])
    if (response.data.code !== 0) throw new Error(response.data.message || '读取版本列表失败')
    versions.value = response.data.data || []
    deploymentHistory.value = historyResponse.data.code === 0 ? historyResponse.data.data || [] : []
    const currentWorking = versions.value.find(version => version.working)
    const currentDeployed = versions.value.find(version => version.deployed)
    if (appInfo.value) {
      appInfo.value.workingVersionId = currentWorking?.id
      appInfo.value.deployedVersionId = currentDeployed?.id
    }
    if (selectWorking || !previewVersionId.value || !versions.value.some(version => String(version.id) === String(previewVersionId.value))) {
      previewVersionId.value = currentWorking?.id || currentDeployed?.id
    }
    if (selectWorking || !baseVersionId.value || !versions.value.some(version => String(version.id) === String(baseVersionId.value))) {
      baseVersionId.value = currentWorking?.id
    }
    const baseStorageKey = `epiph-code:app:${appId.value}:base-version`
    if (baseVersionId.value) localStorage.setItem(baseStorageKey, baseVersionId.value)
    else localStorage.removeItem(baseStorageKey)
    if (selectWorking) {
      const latestTurnVersion = versions.value.find(version => version.turnId
        && version.versionType === 'GENERATED'
        && (!versionIdsBeforeTurn || !versionIdsBeforeTurn.has(String(version.id))))
      const latestUnlinkedReply = [...messages.value].reverse().find(item => item.type === 'ai' && !item.turnId && !item.loading)
      if (latestTurnVersion?.turnId && latestUnlinkedReply) latestUnlinkedReply.turnId = latestTurnVersion.turnId
    }
  } catch (error) {
    console.error('读取版本列表失败：', error)
    message.error(error instanceof Error ? error.message : '读取版本列表失败')
  } finally {
    loadingVersions.value = false
  }
}

const selectPreviewVersion = async (version: API.AppVersionVO) => {
  if (!version.id) return
  previewVersionId.value = String(version.id)
  await updatePreview(2)
}

const setModificationBase = (version: API.AppVersionVO) => {
  if (!version.id) return
  baseVersionId.value = String(version.id)
  localStorage.setItem(`epiph-code:app:${appId.value}:base-version`, baseVersionId.value)
  message.success(`后续修改将基于 V${version.versionNo}`)
}

const openVersionCompare = (version: API.AppVersionVO) => {
  if (!version.id || versions.value.length < 2) {
    message.info('至少需要两个网页版本才能比较')
    return
  }
  compareLeftVersionId.value = String(version.id)
  compareRightVersionId.value = String(
    (versions.value.find(item => item.working && String(item.id) !== String(version.id))
      || versions.value.find(item => String(item.id) !== String(version.id)))?.id || '',
  )
  compareFiles.value = []
  compareModalOpen.value = true
  void loadVersionCompare()
}

const loadVersionCompare = async () => {
  if (!appId.value || !compareLeftVersionId.value || !compareRightVersionId.value) return
  loadingCompare.value = true
  try {
    const response = await compareAppVersions({
      appId: String(appId.value),
      leftVersionId: compareLeftVersionId.value,
      rightVersionId: compareRightVersionId.value,
    })
    if (response.data.code !== 0) throw new Error(response.data.message || '比较版本失败')
    compareFiles.value = response.data.data?.files || []
  } catch (error) {
    console.error('比较版本失败：', error)
    message.error(error instanceof Error ? error.message : '比较版本失败')
  } finally {
    loadingCompare.value = false
  }
}

const deployVersion = (version: API.AppVersionVO) => {
  if (!version.id || !appId.value) return
  Modal.confirm({
    title: `部署网页版本 V${version.versionNo}？`,
    content: version.userPrompt ? `本次版本：${version.userPrompt.slice(0, 120)}` : '线上网页将切换到此版本。',
    okText: '部署此版本',
    cancelText: '取消',
    onOk: async () => {
      deploying.value = true
      deployingVersionId.value = String(version.id)
      try {
        const response = await deployAppApi({ appId: String(appId.value), versionId: String(version.id) })
        if (response.data.code !== 0 || !response.data.data) throw new Error(response.data.message || '部署失败')
        deployUrl.value = response.data.data
        if (appInfo.value) appInfo.value.deployedVersionId = String(version.id)
        deployModalVisible.value = true
        message.success(`V${version.versionNo} 已部署`)
        await refreshVersions()
      } catch (error) {
        console.error('部署版本失败：', error)
        message.error(error instanceof Error ? error.message : '部署失败，请重试')
      } finally {
        deploying.value = false
        deployingVersionId.value = undefined
      }
    },
  })
}

const deployApp = () => {
  const version = workingVersion.value
  if (!version) {
    message.warning('当前还没有可部署的网页版本')
    return
  }
  deployVersion(version)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id
  baseVersionId.value = localStorage.getItem(`epiph-code:app:${id}:base-version`) || undefined
  isWorkflowLocked.value = isAppWorkflowModeLocked(id)

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      if (!isOwner.value && appInfo.value.deployedVersionId) {
        previewVersionId.value = appInfo.value.deployedVersionId
      }
      await refreshChatMetrics()
      await refreshVersions()

      // 先加载对话历史
      await loadChatHistory()
      // 直接检查产物是否存在，避免有代码但历史记录未完整返回时空白预览。
      await updatePreview()
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
          appInfo.value.initPrompt &&
          isOwner.value &&
          messages.value.length === 0 &&
          historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom(true)

  // 开始生成
  isGenerating.value = true
  const clientRequestId = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
  await generateCode(prompt, aiMessageIndex, clientRequestId)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom(true)

  // 开始生成
  isGenerating.value = true
  const clientRequestId = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
  await generateCode(message, aiMessageIndex, clientRequestId)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number, clientRequestId: string) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false
  const versionIdsBeforeTurn = new Set(versions.value.map(version => String(version.id)))

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
      mode: getCurrentGenerationMode(),
      agent: selectedAgent.value,
      clientRequestId,
    })
    if (baseVersionId.value) params.set('baseVersionId', baseVersionId.value)
    if (appId.value && getCurrentGenerationMode() === 'workflow') {
      lockAppWorkflowMode(appId.value)
      isWorkflowLocked.value = true
    }

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''
    let fullThinking = ''
    const toolState: { activeIndex: number | null; collectingResult: boolean } = {
      activeIndex: null,
      collectingResult: false,
    }

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        if (content === undefined || content === null) {
          return
        }
        // 结构化分片（thinking/tool）
        const structuredChunk = parseStructuredStreamChunk(content)
        if (structuredChunk?.type === 'thinking') {
          const thinkingUpdate = mergeStreamSnapshotOrDelta(fullThinking, structuredChunk.data)
          fullThinking = thinkingUpdate.next
          upsertThinkingEvent(messages.value[aiMessageIndex], fullThinking)
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
          return
        }
        if (
          structuredChunk?.type === 'tool_request' ||
          structuredChunk?.type === 'tool_executed'
        ) {
          appendStructuredToolEvent(messages.value[aiMessageIndex], structuredChunk)
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
          return
        }
        // 普通文本分片（AI响应/工具输出）
        const chunkText = typeof content === 'string' ? content : String(content)
        const contentUpdate = mergeStreamSnapshotOrDelta(fullContent, chunkText)
        fullContent = contentUpdate.next
        // 流式阶段保留原始内容，确保工具调用标记可实时展示
        messages.value[aiMessageIndex].content = fullContent
        if (contentUpdate.delta) {
          appendStreamChunkEvents(messages.value[aiMessageIndex], contentUpdate.delta, toolState)
        }
        messages.value[aiMessageIndex].loading = false
        scrollToBottom()
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', async function () {
      if (streamCompleted) return

      streamCompleted = true
      flushStreamBufferEvents(messages.value[aiMessageIndex], toolState)
      const completedMessage = messages.value[aiMessageIndex]
      completedMessage.content = sanitizeAssistantContent(fullContent)
      if (!completedMessage.content && !completedMessage.processEvents?.length) {
        completedMessage.content = '本轮生成已结束，但没有收到模型文本回复。请查看预览或重试。'
      }
      completedMessage.loading = false
      eventSource?.close()
      void refreshChatMetrics()

      // done 到达时后端已完成版本落库；同步应用信息与工作版本后刷新预览。
      try {
        if (!appInfo.value?.codeGenType && appId.value) {
          const appRes = await getAppVoById({ id: appId.value as unknown as number })
          if (appRes.data?.code === 0 && appRes.data.data) {
            appInfo.value = appRes.data.data
          }
        }
        await refreshVersions(true, versionIdsBeforeTurn)
        await updatePreview(3)
      } finally {
        isGenerating.value = false
      }
    })

    // 处理business-error事件（后端限流等错误）
    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        appendProcessEvent(messages.value[aiMessageIndex], {
          type: 'error',
          title: '业务错误',
          summary: '处理过程中出现业务错误',
          details: errorMessage,
        })
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 避免浏览器 EventSource 自动重连导致同一请求被反复触发
      eventSource?.close()
      handleError(new Error('SSE连接中断'), aiMessageIndex)
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  const errorText = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].content = errorText
  appendProcessEvent(messages.value[aiMessageIndex], {
    type: 'error',
    title: '连接异常',
    summary: '网络连接或流式传输异常',
    details: errorText,
  })
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = async (retries = 0) => {
  if (!appId.value) {
    previewStatus.value = 'missing'
    return
  }

  const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
  const sourceUrl = previewVersionId.value
    ? getStaticVersionPreviewUrl(previewVersionId.value)
    : getStaticPreviewUrl(codeGenType, String(appId.value))
  const hadWorkingPreview = Boolean(previewUrl.value)
  previewStatus.value = 'checking'

  for (let attempt = 0; attempt <= retries; attempt += 1) {
    try {
      const response = await fetch(sourceUrl, { method: 'HEAD', cache: 'no-store', credentials: 'include' })
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
      // 跨域配置可能禁止 HEAD 请求，仍尝试让浏览器直接加载预览页。
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

// 滚动到底部
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

// 下载代码
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
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
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

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
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
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
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
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 可拖拽分栏比例与预览全屏状态
const SPLIT_RATIO_STORAGE_KEY = 'epiph-code:chat-split-ratio'
const DEFAULT_CHAT_RATIO = 42
const mainContentRef = ref<HTMLElement>()
const isResizingSplit = ref(false)
const isPreviewFullscreen = ref(false)
const savedRatio = Number(localStorage.getItem(SPLIT_RATIO_STORAGE_KEY))
const chatPaneRatio = ref<number>(
  Number.isFinite(savedRatio) && savedRatio >= 24 && savedRatio <= 76
    ? savedRatio
    : DEFAULT_CHAT_RATIO,
)

const chatPaneStyle = computed(() => ({
  flex: `0 0 ${chatPaneRatio.value}%`,
}))

const handleSplitMouseMove = (event: MouseEvent) => {
  if (!isResizingSplit.value || !mainContentRef.value) return
  const rect = mainContentRef.value.getBoundingClientRect()
  if (rect.width <= 0) return
  const rawPercent = ((event.clientX - rect.left) / rect.width) * 100
  const clamped = Math.min(76, Math.max(24, rawPercent))
  chatPaneRatio.value = Math.round(clamped * 10) / 10
}

const stopSplitResize = () => {
  if (!isResizingSplit.value) return
  isResizingSplit.value = false
  localStorage.setItem(SPLIT_RATIO_STORAGE_KEY, String(chatPaneRatio.value))
  window.removeEventListener('mousemove', handleSplitMouseMove)
  window.removeEventListener('mouseup', stopSplitResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

const startSplitResize = (event: MouseEvent) => {
  event.preventDefault()
  isResizingSplit.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleSplitMouseMove)
  window.addEventListener('mouseup', stopSplitResize)
}

const resetSplitRatio = () => {
  chatPaneRatio.value = DEFAULT_CHAT_RATIO
  localStorage.setItem(SPLIT_RATIO_STORAGE_KEY, String(DEFAULT_CHAT_RATIO))
}

const togglePreviewFullscreen = () => {
  isPreviewFullscreen.value = !isPreviewFullscreen.value
}

const previewDevice = ref<'desktop' | 'tablet' | 'mobile'>('desktop')

const goBackWorkspace = () => {
  router.push('/')
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  stopSplitResize()
})
</script>

<style scoped>
#appChatPage {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 8px 14px 12px;
  background: #F8F9FA;
  overflow: hidden;
}

/* 顶部 IDE 工作台导航栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 48px;
  padding: 6px 14px;
  margin-bottom: 10px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 14px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.back-workspace-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 11px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  background: #F8FAFC;
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.16s ease;
}

.back-workspace-btn:hover {
  color: #0F172A;
  background: #F1F5F9;
  border-color: #CBD5E1;
}

.header-sep {
  width: 1px;
  height: 18px;
  background: #E2E8F0;
}

.app-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  cursor: pointer;
}

.app-name {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-gen-type-tag {
  font-size: 11.5px;
  font-weight: 600;
  background: #EFF6FF;
  color: #2563EB;
  border-radius: 6px;
  padding: 1px 8px;
  margin: 0;
}

.status-meta-tag {
  font-size: 11.5px;
  border-radius: 6px;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.toolbar-btn {
  height: 32px;
  font-size: 12.5px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.btn-version-pill {
  margin-left: 4px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 700;
  color: #2563EB;
  background: #EFF6FF;
  border-radius: 6px;
}

.deploy-primary-btn {
  height: 32px;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 8px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 0;
  overflow: hidden;
}

.main-content.is-resizing .preview-iframe {
  pointer-events: none;
}

/* 左侧 AI Copilot 对话区域 */
.chat-section {
  min-width: 320px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.chat-pane-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 16px;
  background: #F8FAFC;
  border-bottom: 1px solid #EEF2F6;
  flex-shrink: 0;
}

.chat-pane-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  font-weight: 700;
  color: #0F172A;
}

.copilot-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #20BEFF;
}

.chat-pane-sub {
  font-size: 11.5px;
  color: #64748B;
}

/* 中间可拖拽分栏调节线 */
.split-resizer {
  flex: 0 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: col-resize;
  user-select: none;
  position: relative;
}

.split-resizer-handle {
  width: 4px;
  height: 44px;
  border-radius: 999px;
  background: #CBD5E1;
  transition: background-color 0.16s ease, height 0.16s ease, width 0.16s ease;
}

.split-resizer:hover .split-resizer-handle,
.main-content.is-resizing .split-resizer-handle {
  width: 5px;
  height: 68px;
  background: #2563EB;
}

.messages-container {
  flex: 1;
  min-height: 0;
  padding: 18px 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.load-more-container {
  text-align: center;
  margin-bottom: 14px;
}

.chat-welcome-card {
  padding: 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%);
  border: 1px solid rgba(37, 99, 235, 0.15);
  border-radius: 14px;
}

.welcome-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2563EB;
  background: #FFFFFF;
  border-radius: 999px;
  margin-bottom: 8px;
}

.chat-welcome-card h4 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}

.chat-welcome-card p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: #475569;
}

.message-item {
  margin-bottom: 20px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 10px;
}

.user-message .message-content {
  max-width: 84%;
  padding: 11px 15px;
  border-radius: 16px 4px 16px 16px;
  background: #202124;
  color: #FFFFFF;
  font-size: 13.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.user-msg-avatar {
  background: #202124;
  color: #FFFFFF;
  font-weight: 700;
}

.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.ai-message .message-avatar :deep(.ant-avatar) {
  background: #EFF6FF;
  border: 1px solid rgba(37, 99, 235, 0.16);
  padding: 3px;
}

.ai-message .message-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.ai-msg-meta {
  display: flex;
  align-items: center;
  gap: 7px;
}

.ai-author-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #0F172A;
}

.ai-role-pill {
  padding: 1px 7px;
  font-size: 10px;
  font-weight: 600;
  color: #4F46E5;
  background: #EEF2FF;
  border-radius: 999px;
}

/* 任务计划面板 */
.plan-panel {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 12px 14px;
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.plan-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #0F172A;
}

.plan-progress {
  font-size: 11.5px;
  font-weight: 700;
  color: #2563EB;
  background: #EFF6FF;
  padding: 1px 8px;
  border-radius: 999px;
}

.plan-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.plan-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #334155;
}

.plan-item-completed .plan-item-text {
  color: #94A3B8;
  text-decoration: line-through;
}

.plan-item-active .plan-item-text {
  color: #2563EB;
  font-weight: 600;
}

.plan-item-icon {
  width: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #059669;
  font-size: 12px;
}

.plan-item-spinner {
  width: 10px;
  height: 10px;
  border: 2px solid #DBEAFE;
  border-top-color: #2563EB;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.plan-item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #CBD5E1;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.plan-bar {
  height: 4px;
  background: #E2E8F0;
  border-radius: 999px;
  overflow: hidden;
}

.plan-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563EB, #10B981);
  transition: width 0.3s ease;
}

/* 最终回复 */
.final-reply {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}

.final-reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  background: #F8FAFC;
  border-bottom: 1px solid #EEF2F6;
  cursor: pointer;
  user-select: none;
}

.final-reply-toggle {
  font-size: 10px;
  color: #64748B;
}

.final-reply-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #0F172A;
}

.final-reply-body {
  padding: 14px 16px;
  font-size: 13.5px;
  line-height: 1.65;
  color: #0F172A;
}

.loading-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #EFF6FF;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 500;
  color: #1D4ED8;
}

.turn-version-tag {
  align-self: flex-start;
  cursor: pointer;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 8px;
  background: #ECFDF5;
  color: #059669;
  border: 1px solid rgba(5, 150, 105, 0.2);
}

.selected-element-alert {
  margin: 0 12px 8px;
  border-radius: 12px;
}

.element-header {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-weight: 600;
}

.element-selector-code {
  font-size: 11.5px;
  background: rgba(37, 99, 235, 0.08);
  padding: 1px 6px;
  border-radius: 4px;
}

/* 底部 AI 输入工作区 */
.input-container {
  padding: 10px 12px 12px;
  background: #F8FAFC;
  border-top: 1px solid #EEF2F6;
  flex-shrink: 0;
}

.input-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 16px;
  padding: 8px 12px 10px;
  box-shadow: var(--shadow-xs);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input-wrapper:focus-within {
  border-color: #2563EB;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.input-dock-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 10px;
  padding-bottom: 6px;
  margin-bottom: 6px;
  border-bottom: 1px solid #F1F5F9;
  font-size: 12px;
  color: #475569;
}

.input-agent-bar {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #F8FAFC;
  padding: 1px 6px;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
}

.input-agent-label {
  font-size: 11px;
  color: #64748B;
  font-weight: 600;
}

.chat-metrics-compact {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.metric-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 7px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  font-size: 11px;
  color: #475569;
}

.context-meter {
  display: inline-block;
  width: 38px;
  height: 4px;
  background: #E2E8F0;
  border-radius: 999px;
  overflow: hidden;
}

.context-meter > span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #2563EB, #4F46E5);
  border-radius: 999px;
  transition: width 0.25s ease;
}

.version-base-bar {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #2563EB;
  background: #EFF6FF;
  padding: 1px 8px;
  border-radius: 6px;
}

.version-base-bar :deep(.ant-btn-link) {
  padding: 0 2px;
  height: auto;
  font-size: 11px;
}

.input-wrapper :deep(.ant-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 4px 42px 2px 2px !important;
  font-size: 13.5px;
  line-height: 1.55;
  color: #0F172A;
  resize: none;
}

.input-actions {
  position: absolute;
  right: 10px;
  bottom: 10px;
}

.input-actions :deep(.ant-btn) {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 右侧实时预览沙箱区域 */
.preview-section {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 44px;
  padding: 0 14px;
  border-bottom: 1px solid #EEF2F6;
  background: #F8FAFC;
  flex-shrink: 0;
}

.preview-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.browser-dots {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.b-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.b-dot--red {
  background: #FB7185;
}

.b-dot--amber {
  background: #FBBF24;
}

.b-dot--green {
  background: #34D399;
}

.preview-title-meta {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.preview-heading h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #0F172A;
}

.preview-heading-caption {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748B;
}

.preview-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #FEF3C7;
  font-size: 11px;
  font-weight: 600;
  color: #B45309;
}

.preview-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #F59E0B;
}

.preview-status--ready {
  background: #ECFDF5;
  color: #059669;
}

.preview-status--ready i {
  background: #10B981;
}

/* 中部设备切换与 URL 栏 */
.preview-center-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 440px;
  min-width: 0;
}

.device-switcher {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  background: #E2E8F0;
  border-radius: 8px;
  flex-shrink: 0;
}

.device-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748B;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.device-btn:hover {
  color: #0F172A;
}

.device-btn--active {
  background: #FFFFFF;
  color: #2563EB;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.1);
}

.preview-url-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  height: 28px;
  padding: 0 6px 0 8px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
}

.url-host-badge {
  padding: 1px 5px;
  font-size: 9.5px;
  font-weight: 700;
  color: #2563EB;
  background: #EFF6FF;
  border-radius: 4px;
  flex-shrink: 0;
}

.url-path-text {
  flex: 1;
  font-size: 11.5px;
  color: #64748B;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.url-refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: #64748B;
  cursor: pointer;
  flex-shrink: 0;
}

.url-refresh-btn:hover {
  background: #F1F5F9;
  color: #0F172A;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.preview-tool-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #E2E8F0;
  border-radius: 7px;
  background: #FFFFFF;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preview-tool-pill:hover {
  color: #0F172A;
  border-color: #CBD5E1;
  background: #F8FAFC;
}

.preview-tool-pill--active {
  background: #FEF2F2;
  border-color: #FECACA;
  color: #DC2626;
  font-weight: 600;
}

.preview-content {
  flex: 1;
  min-height: 0;
  position: relative;
  display: flex;
  justify-content: center;
  background-color: #F1F5F9;
  background-image: radial-gradient(#CBD5E1 1px, transparent 1px);
  background-size: 18px 18px;
  overflow: auto;
}

.preview-content--tablet,
.preview-content--mobile {
  padding: 16px;
}

.preview-frame-wrap {
  width: 100%;
  height: 100%;
  position: relative;
  background: #FFFFFF;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.frame-wrap--tablet {
  width: 768px;
  max-width: 100%;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.frame-wrap--mobile {
  width: 390px;
  max-width: 100%;
  border-radius: 24px;
  border: 4px solid #1E293B;
  box-shadow: var(--shadow-xl);
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #FFFFFF;
}

.preview-frame-error,
.preview-loading,
.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #64748B;
  text-align: center;
  padding: 24px;
}

.preview-placeholder h4 {
  margin: 4px 0 0;
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
}

.placeholder-icon {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: #FFFFFF;
  border: 1px solid rgba(37, 99, 235, 0.18);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #2563EB;
}

/* 版本抽屉与对比弹窗 */
.version-drawer-intro {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  font-size: 12.5px;
  color: #64748B;
}

.version-current-base {
  margin-bottom: 14px;
  border-radius: 10px;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-item {
  padding: 14px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  transition: border-color 0.16s ease;
}

.version-item:hover {
  border-color: #CBD5E1;
}

.version-item-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.version-item-time {
  font-size: 12px;
  color: #64748B;
  margin-bottom: 8px;
}

.version-item-prompt {
  padding: 8px 10px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  font-size: 12.5px;
  color: #0F172A;
  margin-bottom: 8px;
}

.version-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 11.5px;
  color: #64748B;
  margin-bottom: 10px;
}

.version-item-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.version-compare-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.version-diff {
  background: #0F172A;
  color: #E2E8F0;
  padding: 12px;
  border-radius: 10px;
  font-size: 12px;
  max-height: 380px;
  overflow: auto;
}

.deployment-history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.deployment-history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #64748B;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .preview-center-bar {
    display: none;
  }
}

@media (max-width: 960px) {
  .main-content {
    flex-direction: column;
  }
  .chat-section {
    flex: 1;
    max-width: 100%;
    min-width: 0;
  }
}
</style>
