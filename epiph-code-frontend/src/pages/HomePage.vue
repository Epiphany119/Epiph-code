<template>
  <div id="homePage">
    <!-- Hero & Flagship AI Composer Section -->
    <section class="hero-section">
      <div class="hero-bg-grid" aria-hidden="true"></div>
      <div class="hero-glow hero-glow--left" aria-hidden="true"></div>
      <div class="hero-glow hero-glow--right" aria-hidden="true"></div>

      <div class="hero-content">
        <div class="hero-pill">
          <span class="hero-pill-dot"></span>
          <span class="hero-pill-text">Epiph-code Studio · 双引擎 AI 全栈构建平台</span>
          <span class="hero-pill-badge">v2.0</span>
        </div>

        <h1 class="hero-title">
          一句话灵感，构建
          <span class="hero-gradient-title">可运行、可部署的真实应用</span>
        </h1>

        <p class="hero-subtitle">
          融合结构化 TODO 规划、长对话三层上下文记忆、可视化点选编辑与不可变版本快照。<br />
          从一句自然语言开始，让 AI 全栈工程师为你完成编码、预览与公网发布。
        </p>

        <!-- Feature Capabilities -->
        <div class="hero-capabilities">
          <div class="capability-item">
            <ThunderboltOutlined class="capability-icon" />
            <span>HTML / 多文件 / Vue 3 架构</span>
          </div>
          <div class="capability-item">
            <FireOutlined class="capability-icon" />
            <span>TODO 任务分解与透明工具流</span>
          </div>
          <div class="capability-item">
            <EyeOutlined class="capability-icon" />
            <span>实时沙箱预览与可视化点选微调</span>
          </div>
          <div class="capability-item">
            <CloudUploadOutlined class="capability-icon" />
            <span>多版本 Diff 对比与一键部署</span>
          </div>
        </div>
      </div>

      <!-- Flagship AI Prompt Composer Card -->
      <div id="projectPrompt" class="prompt-studio-shell">
        <div class="prompt-studio-card">
          <div class="studio-card-header">
            <div class="studio-header-title">
              <span class="studio-header-dot"></span>
              <h3>AI 应用构建台</h3>
              <span class="studio-header-sub">Prompt Composer</span>
            </div>
            <div class="studio-header-controls">
              <span class="agent-label">推理模型</span>
              <a-select
                v-model:value="selectedAgent"
                class="agent-select"
                size="small"
                :disabled="creating"
              >
                <a-select-option value="default">默认 Agent · 极速构建</a-select-option>
                <a-select-option value="deepseek">DeepSeek Flash · 深度推理</a-select-option>
              </a-select>
            </div>
          </div>

          <div class="studio-card-body">
            <a-textarea
              v-model:value="userPrompt"
              placeholder="描述你想要构建的网站或应用：功能模块、视觉风格、目标受众或交互细节……（按 Ctrl/⌘ + Enter 快速生成）"
              :rows="4"
              :maxlength="1000"
              class="studio-prompt-input"
              :disabled="creating"
              @keydown.meta.enter.prevent="createApp"
              @keydown.ctrl.enter.prevent="createApp"
            />

            <!-- Quick Inspiration Templates -->
            <div class="inspiration-box">
              <span class="inspiration-label">精选模板预设：</span>
              <div class="inspiration-chips">
                <button
                  v-for="item in promptIdeas"
                  :key="item.label"
                  type="button"
                  class="idea-chip"
                  @click="setPrompt(item.prompt)"
                >
                  <span class="idea-chip-tag">{{ item.category }}</span>
                  <span>{{ item.label }}</span>
                </button>
              </div>
            </div>
          </div>

          <div class="studio-card-footer">
            <div class="footer-left-meta">
              <div class="prompt-length-hint">
                <span :class="{ 'text-warn': userPrompt.length > 900 }">{{ userPrompt.length }}</span>
                <span class="dim">/ 1000 字</span>
              </div>
              <span class="meta-sep">·</span>
              <span class="shortcut-hint">
                <kbd>⌘</kbd> + <kbd>Enter</kbd> 快速启动
              </span>
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
              <span>{{ creating ? '正在初始化工程...' : '立即开始构建' }}</span>
            </a-button>
          </div>
        </div>
      </div>
    </section>

    <!-- Workspace Section (My Apps) -->
    <section class="section-container">
      <div class="section-header">
        <div class="section-title-group">
          <div class="section-badge">
            <FolderOpenOutlined />
            <span>MY WORKSPACE</span>
          </div>
          <div class="section-heading-row">
            <h2 class="section-title">我的应用工作区</h2>
            <span class="stat-pill">{{ myAppsPage.total }} 个项目</span>
          </div>
          <p class="section-desc">管理你创建的全部应用，随时恢复对话迭代、对比历史版本或发布上线。</p>
        </div>

        <div v-if="loginUserStore.loginUser.id" class="section-toolbar">
          <div class="arch-filter-pills" role="tablist" aria-label="按架构筛选我的作品">
            <button
              v-for="tab in archFilterTabs"
              :key="tab.value"
              type="button"
              class="arch-pill-btn"
              :class="{ 'arch-pill-btn--active': myAppsFilter.codeGenType === tab.value }"
              @click="setMyAppsArchFilter(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
          <a-input
            v-model:value="myAppsFilter.appName"
            placeholder="搜索我的应用..."
            allow-clear
            class="section-search-input"
            @pressEnter="triggerMyAppsSearch"
            @change="onMyAppsSearchClear"
          >
            <template #prefix>
              <SearchOutlined class="search-prefix-icon" />
            </template>
          </a-input>
        </div>
      </div>

      <!-- Not Logged In State -->
      <div v-if="!loginUserStore.loginUser.id" class="login-prompt-card">
        <div class="login-prompt-badge">WORKSPACE LOCKED</div>
        <div class="login-prompt-content">
          <h4>登录以开启你的专属 AI 应用工作台</h4>
          <p>登录后即可永久保存 Agent 对话历史、多版本快照与 Token 用量统计，并支持一键部署公网站点。</p>
        </div>
        <a-button type="primary" size="large" @click="goToLogin">立即登录 / 免费注册</a-button>
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
          @toggle-featured="handleToggleFeatured"
        />
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state-card">
        <div class="empty-icon-circle">
          <FolderOpenOutlined class="empty-icon" />
        </div>
        <h3>{{ hasMyAppsFilter ? '没有找到匹配的应用' : '还没有创建任何应用' }}</h3>
        <p>
          {{
            hasMyAppsFilter
              ? '尝试切换架构分类或清空搜索关键词以查看全部作品。'
              : '在上方 AI 应用构建台输入一句描述，即可自动规划并生成你的第一个网站。'
          }}
        </p>
        <a-button v-if="hasMyAppsFilter" @click="resetMyAppsFilter">重置筛选条件</a-button>
        <a-button v-else type="primary" ghost @click="scrollToPrompt">
          <template #icon><PlusOutlined /></template>
          立即创建第一个应用
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
        <div class="section-title-group">
          <div class="section-badge section-badge--indigo">
            <CompassOutlined />
            <span>COMMUNITY SHOWCASE</span>
          </div>
          <div class="section-heading-row">
            <h2 class="section-title">精选应用画廊</h2>
            <span class="stat-pill">{{ featuredAppsPage.total }} 个精选</span>
          </div>
          <p class="section-desc">探索社区创作者通过 Epiph-code 生成的交互站点、效率工具与完整 Vue 工程。</p>
        </div>

        <div class="section-toolbar">
          <div class="arch-filter-pills" role="tablist" aria-label="按架构筛选精选应用">
            <button
              v-for="tab in archFilterTabs"
              :key="tab.value"
              type="button"
              class="arch-pill-btn"
              :class="{ 'arch-pill-btn--active': featuredFilter.codeGenType === tab.value }"
              @click="setFeaturedArchFilter(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
          <a-input
            v-model:value="featuredFilter.appName"
            placeholder="搜索精选作品..."
            allow-clear
            class="section-search-input"
            @pressEnter="triggerFeaturedSearch"
            @change="onFeaturedSearchClear"
          >
            <template #prefix>
              <SearchOutlined class="search-prefix-icon" />
            </template>
          </a-input>
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
        <div class="empty-icon-circle">
          <CompassOutlined class="empty-icon" />
        </div>
        <h3>{{ hasFeaturedFilter ? '未找到匹配的精选作品' : '暂无精选应用' }}</h3>
        <p>
          {{
            hasFeaturedFilter
              ? '尝试切换筛选标签或清空搜索词查看更多社区作品。'
              : '你可以在「我的应用工作区」点击卡片右下角的星标按钮，将优秀作品推荐至精选画廊。'
          }}
        </p>
        <a-button v-if="hasFeaturedFilter" @click="resetFeaturedFilter">查看全部精选</a-button>
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
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, deleteApp, listGoodAppVoByPage, listMyAppVoByPage, toggleAppFeatured } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
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
  SearchOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const userPrompt = ref('')
