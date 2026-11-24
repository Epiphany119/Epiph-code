<template>
  <div id="homePage">
    <!-- Kaggle-Style Editorial Split Hero -->
    <section class="kaggle-hero">
      <div class="hero-left">
        <h1 class="hero-title">
          用自然语言，构建<br />真正可运行的 AI 应用
        </h1>
        <p class="hero-subtitle">
          从一句话灵感出发，Epiph-code Studio 自动完成 TODO 任务规划、多架构工程编码、实时沙箱预览与公网一键部署。
        </p>

        <div class="hero-cta-row">
          <a-button type="primary" size="large" class="cta-black-pill" @click="scrollToPrompt">
            <template #icon><RocketOutlined /></template>
            开始构建应用
          </a-button>
          <a-button type="default" size="large" class="cta-outline-pill" @click="scrollToGallery">
            浏览精选画廊
          </a-button>
        </div>

        <!-- Kaggle-Style 3-Column Bold Stat Strip -->
        <div class="hero-stats">
          <div class="hero-stat-item">
            <span class="stat-num">3<span class="stat-unit">种</span></span>
            <span class="stat-label">HTML · 多文件 · Vue 3 工程</span>
          </div>
          <div class="hero-stat-item">
            <span class="stat-num">100<span class="stat-unit">%</span></span>
            <span class="stat-label">透明思考流与 Token 统计</span>
          </div>
          <div class="hero-stat-item">
            <span class="stat-num">V<span class="stat-unit">n</span></span>
            <span class="stat-label">不可变版本快照与 Diff 对比</span>
          </div>
        </div>
      </div>

      <!-- Kaggle-Style Bold Geometric Hero Illustration -->
      <div class="hero-visual" aria-hidden="true">
        <svg viewBox="0 0 460 340" fill="none" xmlns="http://www.w3.org/2000/svg" class="hero-svg">
          <!-- Soft geometric background shapes -->
          <circle cx="355" cy="95" r="78" fill="#E6F8FF" />
          <rect x="44" y="216" width="126" height="92" rx="18" fill="#FEF7E0" />
          <circle cx="88" cy="84" r="34" fill="#E6F4EA" />

          <!-- Subtle dot matrix -->
          <g fill="#DADCE0">
            <circle cx="34" cy="148" r="2.5" /><circle cx="50" cy="148" r="2.5" /><circle cx="66" cy="148" r="2.5" />
            <circle cx="34" cy="164" r="2.5" /><circle cx="50" cy="164" r="2.5" /><circle cx="66" cy="164" r="2.5" />
            <circle cx="34" cy="180" r="2.5" /><circle cx="50" cy="180" r="2.5" /><circle cx="66" cy="180" r="2.5" />
            <circle cx="394" cy="240" r="2.5" /><circle cx="410" cy="240" r="2.5" /><circle cx="426" cy="240" r="2.5" />
            <circle cx="394" cy="256" r="2.5" /><circle cx="410" cy="256" r="2.5" /><circle cx="426" cy="256" r="2.5" />
          </g>

          <!-- Main Browser Window Card -->
          <rect x="86" y="42" width="302" height="218" rx="16" fill="#FFFFFF" stroke="#202124" stroke-width="2" />
          <path d="M86 58C86 49.1634 93.1634 42 102 42H372C380.837 42 388 49.1634 388 58V76H86V58Z" fill="#F8F9FA" />
          <line x1="86" y1="76" x2="388" y2="76" stroke="#202124" stroke-width="1.5" />
          <circle cx="106" cy="59" r="4.5" fill="#202124" />
          <circle cx="121" cy="59" r="4.5" fill="#20BEFF" />
          <circle cx="136" cy="59" r="4.5" fill="#F9AB00" />
          <rect x="166" y="51" width="144" height="16" rx="8" fill="#FFFFFF" stroke="#DADCE0" stroke-width="1.2" />

          <!-- Code / UI Blocks inside Browser -->
          <rect x="108" y="96" width="114" height="74" rx="10" fill="#202124" />
          <rect x="120" y="110" width="46" height="6" rx="3" fill="#20BEFF" />
          <rect x="120" y="124" width="76" height="5" rx="2.5" fill="#9AA0A6" />
          <rect x="120" y="136" width="60" height="5" rx="2.5" fill="#34A853" />
          <rect x="120" y="148" width="84" height="5" rx="2.5" fill="#F9AB00" />

          <!-- Preview UI Card inside Browser -->
          <rect x="234" y="96" width="132" height="142" rx="10" fill="#F8F9FA" stroke="#DADCE0" stroke-width="1.5" />
          <rect x="248" y="110" width="104" height="44" rx="8" fill="#20BEFF" />
          <rect x="248" y="166" width="64" height="8" rx="4" fill="#202124" />
          <rect x="248" y="182" width="96" height="6" rx="3" fill="#BDC1C6" />
          <rect x="248" y="196" width="82" height="6" rx="3" fill="#BDC1C6" />
          <rect x="248" y="212" width="54" height="16" rx="8" fill="#202124" />

          <!-- Floating Version Pill Badge -->
          <rect x="108" y="184" width="114" height="54" rx="10" fill="#E6F8FF" stroke="#20BEFF" stroke-width="1.5" />
          <circle cx="128" cy="211" r="10" fill="#20BEFF" />
          <path d="M124 211L127 214L133 208" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          <rect x="146" y="201" width="60" height="7" rx="3.5" fill="#202124" />
          <rect x="146" y="214" width="44" height="5" rx="2.5" fill="#5F6368" />

          <!-- Bottom Floating Live Deploy Badge -->
          <rect x="164" y="276" width="196" height="40" rx="20" fill="#202124" />
          <circle cx="186" cy="296" r="5" fill="#34A853" />
          <rect x="200" y="292" width="92" height="8" rx="4" fill="#FFFFFF" />
          <rect x="304" y="287" width="42" height="18" rx="9" fill="#20BEFF" />
        </svg>
      </div>
    </section>

    <!-- Crisp Kaggle-Style AI Prompt Studio -->
    <section id="projectPrompt" class="prompt-studio-section">
      <div class="prompt-studio-card">
        <div class="studio-top-bar">
          <div class="studio-title-wrap">
            <span class="studio-cyan-dot"></span>
            <h2 class="studio-heading">AI 应用构建台</h2>
            <span class="studio-sub-tag">输入需求即可自动规划、编码并运行</span>
          </div>
          <div class="studio-model-select">
            <span class="model-label">推理模型</span>
            <a-select
              v-model:value="selectedAgent"
              size="small"
              class="agent-select"
              :disabled="creating"
            >
              <a-select-option value="default">默认 Agent（极速构建）</a-select-option>
              <a-select-option value="deepseek">DeepSeek Flash（深度推理）</a-select-option>
            </a-select>
          </div>
        </div>

        <div class="studio-input-area">
          <a-textarea
            v-model:value="userPrompt"
            placeholder="例如：创建一个现代化的数据分析仪表盘，包含顶部 KPI 指标卡片、趋势图表、筛选工具栏与暗色模式切换……（Ctrl / ⌘ + Enter 快速开始）"
            :rows="4"
            :maxlength="1000"
            class="studio-textarea"
            :disabled="creating"
            @keydown.meta.enter.prevent="createApp"
            @keydown.ctrl.enter.prevent="createApp"
          />
        </div>

        <div class="studio-templates-row">
          <span class="templates-label">灵感预设</span>
          <div class="templates-chips">
            <button
              v-for="item in promptIdeas"
              :key="item.label"
              type="button"
              class="kaggle-chip"
              @click="setPrompt(item.prompt)"
            >
              <span class="chip-cat">{{ item.category }}</span>
              <span>{{ item.label }}</span>
            </button>
          </div>
        </div>

        <div class="studio-bottom-bar">
          <div class="studio-meta-left">
            <span :class="{ 'char-warn': userPrompt.length > 900 }">{{ userPrompt.length }}</span>
            <span class="char-dim">/ 1000 字</span>
            <span class="meta-dot">·</span>
            <span class="shortcut-tip"><kbd>⌘</kbd> + <kbd>Enter</kbd> 直接创建</span>
          </div>

          <a-button
            type="primary"
            size="large"
            class="build-submit-btn"
            :loading="creating"
            @click="createApp"
          >
            <template #icon><RocketOutlined /></template>
            <span>{{ creating ? '正在初始化工程...' : '立即开始构建' }}</span>
          </a-button>
        </div>
      </div>
    </section>

    <!-- My Workspace Section (Kaggle Explorer Layout) -->
    <section id="myWorkspaceSection" class="explorer-section">
      <div class="explorer-header">
        <div class="explorer-heading-block">
          <div class="explorer-title-row">
            <h2 class="explorer-title">我的作品</h2>
            <span class="count-pill">{{ myAppsPage.total }}</span>
          </div>
          <p class="explorer-desc">继续与 AI 对话迭代、管理历史版本快照或一键发布至公网。</p>
        </div>

        <div v-if="loginUserStore.loginUser.id" class="explorer-controls">
          <div class="kaggle-search-pill">
            <SearchOutlined class="pill-search-icon" />
            <input
              v-model="myAppsFilter.appName"
              type="text"
              placeholder="搜索我的作品..."
              @keydown.enter="triggerMyAppsSearch"
              @input="onMyAppsSearchClear"
            />
          </div>
        </div>
      </div>

      <!-- Horizontal Filter Chips (Kaggle Style) -->
      <div v-if="loginUserStore.loginUser.id" class="filter-chip-bar">
        <button
          v-for="tab in archFilterTabs"
          :key="tab.value"
          type="button"
          class="filter-chip"
          :class="{ 'filter-chip--active': myAppsFilter.codeGenType === tab.value }"
          @click="setMyAppsArchFilter(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Not Logged In State -->
      <div v-if="!loginUserStore.loginUser.id" class="kaggle-empty-card">
        <div class="empty-badge">MY WORKSPACE</div>
        <h3>登录以查看你的专属作品集</h3>
        <p>登录后可永久保留全部 Agent 对话记录、Token 用量统计与多版本快照。</p>
        <a-button type="primary" size="large" @click="goToLogin">立即登录 / 注册</a-button>
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
      <div v-else class="kaggle-empty-card">
        <FolderOpenOutlined class="empty-line-icon" />
        <h3>{{ hasMyAppsFilter ? '没有找到匹配的作品' : '暂无作品' }}</h3>
        <p>
          {{
            hasMyAppsFilter
              ? '尝试切换架构筛选或清空搜索关键词。'
              : '在上方 AI 应用构建台输入一句描述，即可自动生成你的第一个应用。'
          }}
        </p>
        <a-button v-if="hasMyAppsFilter" @click="resetMyAppsFilter">重置筛选</a-button>
        <a-button v-else type="primary" @click="scrollToPrompt">
          <template #icon><PlusOutlined /></template>
          创建第一个应用
        </a-button>
      </div>

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

    <!-- Community Featured Gallery Section -->
    <section id="featuredGallerySection" class="explorer-section">
      <div class="explorer-header">
        <div class="explorer-heading-block">
          <div class="explorer-title-row">
            <h2 class="explorer-title">精选应用画廊</h2>
            <span class="count-pill">{{ featuredAppsPage.total }}</span>
          </div>
          <p class="explorer-desc">探索社区创作者使用 Epiph-code 生成的交互站点、效率工具与完整工程。</p>
        </div>

        <div class="explorer-controls">
          <div class="kaggle-search-pill">
            <SearchOutlined class="pill-search-icon" />
            <input
              v-model="featuredFilter.appName"
              type="text"
              placeholder="搜索精选画廊..."
              @keydown.enter="triggerFeaturedSearch"
              @input="onFeaturedSearchClear"
            />
          </div>
        </div>
      </div>

      <div class="filter-chip-bar">
        <button
          v-for="tab in archFilterTabs"
          :key="tab.value"
          type="button"
          class="filter-chip"
          :class="{ 'filter-chip--active': featuredFilter.codeGenType === tab.value }"
          @click="setFeaturedArchFilter(tab.value)"
        >
          {{ tab.label }}
        </button>
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

      <div v-else class="kaggle-empty-card">
        <CompassOutlined class="empty-line-icon" />
        <h3>{{ hasFeaturedFilter ? '未找到匹配的精选作品' : '暂无精选应用' }}</h3>
        <p>
          {{
            hasFeaturedFilter
              ? '尝试更换关键词或选择「全部」查看所有精选应用。'
              : '你可以在「我的作品」点击卡片星标，将优秀站点推荐到社区精选画廊。'
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
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, deleteApp, listGoodAppVoByPage, listMyAppVoByPage, toggleAppFeatured } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import AppCard from '@/components/AppCard.vue'
import {
  RocketOutlined,
  FolderOpenOutlined,
  CompassOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const userPrompt = ref('')
const selectedAgent = ref('default')
const creating = ref(false)

const archFilterTabs = [
  { label: '全部架构', value: '' },
  { label: 'HTML 单页', value: CodeGenTypeEnum.HTML },
  { label: '原生多文件', value: CodeGenTypeEnum.MULTI_FILE },
  { label: 'Vue 3 工程', value: CodeGenTypeEnum.VUE_PROJECT },
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
    category: '博客',
    label: '现代极客博客',
    prompt:
      '创建一个现代化的个人技术博客网站，包含精选文章瀑布流、标签分类过滤、即时搜索框、代码块高亮展示、阅读进度条、暗色模式切换和作者名片。采用清爽现代排版，支持响应式布局。',
  },
  {
    category: '官网',
    label: 'AI SaaS 落地页',
    prompt:
      '设计一个极具高级感的 AI SaaS 产品官网，包含高对比度 Hero 首屏、核心功能矩阵、交互式价格方案对比表（支持月付/年付切换）、客户证言轮播以及常见问题折叠面板。',
  },
  {
    category: '商城',
    label: '潮玩数码商城',
    prompt:
      '构建一个现代风格的数码潮玩商城，包含顶部促销横幅、分类筛选栏、商品卡片网格（支持快速预览与规格选择）、右侧滑出购物车抽屉、实时优惠计算与模拟结算弹窗。',
  },
  {
    category: '作品集',
    label: '设计师画廊',
    prompt:
      '制作一个视觉设计师与摄影师作品集网站，采用沉浸式画廊布局，支持按作品类型筛选、点击大图灯箱预览、个人技能时间轴履历以及合作联系表单。',
  },
  {
    category: '数据',
    label: '运营分析看板',
    prompt:
      '开发一个企业级 SaaS 运营数据分析仪表盘，包含核心 KPI 指标卡片（带环比增长徽章）、营收趋势折线图、渠道转化分布、最近订单状态表格以及时间区间筛选器。',
  },
  {
    category: '工具',
    label: '番茄钟任务板',
    prompt:
      '构建一个沉浸式专注生产力工具，左侧为环形倒计时番茄钟（支持专注/短休/长休模式切换），右侧为四象限待办任务看板（支持新增、完成勾选、优先级标签与专注统计）。',
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

watch(
  () => route.query.q,
  (q) => {
    if (typeof q === 'string' && q.trim()) {
      featuredFilter.appName = q.trim()
      featuredAppsPage.current = 1
      loadFeaturedApps()
    }
  },
  { immediate: true },
)

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

const scrollToGallery = () => {
  document.getElementById('featuredGallerySection')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
  gap: 56px;
}

/* Kaggle Editorial Split Hero */
.kaggle-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  align-items: center;
  gap: 40px;
  padding: 20px 0 8px;
}

.hero-title {
  margin: 0;
  font-size: clamp(34px, 3.8vw, 48px);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.035em;
  color: #202124;
}

.hero-subtitle {
  max-width: 560px;
  margin: 18px 0 0;
  font-size: 16px;
  line-height: 1.65;
  color: #5F6368;
}

.hero-cta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.cta-black-pill,
.cta-outline-pill {
  height: 44px;
  padding: 0 24px;
  font-size: 14.5px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-top: 34px;
  padding-top: 24px;
  border-top: 1px solid #E8EAED;
}

.hero-stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #202124;
  line-height: 1.1;
}

.stat-unit {
  font-size: 14px;
  font-weight: 700;
  color: #008ABC;
  margin-left: 2px;
}

.stat-label {
  font-size: 12.5px;
  color: #5F6368;
  line-height: 1.4;
}

.hero-visual {
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-svg {
  width: 100%;
  max-width: 460px;
  height: auto;
}

/* Crisp Kaggle-Style Prompt Studio */
.prompt-studio-card {
  background: #FFFFFF;
  border: 1.5px solid #DADCE0;
  border-radius: 20px;
  overflow: hidden;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.prompt-studio-card:focus-within {
  border-color: #202124;
  box-shadow: 0 8px 28px rgba(32, 33, 36, 0.08);
}

.studio-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 22px;
  background: #F8F9FA;
  border-bottom: 1px solid #E8EAED;
}

.studio-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.studio-cyan-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #20BEFF;
}

.studio-heading {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #202124;
}

.studio-sub-tag {
  font-size: 12.5px;
  color: #5F6368;
}

.studio-model-select {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-label {
  font-size: 12px;
  font-weight: 600;
  color: #5F6368;
}

.agent-select {
  min-width: 200px;
}

.studio-input-area {
  padding: 18px 22px 10px;
}

.studio-textarea {
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
  font-size: 15px;
  line-height: 1.65;
  color: #202124;
  resize: none;
}

.studio-templates-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 22px 16px;
}

.templates-label {
  font-size: 12px;
  font-weight: 700;
  color: #5F6368;
  margin-right: 4px;
}

.templates-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.kaggle-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px 5px 7px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  color: #202124;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip-cat {
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 700;
  color: #008ABC;
  background: #E6F8FF;
  border-radius: 999px;
}

