<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const userPrompt = ref('')
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
    label: '个人博客网站',
    prompt:
      '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁设计，支持响应式布局。',
  },
  {
    label: '企业官网',
    prompt:
      '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面，采用商务简约风格并支持移动端。',
  },
  {
    label: '在线商城',
    prompt:
      '构建一个在线商城，包含商品展示、购物车、用户登录注册、订单管理、支付结算等功能，界面清晰且易于浏览。',
  },
  {
    label: '作品展示网站',
    prompt:
      '制作一个作品展示网站，包含作品画廊、项目详情、个人介绍和联系方式，风格简洁高级，支持分类筛选。',
  },
]

const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({ initPrompt: userPrompt.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
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

<template>
  <div id="homePage">
    <section class="hero-section" aria-labelledby="heroTitle">
      <div class="hero-copy">
        <div class="hero-kicker"><span class="hero-kicker-dot"></span> AI APP WORKSPACE</div>
        <h1 id="heroTitle" class="hero-title">把想法，<br /><span>做成真正可用的应用</span></h1>
        <p class="hero-subtitle">
          从一句描述开始，完成生成、编辑、预览与部署。让 AI 帮你把灵感变成可以运行的产品。
        </p>

        <div class="hero-features">
          <div class="hero-feature"><span class="feature-index">01</span><span>自然语言生成</span></div>
          <div class="hero-feature"><span class="feature-index">02</span><span>可视化工作流</span></div>
          <div class="hero-feature"><span class="feature-index">03</span><span>持续编辑与预览</span></div>
          <div class="hero-feature"><span class="feature-index">04</span><span>本地一键部署</span></div>
        </div>

        <div class="hero-footnote">
          <span class="status-dot"></span>
          支持 HTML、多文件与 Vue 工程
        </div>
      </div>

      <div id="projectPrompt" class="prompt-panel">
        <div class="prompt-panel-head">
          <div>
            <span class="prompt-panel-kicker">START A PROJECT</span>
            <h2>描述你的应用</h2>
          </div>
          <span class="prompt-step">01 <span>/</span> START</span>
        </div>

        <a-textarea
          v-model:value="userPrompt"
          placeholder="例如：帮我创建一个简洁的个人博客，包含文章列表、分类和个人介绍……"
          :rows="5"
          :maxlength="1000"
          class="prompt-input"
          aria-label="描述你想创建的应用"
        />

        <div class="prompt-panel-actions">
          <div class="prompt-hint">
            <span>{{ userPrompt.length }}</span> / 1000
            <span class="hint-divider">·</span>
            描述越具体，生成结果越贴合
          </div>
          <a-button class="generate-button" type="primary" size="large" :loading="creating" @click="createApp">
            <span>{{ creating ? '正在创建' : '开始生成' }}</span>
            <span class="generate-arrow" aria-hidden="true">→</span>
          </a-button>
        </div>

        <div class="prompt-examples">
          <span class="examples-label">试试灵感</span>
          <div class="example-chips">
            <button
              v-for="item in promptIdeas"
              :key="item.label"
              type="button"
              class="example-chip"
              @click="setPrompt(item.prompt)"
            >
              {{ item.label }}
              <span aria-hidden="true">↗</span>
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="content-section" aria-labelledby="worksTitle">
      <div class="section-head">
        <div>
          <span class="section-kicker">YOUR WORKSPACE</span>
          <div class="section-title-row">
            <h2 id="worksTitle">我的作品</h2>
            <span class="section-count">{{ myAppsPage.total }} 个应用</span>
          </div>
        </div>
        <span class="section-note">你创建的应用会集中显示在这里</span>
      </div>

      <div v-if="myApps.length" class="app-grid">
        <AppCard
          v-for="app in myApps"
          :key="app.id"
          :app="app"
          @view-chat="viewChat"
          @view-work="viewWork"
        />
      </div>

      <div v-else class="empty-state">
        <div class="empty-mark">01</div>
        <div class="empty-copy">
          <h3>{{ loginUserStore.loginUser.id ? '你的作品空间已准备好' : '登录后开始创建作品' }}</h3>
          <p>
            {{
              loginUserStore.loginUser.id
                ? '从一个想法开始，AI 会帮你搭建第一个版本。'
                : '登录后即可保存、编辑并管理你创建的应用。'
            }}
          </p>
        </div>
        <a-button v-if="loginUserStore.loginUser.id" class="empty-action" @click="scrollToPrompt">
          描述一个想法 <span aria-hidden="true">→</span>
        </a-button>
        <a-button v-else class="empty-action" @click="goToLogin">
          前往登录 <span aria-hidden="true">→</span>
        </a-button>
      </div>

      <div v-if="myAppsPage.total > myAppsPage.pageSize" class="pagination-wrapper">
        <a-pagination
          v-model:current="myAppsPage.current"
          v-model:page-size="myAppsPage.pageSize"
          :total="myAppsPage.total"
          :show-size-changer="false"
          :show-total="(total: number) => `共 ${total} 个应用`"
          @change="loadMyApps"
        />
      </div>
    </section>

    <section class="content-section" aria-labelledby="featuredTitle">
      <div class="section-head">
        <div>
          <span class="section-kicker">COMMUNITY PICKS</span>
          <div class="section-title-row">
            <h2 id="featuredTitle">精选案例</h2>
            <span class="section-count">{{ featuredAppsPage.total }} 个案例</span>
          </div>
        </div>
        <span class="section-note">看看大家用 AI 做出了什么</span>
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

      <div v-else class="empty-state empty-state--featured">
        <div class="empty-mark empty-mark--featured">✦</div>
        <div class="empty-copy">
          <h3>精选案例正在准备中</h3>
          <p>这里会展示值得参考的作品。你也可以先创建自己的第一个应用。</p>
        </div>
        <a-button class="empty-action" @click="scrollToPrompt">开始创作 <span aria-hidden="true">→</span></a-button>
      </div>

      <div v-if="featuredAppsPage.total > featuredAppsPage.pageSize" class="pagination-wrapper">
        <a-pagination
          v-model:current="featuredAppsPage.current"
          v-model:page-size="featuredAppsPage.pageSize"
          :total="featuredAppsPage.total"
          :show-size-changer="false"
          :show-total="(total: number) => `共 ${total} 个案例`"
          @change="loadFeaturedApps"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  min-height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-section {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(440px, 1.08fr);
  align-items: center;
  gap: clamp(28px, 5vw, 64px);
  padding: clamp(30px, 4.5vw, 58px);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 28px;
  color: #fff;
  background: linear-gradient(128deg, #1d2231 0%, #292b3b 56%, #3b3338 100%);
  box-shadow: 0 28px 76px rgba(35, 38, 54, 0.18);
}

.hero-section::before,
.hero-section::after {
  content: '';
  position: absolute;
  z-index: -1;
  pointer-events: none;
  border-radius: 50%;
}

.hero-section::before {
  width: 480px;
  height: 480px;
  top: -260px;
  right: 12%;
  background: radial-gradient(circle, rgba(226, 157, 112, 0.27), transparent 68%);
}

.hero-section::after {
  width: 540px;
  height: 540px;
  right: -320px;
  bottom: -360px;
  background: radial-gradient(circle, rgba(123, 139, 186, 0.28), transparent 68%);
}

.hero-copy,
.prompt-panel {
  position: relative;
  z-index: 1;
}

.hero-copy {
  max-width: 530px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  color: #e8d9d0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.hero-kicker-dot,
.status-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #87d1a0;
  box-shadow: 0 0 0 4px rgba(135, 209, 160, 0.13);
}

.hero-title {
  margin: 24px 0 16px;
  color: #fff;
  font-size: clamp(36px, 3.5vw, 50px);
  font-weight: 750;
  line-height: 1.18;
  letter-spacing: -0.055em;
}

.hero-title span {
  display: inline-block;
  color: #f0b88f;
  background: linear-gradient(90deg, #f2c4a1 0%, #e9a579 65%, #f7d5bc 100%);
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  max-width: 470px;
  margin: 0;
  color: rgba(240, 241, 247, 0.72);
  font-size: 15px;
  line-height: 1.85;
}

.hero-features {
  display: grid;
  grid-template-columns: repeat(2, max-content);
  gap: 15px 22px;
  margin-top: 30px;
}

.hero-feature {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(245, 245, 249, 0.88);
  font-size: 13px;
}

.feature-index {
  color: #d6a17f;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.hero-footnote {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 32px;
  color: rgba(235, 236, 242, 0.58);
  font-size: 12px;
}

.prompt-panel {
  min-width: 0;
  padding: clamp(18px, 2.4vw, 28px);
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 22px;
  background: #fff;
  color: var(--zc-text-primary);
  box-shadow: 0 24px 64px rgba(8, 12, 25, 0.24);
}

.prompt-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.prompt-panel-kicker,
.section-kicker {
  display: block;
  color: #a37158;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.prompt-panel-head h2 {
  margin: 7px 0 0;
  color: #242735;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.prompt-step {
  padding: 7px 10px;
  border-radius: 9px;
  background: #f6f3f1;
  color: #8a7468;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.prompt-step span {
  margin: 0 3px;
  color: #c7b8ae;
}

.prompt-input {
  width: 100%;
}

.prompt-input :deep(.ant-input) {
  min-height: 138px;
  padding: 15px 16px;
  border: 1px solid #e8e9ee;
  border-radius: 14px;
  background: #fafbfc;
  color: #262a35;
  font-size: 14px;
  line-height: 1.75;
  box-shadow: none;
  resize: vertical;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.prompt-input :deep(.ant-input::placeholder) {
  color: #a0a5b1;
}

.prompt-input :deep(.ant-input:hover) {
  border-color: #d7b8a5;
}

.prompt-input :deep(.ant-input:focus) {
  border-color: var(--zc-primary);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(184, 106, 66, 0.11);
}

.prompt-panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
}

.prompt-hint {
  color: #9a9eaa;
  font-size: 11px;
  line-height: 1.6;
}

.prompt-hint > span:first-child {
  color: #6f7481;
  font-variant-numeric: tabular-nums;
}

.hint-divider {
  margin: 0 6px;
  color: #c0c3cb;
}

.generate-button {
  height: 46px;
  padding: 0 17px;
  border: 0;
  border-radius: 12px;
  box-shadow: 0 8px 18px rgba(184, 106, 66, 0.2);
  font-weight: 600;
}

.generate-arrow {
  margin-left: 10px;
  font-size: 17px;
  transition: transform 0.2s ease;
}

.generate-button:hover .generate-arrow {
  transform: translateX(3px);
}

.prompt-examples {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 20px;
  padding-top: 17px;
  border-top: 1px solid #f0f0f3;
}

.examples-label {
  padding-top: 7px;
  color: #9297a4;
  font-size: 11px;
  white-space: nowrap;
}

.example-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.example-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid #e9e9ee;
  border-radius: 999px;
  background: #fff;
  color: #666c79;
  cursor: pointer;
  font: inherit;
  font-size: 11px;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.example-chip span {
  color: #b8876d;
  font-size: 12px;
}

.example-chip:hover {
  transform: translateY(-1px);
  border-color: #d9b6a0;
  background: #fbf7f4;
  color: #8a5539;
}

.content-section {
  min-width: 0;
  padding: 23px 25px;
  border: 1px solid rgba(225, 228, 235, 0.9);
  border-radius: 20px;
  background: var(--zc-bg-card);
  box-shadow: var(--zc-shadow);
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-kicker {
  margin-bottom: 5px;
  color: #9a8290;
  font-size: 9px;
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 11px;
}

.section-head h2 {
  margin: 0;
  color: #282b36;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.section-count {
  padding: 4px 9px;
  border-radius: 999px;
  background: #f3f4f7;
  color: #777d8a;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.section-note {
  color: #969ba7;
  font-size: 12px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.empty-state {
  display: flex;
  align-items: center;
  gap: 17px;
  min-height: 112px;
  padding: 20px 22px;
  border: 1px dashed #e1e4eb;
  border-radius: 16px;
  background: linear-gradient(110deg, #fafbfc 0%, #f8f8fa 100%);
}

.empty-mark {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid #f0e0d5;
  border-radius: 15px;
  background: linear-gradient(145deg, #fff8f3, #f7e9df);
  color: #a96c4a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.empty-mark--featured {
  border-color: #e2e5f1;
  background: linear-gradient(145deg, #f5f6fc, #e9edf7);
  color: #66729b;
  font-size: 22px;
}

.empty-copy {
  min-width: 0;
  flex: 1;
}

.empty-copy h3 {
  margin: 0;
  color: #383c48;
  font-size: 14px;
  font-weight: 650;
}

.empty-copy p {
  margin: 5px 0 0;
  color: #9297a3;
  font-size: 12px;
  line-height: 1.6;
}

.empty-action {
  height: 36px;
  flex: 0 0 auto;
  border-radius: 10px;
  color: #755340;
  font-size: 12px;
}

.empty-action span {
  margin-left: 7px;
  font-size: 14px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 980px) {
  .hero-section {
    grid-template-columns: minmax(0, 1fr) minmax(360px, 1fr);
    gap: 28px;
    padding: 32px;
  }

  .hero-title {
    font-size: clamp(36px, 4.2vw, 46px);
  }

  .hero-features {
    gap: 13px 15px;
  }
}

@media (max-width: 768px) {
  .hero-section {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
    padding: 25px 20px 20px;
    border-radius: 22px;
  }

  .hero-title {
    margin-top: 19px;
    font-size: clamp(34px, 8vw, 46px);
  }

  .hero-subtitle {
    max-width: 560px;
    font-size: 14px;
  }

  .hero-features {
    margin-top: 22px;
  }

  .hero-footnote {
    margin-top: 21px;
  }

  .prompt-panel {
    padding: 18px;
    border-radius: 18px;
  }

  .prompt-input :deep(.ant-input) {
    min-height: 120px;
    font-size: 13px;
  }

  .prompt-panel-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .generate-button {
    width: 100%;
    justify-content: center;
  }

  .prompt-examples {
    flex-direction: column;
    gap: 8px;
  }

  .examples-label {
    padding-top: 0;
  }

  .content-section {
    padding: 18px 16px;
    border-radius: 17px;
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 14px;
  }

  .empty-state {
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 12px;
    padding: 16px;
  }

  .empty-copy {
    flex-basis: calc(100% - 62px);
  }

  .empty-action {
    margin-left: 60px;
  }
}

@media (max-width: 420px) {
  .hero-features {
    grid-template-columns: minmax(0, 1fr);
    gap: 11px;
  }

  .section-title-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
