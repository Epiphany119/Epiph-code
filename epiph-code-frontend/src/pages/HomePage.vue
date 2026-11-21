<template>
  <div id="homePage">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="hero-pill">
          <span class="hero-pill-dot"></span>
          <span class="hero-pill-text">AI 驱动的全链路应用工作台</span>
          <span class="hero-pill-badge">v2.0</span>
        </div>

        <h1 class="hero-title">
          把灵感，
          <span class="hero-gradient-title">做成真正可运行的应用</span>
        </h1>

        <p class="hero-subtitle">
          从一句自然语言开始，完成结构化任务规划、代码生成、实时预览与一键部署。<br />
          让 AI 成为你的全栈工程师搭档，把创意极速变为现实。
        </p>

        <!-- Feature Capabilities -->
        <div class="hero-capabilities">
          <div class="capability-item">
            <ThunderboltOutlined class="capability-icon" />
            <span>自然语言秒级构建</span>
          </div>
          <div class="capability-item">
            <FireOutlined class="capability-icon" />
            <span>Agent 任务计划推进</span>
          </div>
          <div class="capability-item">
            <EyeOutlined class="capability-icon" />
            <span>实时运行与可视化编辑</span>
          </div>
          <div class="capability-item">
            <CloudUploadOutlined class="capability-icon" />
            <span>多版本管理与一键上线</span>
          </div>
        </div>
      </div>

      <!-- Prompt Console Card -->
      <div id="projectPrompt" class="prompt-studio-card">
        <div class="studio-card-header">
          <div class="studio-header-title">
            <span class="studio-header-badge">01</span>
            <h3>描述你的应用需求</h3>
          </div>
          <div class="studio-header-controls">
            <span class="agent-label">生成模型:</span>
            <a-select
              v-model:value="selectedAgent"
              class="agent-select"
              size="small"
              :disabled="creating"
            >
              <a-select-option value="default">默认 Agent (基础模式)</a-select-option>
              <a-select-option value="deepseek">DeepSeek Flash (长链路推理)</a-select-option>
            </a-select>
          </div>
        </div>

        <div class="studio-card-body">
          <a-textarea
            v-model:value="userPrompt"
            placeholder="例如：帮我创建一个现代化响应式的个人博客网站，包含文章列表、分类标签、搜索框、暗色模式切换和个人简介页面……"
            :rows="5"
            :maxlength="1000"
            class="studio-prompt-input"
            :disabled="creating"
          />

          <!-- Quick Suggestions -->
          <div class="inspiration-box">
            <span class="inspiration-label">灵感预设:</span>
            <div class="inspiration-chips">
              <button
                v-for="item in promptIdeas"
                :key="item.label"
                type="button"
                class="idea-chip"
                @click="setPrompt(item.prompt)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
        </div>

        <div class="studio-card-footer">
          <div class="prompt-length-hint">
            <span :class="{ 'text-warn': userPrompt.length > 900 }">{{ userPrompt.length }}</span>
            <span class="dim">/ 1000 字 · 描述越具体，生成效果越出色</span>
          </div>
          <a-button
            type="primary"
            size="large"
            class="start-generate-btn"
            :loading="creating"
            @click="createApp"
          >
            <template #icon>
              <RocketOutlined />
            </template>
            <span>{{ creating ? '正在创建并构思...' : '立即开始生成' }}</span>
          </a-button>
        </div>
      </div>
    </section>

    <!-- Workspace Section (My Apps) -->
    <section class="section-container">
      <div class="section-header">
        <div>
          <div class="section-badge">
            <FolderOpenOutlined />
            <span>MY WORKSPACE</span>
          </div>
          <h2 class="section-title">我的作品</h2>
          <p class="section-desc">管理你创建的应用，继续对话迭代或查看线上版本。</p>
        </div>
        <div class="section-extra">
          <span class="stat-pill">{{ myAppsPage.total }} 个应用</span>
        </div>
      </div>

      <!-- Not Logged In State -->
      <div v-if="!loginUserStore.loginUser.id" class="login-prompt-card">
        <div class="login-prompt-content">
          <h4>登录以查看你的专属工作台</h4>
          <p>登录后可永久保存对话历史、多版本迭代记录并随时管理线上站点。</p>
        </div>
        <a-button type="primary" @click="goToLogin">立即登录 / 注册</a-button>
      </div>

      <!-- App Grid -->
      <div v-else-if="myApps.length" class="app-grid">
        <AppCard
          v-for="app in myApps"
          :key="app.id"
          :app="app"
          @view-chat="viewChat"
          @view-work="viewWork"
          @delete-app="handleDeleteApp"
        />
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state-card">
        <FolderOpenOutlined class="empty-icon" />
        <h3>暂无作品</h3>
        <p>在上方控制台输入提示词，即可快速创建并运行你的第一个 AI 应用。</p>
        <a-button type="primary" ghost @click="scrollToPrompt">
          <template #icon><PlusOutlined /></template>
          立即创建新应用
        </a-button>
      </div>

      <!-- Pagination -->
      <div v-if="myApps.length && myAppsPage.total > myAppsPage.pageSize" class="pagination-wrap">
        <a-pagination
          v-model:current="myAppsPage.current"
          :page-size="myAppsPage.pageSize"
          :total="myAppsPage.total"
          show-less-items
          @change="loadMyApps"
        />
      </div>
    </section>

    <!-- Featured Gallery Section -->
    <section class="section-container">
      <div class="section-header">
        <div>
          <div class="section-badge">
            <CompassOutlined />
            <span>COMMUNITY GALLERY</span>
          </div>
          <h2 class="section-title">精选应用展示</h2>
          <p class="section-desc">探索社区创作者通过 Epiph-code 生成的精美站点与实用工具。</p>
        </div>
        <div class="section-extra">
          <span class="stat-pill">{{ featuredAppsPage.total }} 个精选</span>
        </div>
      </div>

      <div v-if="featuredApps.length" class="app-grid">
        <AppCard
          v-for="app in featuredApps"
          :key="app.id"
          :app="app"
          :featured="true"
          @view-chat="viewChat"
          @view-work="viewWork"
        />
      </div>

      <div v-else class="empty-state-card">
        <CompassOutlined class="empty-icon" />
        <h3>暂无精选应用</h3>
        <p>精选应用正在持续评选并收录中，敬请期待。</p>
      </div>

      <div v-if="featuredApps.length && featuredAppsPage.total > featuredAppsPage.pageSize" class="pagination-wrap">
        <a-pagination
          v-model:current="featuredAppsPage.current"
          :page-size="featuredAppsPage.pageSize"
          :total="featuredAppsPage.total"
          show-less-items
          @change="loadFeaturedApps"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, deleteApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'