.kaggle-chip:hover {
  border-color: #202124;
  background: #F8F9FA;
}

.studio-bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 22px;
  background: #F8F9FA;
  border-top: 1px solid #E8EAED;
}

.studio-meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #5F6368;
}

.char-dim {
  color: #80868B;
}

.char-warn {
  color: #D93025;
  font-weight: 700;
}

.meta-dot {
  color: #BDC1C6;
}

.shortcut-tip kbd {
  padding: 1px 6px;
  font-size: 11px;
  font-family: inherit;
  color: #202124;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 5px;
}

.build-submit-btn {
  height: 40px;
  padding: 0 22px;
  font-size: 14px;
}

/* Explorer Sections (Kaggle Datasets / Models Layout) */
.explorer-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.explorer-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.explorer-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.explorer-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.025em;
  color: #202124;
}

.count-pill {
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #5F6368;
  background: #F1F3F4;
  border-radius: 999px;
}

.explorer-desc {
  margin: 5px 0 0;
  font-size: 14px;
  color: #5F6368;
}

.kaggle-search-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 260px;
  height: 38px;
  padding: 0 14px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  transition: border-color 0.15s ease;
}

.kaggle-search-pill:focus-within {
  border-color: #202124;
}

.pill-search-icon {
  color: #5F6368;
  font-size: 14px;
}