const selectedAgent = ref('default')
const creating = ref(false)

const archFilterTabs = [
  { label: '全部', value: '' },
  { label: 'HTML', value: CodeGenTypeEnum.HTML },
  { label: '多文件', value: CodeGenTypeEnum.MULTI_FILE },
  { label: 'Vue 工程', value: CodeGenTypeEnum.VUE_PROJECT },
]

const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})
const myAppsFilter = reactive({
  appName: '',
  codeGenType: '',
})

const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})
const featuredFilter = reactive({
  appName: '',
  codeGenType: '',
})

const hasMyAppsFilter = computed(() => Boolean(myAppsFilter.appName.trim() || myAppsFilter.codeGenType))
const hasFeaturedFilter = computed(() => Boolean(featuredFilter.appName.trim() || featuredFilter.codeGenType))

const promptIdeas = [
  {
    category: '内容博客',
    label: '现代极客博客',
    prompt:
      '创建一个现代化的个人技术博客网站，包含精选文章瀑布流、标签分类过滤、即时搜索框、代码块高亮展示、阅读进度条、暗色模式切换和作者名片。采用清爽现代排版，支持响应式布局。',
  },
  {
    category: 'SaaS 落地页',
    label: 'AI 产品官网',
    prompt:
      '设计一个极具高级感的 AI SaaS 产品官网，包含动态渐变 Hero 首屏、核心功能 Bento Grid 矩阵、交互式价格方案对比表（支持月付/年付切换）、客户证言轮播以及常见问题折叠面板。',
  },
  {
    category: '电商零售',
    label: '潮玩数码商城',
    prompt:
      '构建一个现代风格的数码潮玩商城，包含顶部促销横幅、分类筛选栏、商品卡片网格（支持快速预览与规格选择）、右侧滑出购物车抽屉、实时优惠计算与模拟结算弹窗。',
  },
  {
    category: '创意展示',
    label: '设计师作品集',
    prompt:
      '制作一个视觉设计师与摄影师作品集网站，采用沉浸式画廊布局，支持按作品类型筛选、点击大图灯箱预览、个人技能雷达/时间轴履历以及合作联系表单。',
  },
  {
    category: '数据看板',
    label: '运营分析工作台',
    prompt:
      '开发一个企业级 SaaS 运营数据分析仪表盘，包含核心 KPI 指标卡片（带环比增长徽章）、营收趋势折线图、渠道转化分布、最近订单状态表格以及时间区间筛选器。',
  },
  {
    category: '效率工具',
    label: '番茄钟与任务板',
    prompt:
      '构建一个沉浸式专注生产力工具，左侧为环形倒计时番茄钟（支持专注/短休/长休模式切换与音效提示），右侧为四象限待办任务看板（支持新增、完成勾选、优先级标签与专注统计）。',
  },
]