import {
  RocketOutlined,
  ThunderboltOutlined,
  FireOutlined,
  EyeOutlined,
  CloudUploadOutlined,
  FolderOpenOutlined,
  CompassOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const userPrompt = ref('')
const selectedAgent = ref('default')
const creating = ref(false)

const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

const promptIdeas = [
  {
    label: '个人博客',
    prompt:
      '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、暗黑模式切换和个人简介页面。采用简洁高端设计，支持响应式布局。',
  },
  {
    label: '企业官网',
    prompt:
      '设计一个专业的 SaaS 产品企业官网，包含产品优势、功能矩阵、客户案例、价格方案、常见问题和在线咨询入口，科技感强，排版大气。',
  },
  {
    label: '在线商城',
    prompt:
      '构建一个现代风格的在线数码商城，包含热门商品网格、类目侧边栏、购物车抽屉、用户结算面板和优惠券领取模块，交互顺畅。',
  },
  {
    label: '作品集画廊',
    prompt:
      '制作一个摄影与设计作品展示网站，采用瀑布流画廊布局，支持大图沉浸式预览、项目分类筛选以及个人履历，视觉质感极高。',
  },
]

const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入你想创建的应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录后再创建应用')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({ initPrompt: userPrompt.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功，正在进入工作台...')
      const appId = String(res.data.data)
      await router.push({
        path: `/app/chat/${appId}`,
        query: { agent: selectedAgent.value },
      })
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