.kaggle-search-pill input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13.5px;
  color: #202124;
}

.filter-chip-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 4px;
}

.filter-chip {
  height: 32px;
  padding: 0 15px;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  background: #FFFFFF;
  color: #3C4043;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-chip:hover {
  background: #F8F9FA;
  border-color: #202124;
}

.filter-chip--active {
  background: #202124;
  border-color: #202124;
  color: #FFFFFF;
}

.filter-chip--active:hover {
  background: #3C4043;
  border-color: #3C4043;
  color: #FFFFFF;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 20px;
}

.kaggle-empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 24px;
  background: #F8F9FA;
  border: 1px solid #E8EAED;
  border-radius: 16px;
  text-align: center;
}

.empty-badge {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #008ABC;
  background: #E6F8FF;
  border-radius: 999px;
}

.empty-line-icon {
  font-size: 30px;
  color: #80868B;
}

.kaggle-empty-card h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #202124;
}

.kaggle-empty-card p {
  margin: 0 0 8px;
  font-size: 13.5px;
  color: #5F6368;
  max-width: 440px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

@media (max-width: 1024px) {
  .kaggle-hero {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .hero-visual {
    display: none;
  }
}

@media (max-width: 768px) {
  .studio-sub-tag,
  .studio-model-select,
  .shortcut-tip,
  .meta-dot {
    display: none;
  }

  .hero-stats {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .kaggle-search-pill {
    width: 100%;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