const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

const createApp = async () => {
  if (creating.value) return
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
      appName: myAppsFilter.appName.trim() || undefined,
      codeGenType: myAppsFilter.codeGenType || undefined,
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
      appName: featuredFilter.appName.trim() || undefined,
      codeGenType: featuredFilter.codeGenType || undefined,
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

const setMyAppsArchFilter = (codeGenType: string) => {
  myAppsFilter.codeGenType = codeGenType
  myAppsPage.current = 1
  loadMyApps()
}

const triggerMyAppsSearch = () => {
  myAppsPage.current = 1
  loadMyApps()
}

const onMyAppsSearchClear = () => {
  if (!myAppsFilter.appName) {
    myAppsPage.current = 1
    loadMyApps()
  }
}

const resetMyAppsFilter = () => {
  myAppsFilter.appName = ''
  myAppsFilter.codeGenType = ''
  myAppsPage.current = 1
  loadMyApps()
}

const setFeaturedArchFilter = (codeGenType: string) => {
  featuredFilter.codeGenType = codeGenType
  featuredAppsPage.current = 1
  loadFeaturedApps()
}

const triggerFeaturedSearch = () => {
  featuredAppsPage.current = 1
  loadFeaturedApps()
}

const onFeaturedSearchClear = () => {
  if (!featuredFilter.appName) {
    featuredAppsPage.current = 1
    loadFeaturedApps()
  }
}

const resetFeaturedFilter = () => {
  featuredFilter.appName = ''
  featuredFilter.codeGenType = ''
  featuredAppsPage.current = 1
  loadFeaturedApps()
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
      message.success('已移入回收站（数据将在 30 天后彻底清理）')
      await Promise.all([loadMyApps(), loadFeaturedApps()])
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('删除请求异常，请稍后重试')
  }
}