const handleDeleteApp = async (appId: string | number | undefined) => {
  if (!appId) return
  try {
    const res = await deleteApp({ id: appId as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      message.success('已删除（数据将在 30 天后彻底清理）')
      await loadMyApps()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('删除请求异常，请稍后重试')
  }
}

const scrollToPrompt = () => {
  document.getElementById('projectPrompt')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const goToLogin = async () => {
  await router.push('/user/login')
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<style scoped>
#homePage {
  display: flex;
  flex-direction: column;
  gap: 64px;
}

/* Hero Section */
.hero-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 0 10px;
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 10px;
  background: #E8F0FE;
  border: 1px solid #D3E3FD;
  border-radius: 999px;
  margin-bottom: 24px;
}

.hero-pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0B57D0;
  box-shadow: none;
}

.hero-pill-text {
  font-size: 13px;
  font-weight: 600;
  color: #0842A0;
}

.hero-pill-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  background: #0B57D0;
  color: #FFFFFF;
  border-radius: 999px;
}

.hero-title {
  margin: 0;
  font-size: clamp(34px, 4.5vw, 54px);
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: -0.04em;
  color: #1F1F1F;
}

.hero-gradient-title {
  background: linear-gradient(135deg, #0B57D0 0%, #4285F4 55%, #0842A0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  max-width: 680px;
  margin: 18px auto 0;
  font-size: 16px;
  line-height: 1.65;
  color: #444746;
}

.hero-capabilities {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 28px;
}

.capability-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: #1F1F1F;
  box-shadow: none;
}

.capability-icon {
  color: #0B57D0;
  font-size: 14px;
}

/* Prompt Studio Card */
.prompt-studio-card {
  width: min(840px, 100%);
  margin-top: 40px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 28px;
  box-shadow: none;
  overflow: hidden;
  text-align: left;
  transition: box-shadow 0.2s ease;
}

.prompt-studio-card:focus-within {
  box-shadow: 0 0 0 2px rgba(11, 87, 208, 0.14);
  border-color: #0B57D0;
}

.studio-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #F0F4F9;
  border-bottom: 1px solid #E3E3E3;
}

.studio-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.studio-header-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #0B57D0;
  color: #FFFFFF;
  font-size: 11px;
  font-weight: 700;
}

.studio-header-title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1F1F1F;
}

.studio-header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-label {
  font-size: 12px;
  color: #444746;
}

.agent-select {
  min-width: 200px;
}

.studio-card-body {
  padding: 20px;
}

.studio-prompt-input {
  font-size: 15px;
  line-height: 1.6;
  border: 1px solid transparent !important;
  background: transparent !important;
  box-shadow: none !important;
  resize: none;
  padding: 0;
}

.studio-prompt-input:focus {
  box-shadow: none !important;
}

.inspiration-box {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #E3E3E3;
}

.inspiration-label {
  font-size: 12px;
  font-weight: 600;
  color: #444746;
}

.inspiration-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.idea-chip {
  padding: 4px 10px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  color: #444746;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.idea-chip:hover {
  background: #E8F0FE;
  border-color: #0B57D0;
  color: #0842A0;
  transform: translateY(-1px);
}

.studio-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #F0F4F9;
  border-top: 1px solid #E3E3E3;
}

.prompt-length-hint {
  font-size: 12px;
  color: #444746;
}

.prompt-length-hint .dim {
  color: #747775;
  margin-left: 4px;
}

.prompt-length-hint .text-warn {
  color: #EF4444;
  font-weight: 600;
}

.start-generate-btn {
  height: 44px;
  padding: 0 24px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
}

/* Sections */
.section-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #E6ECF5;
}

.section-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #0B57D0;
  letter-spacing: 0.06em;
  margin-bottom: 6px;
}

.section-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1F1F1F;
  letter-spacing: -0.02em;
}

.section-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: #444746;
}

.stat-pill {
  padding: 4px 12px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #444746;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.login-prompt-card,
.empty-state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px 24px;
  background: #FFFFFF;
  border: 1px dashed #DADCE0;
  border-radius: 20px;
  text-align: center;
}

.login-prompt-content h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1F1F1F;
}

.login-prompt-content p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #444746;
}

.empty-icon {
  font-size: 36px;
  color: #747775;
  margin-bottom: 4px;
}

.empty-state-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1F1F1F;
}

.empty-state-card p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #444746;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .hero-section {
    padding: 10px 0;
  }

  .studio-header-controls {
    display: none;
  }

  .studio-card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .start-generate-btn {
    width: 100%;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