const handleToggleFeatured = async (app: API.AppVO) => {
  if (!app.id) return
  const nextFeatured = (app.priority ?? 0) < 99
  try {
    const res = await toggleAppFeatured({
      id: app.id,
      featured: nextFeatured,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success(nextFeatured ? '已推荐至精选应用画廊' : '已从精选应用画廊移除')
      await Promise.all([loadMyApps(), loadFeaturedApps()])
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch {
    message.error('设置精选失败，请稍后重试')
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
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 36px 24px 32px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.65) 100%);
  border: 1px solid rgba(15, 23, 42, 0.07);
  box-shadow: 0 4px 24px -6px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.hero-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(15, 23, 42, 0.035) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(15, 23, 42, 0.035) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: radial-gradient(ellipse 80% 70% at 50% 25%, #000 35%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 80% 70% at 50% 25%, #000 35%, transparent 100%);
  pointer-events: none;
}

.hero-glow {
  position: absolute;
  width: 440px;
  height: 440px;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
  opacity: 0.55;
}

.hero-glow--left {
  top: -180px;
  left: -80px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.18) 0%, transparent 70%);
}

.hero-glow--right {
  top: -160px;
  right: -80px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18) 0%, transparent 70%);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 820px;
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 10px;
  background: #FFFFFF;
  border: 1px solid rgba(37, 99, 235, 0.22);
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
  margin-bottom: 22px;
}

.hero-pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2563EB;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.16);
}

.hero-pill-text {
  font-size: 12.5px;
  font-weight: 600;
  color: #1E3A8A;
}

.hero-pill-badge {
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 7px;
  background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
  color: #FFFFFF;
  border-radius: 999px;
}

.hero-title {
  margin: 0;
  font-size: clamp(32px, 4.2vw, 50px);
  font-weight: 800;
  line-height: 1.16;
  letter-spacing: -0.035em;
  color: #0F172A;
}

.hero-gradient-title {
  background: linear-gradient(135deg, #2563EB 0%, #4F46E5 50%, #0EA5E9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  max-width: 680px;
  margin: 16px auto 0;
  font-size: 15.5px;
  line-height: 1.7;
  color: #475569;
}

.hero-capabilities {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}

.capability-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 13px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 500;
  color: #334155;
  box-shadow: var(--shadow-xs);
}

.capability-icon {
  color: #2563EB;
  font-size: 13px;
}

/* Flagship Prompt Composer */
.prompt-studio-shell {
  position: relative;
  z-index: 1;
  width: min(880px, 100%);
  margin-top: 32px;
  padding: 3px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.22), rgba(79, 70, 229, 0.18), rgba(14, 165, 233, 0.2));
  box-shadow: 0 18px 44px -12px rgba(15, 23, 42, 0.1), 0 4px 16px -4px rgba(37, 99, 235, 0.08);
  transition: all 0.25s ease;
}

.prompt-studio-shell:focus-within {
  background: linear-gradient(135deg, #2563EB, #4F46E5, #0EA5E9);
  box-shadow: 0 22px 50px -12px rgba(37, 99, 235, 0.22), 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.prompt-studio-card {
  background: #FFFFFF;
  border-radius: 21px;
  overflow: hidden;
  text-align: left;
}

.studio-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 20px;
  background: #F8FAFC;
  border-bottom: 1px solid #EEF2F6;
}

.studio-header-title {
  display: flex;
  align-items: center;
  gap: 9px;
}

.studio-header-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563EB, #4F46E5);
}

.studio-header-title h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}

.studio-header-sub {
  font-size: 11px;
  font-weight: 600;
  color: #64748B;
  padding: 1px 7px;
  background: #E2E8F0;
  border-radius: 6px;
}

.studio-header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-label {
  font-size: 12px;
  font-weight: 500;
  color: #64748B;
}

.agent-select {
  min-width: 210px;
}

.studio-card-body {
  padding: 18px 20px 14px;
}

.studio-prompt-input {
  font-size: 15px;
  line-height: 1.65;
  color: #0F172A;
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
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #E2E8F0;
}

.inspiration-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748B;
}

.inspiration-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.idea-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 11px 5px 7px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 999px;
  color: #334155;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.16s ease;
}

.idea-chip-tag {
  padding: 1px 6px;
  font-size: 10.5px;
  font-weight: 600;
  color: #2563EB;
  background: #EFF6FF;
  border-radius: 999px;
}

.idea-chip:hover {
  background: #EFF6FF;
  border-color: #93C5FD;
  color: #1D4ED8;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.1);
}

.studio-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #F8FAFC;
  border-top: 1px solid #EEF2F6;
}

.footer-left-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #64748B;
}

.prompt-length-hint .dim {
  color: #94A3B8;
  margin-left: 3px;
}

.prompt-length-hint .text-warn {
  color: #DC2626;
  font-weight: 700;
}

.meta-sep {
  color: #CBD5E1;
}

.shortcut-hint kbd {
  display: inline-block;
  padding: 1px 5px;
  font-size: 11px;
  font-family: inherit;
  color: #475569;
  background: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.08);
}

.start-generate-btn {
  height: 42px;
  padding: 0 22px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

/* Sections */
.section-container {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.section-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #2563EB;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}

.section-badge--indigo {
  color: #4F46E5;
}

.section-heading-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  margin: 0;
  font-size: 23px;
  font-weight: 700;
  color: #0F172A;
  letter-spacing: -0.025em;
}

.section-desc {
  margin: 5px 0 0;
  font-size: 13.5px;
  color: #64748B;
}

.stat-pill {
  padding: 2px 10px;
  background: #EFF6FF;
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #2563EB;
}

.section-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.arch-filter-pills {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px;
  background: #F1F5F9;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 10px;
}

.arch-pill-btn {
  padding: 5px 12px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #475569;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.arch-pill-btn:hover {
  color: #0F172A;
}

.arch-pill-btn--active {
  background: #FFFFFF;
  color: #2563EB;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.section-search-input {
  width: 210px;
}

.search-prefix-icon {
  color: #94A3B8;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.login-prompt-card,
.empty-state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 52px 24px;
  background: #FFFFFF;
  border: 1px dashed #CBD5E1;
  border-radius: 20px;
  text-align: center;
  box-shadow: var(--shadow-xs);
}

.login-prompt-badge {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2563EB;
  background: #EFF6FF;
  border-radius: 999px;
}

.login-prompt-content h4 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #0F172A;
}

.login-prompt-content p {
  margin: 6px 0 4px;
  font-size: 13.5px;
  color: #64748B;
  max-width: 500px;
}

.empty-icon-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: #EFF6FF;
  color: #2563EB;
  margin-bottom: 4px;
}

.empty-icon {
  font-size: 26px;
}

.empty-state-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;
}

.empty-state-card p {
  margin: 0 0 8px;
  font-size: 13.5px;
  color: #64748B;
  max-width: 440px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .hero-section {
    padding: 24px 16px;
  }

  .studio-header-controls,
  .shortcut-hint,
  .meta-sep {
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

  .section-toolbar {
    width: 100%;
  }

  .section-search-input {
    width: 100%;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
