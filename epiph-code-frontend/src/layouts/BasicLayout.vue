<template>
  <div class="kaggle-shell" :class="{ 'kaggle-shell--chat': isChatPage, 'kaggle-shell--collapsed': sidebarCollapsed }">
    <!-- Kaggle-Style Fixed Left Navigation Sidebar (Hidden on full-bleed IDE chat page) -->
    <aside v-if="!isChatPage" class="kaggle-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
      <div class="sidebar-top">
        <button
          type="button"
          class="sidebar-toggle-btn"
          :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="sidebarCollapsed = !sidebarCollapsed"
        >
          <MenuUnfoldOutlined v-if="sidebarCollapsed" />
          <MenuFoldOutlined v-else />
        </button>

        <RouterLink v-show="!sidebarCollapsed" to="/" class="sidebar-brand" aria-label="回到首页">
          <span class="brand-wordmark">epiph<span class="brand-accent">-code</span></span>
        </RouterLink>
      </div>

      <!-- Signature Kaggle Floating "+ Create" Pill CTA -->
      <div class="sidebar-create-wrap">
        <button type="button" class="kaggle-create-btn" @click="handleQuickCreate">
          <span class="create-plus-icon">
            <PlusOutlined />
          </span>
          <span v-show="!sidebarCollapsed" class="create-label">创建应用</span>
        </button>
      </div>

      <!-- Vertical Navigation Rail -->
      <nav class="sidebar-nav" aria-label="主导航">
        <button
          v-for="item in primaryNavItems"
          :key="item.key"
          type="button"
          class="rail-item"
          :class="{ 'rail-item--active': isNavActive(item.key) }"
          :title="sidebarCollapsed ? item.title : undefined"
          @click="handleNavClick(item)"
        >
          <component :is="item.icon" class="rail-icon" />
          <span v-show="!sidebarCollapsed" class="rail-label">{{ item.title }}</span>
        </button>

        <template v-if="adminNavItems.length">
          <div class="rail-divider"></div>
          <div v-show="!sidebarCollapsed" class="rail-section-label">平台管理</div>
          <button
            v-for="item in adminNavItems"
            :key="item.key"
            type="button"
            class="rail-item"
            :class="{ 'rail-item--active': isNavActive(item.key) }"
            :title="sidebarCollapsed ? item.title : undefined"
            @click="handleNavClick(item)"
          >
            <component :is="item.icon" class="rail-icon" />
            <span v-show="!sidebarCollapsed" class="rail-label">{{ item.title }}</span>
          </button>
        </template>
      </nav>

      <!-- Bottom Engine Mode Switcher -->
      <div v-show="!sidebarCollapsed" class="sidebar-bottom">
        <div class="engine-box">
          <div class="engine-box-head">
            <ThunderboltOutlined class="engine-icon" />
            <span>生成引擎</span>
          </div>
          <a-segmented
            v-model:value="globalGenerationMode"
            :options="generationModeOptions"
            block
            size="small"
            @change="onGlobalModeChange"
          />
        </div>
      </div>
    </aside>

    <!-- Main Right Column -->
    <div class="kaggle-body" :class="{ 'kaggle-body--chat': isChatPage }">
      <GlobalHeader :compact="isChatPage" />
      <main class="main-content" :class="{ 'main-content--chat': isChatPage }">
        <RouterView />
      </main>
      <GlobalFooter v-if="!isChatPage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import {
  getGlobalChatGenMode,
  setGlobalChatGenMode,
  type ChatGenMode,
} from '@/utils/chatGenMode'
import {
  HomeOutlined,
  AppstoreOutlined,
  CompassOutlined,
  TeamOutlined,
  DatabaseOutlined,
  MessageOutlined,
  PlusOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const sidebarCollapsed = ref(false)
const globalGenerationMode = ref<ChatGenMode>(getGlobalChatGenMode())
const generationModeOptions = [
  { label: '标准', value: 'classic' },
  { label: '工作流', value: 'workflow' },
]

const isChatPage = computed(() => route.path.startsWith('/app/chat/'))

interface RailNavItem {
  key: string
  title: string
  icon: unknown
  anchor?: string
}

const primaryNavItems: RailNavItem[] = [
  { key: '/', title: '首页探索', icon: HomeOutlined },
  { key: '/#workspace', title: '我的作品', icon: AppstoreOutlined, anchor: 'myWorkspaceSection' },
  { key: '/#gallery', title: '精选画廊', icon: CompassOutlined, anchor: 'featuredGallerySection' },
]

const adminNavItems = computed<RailNavItem[]>(() => {
  if (loginUserStore.loginUser?.userRole !== 'admin') return []
  return [
    { key: '/admin/userManage', title: '用户管理', icon: TeamOutlined },
    { key: '/admin/appManage', title: '应用管理', icon: DatabaseOutlined },
    { key: '/admin/chatManage', title: '对话管理', icon: MessageOutlined },
  ]
})

const activeAnchor = ref<string>('')

const isNavActive = (key: string) => {
  if (key.startsWith('/#')) {
    return route.path === '/' && activeAnchor.value === key
  }
  if (key === '/') {
    return route.path === '/' && !activeAnchor.value
  }
  return route.path === key
}

const handleNavClick = async (item: RailNavItem) => {
  if (item.anchor) {
    activeAnchor.value = item.key
    if (route.path !== '/') {
      await router.push('/')
      setTimeout(() => {
        document.getElementById(item.anchor!)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }, 120)
    } else {
      document.getElementById(item.anchor)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
    return
  }
  activeAnchor.value = ''
  router.push(item.key)
}

const handleQuickCreate = async () => {
  activeAnchor.value = ''
  if (route.path !== '/') {
    await router.push('/')
    setTimeout(() => {
      document.getElementById('projectPrompt')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }, 120)
  } else {
    document.getElementById('projectPrompt')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

const onGlobalModeChange = (value: string | number) => {
  const mode: ChatGenMode = value === 'workflow' ? 'workflow' : 'classic'
  globalGenerationMode.value = mode
  setGlobalChatGenMode(mode)
}
</script>

<style scoped>
.kaggle-shell {
  display: flex;
  min-height: 100vh;
  background: #FFFFFF;
}

.kaggle-shell--chat {
  height: 100vh;
  overflow: hidden;
}

/* Fixed Left Kaggle Sidebar */
.kaggle-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 240px;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
  border-right: 1px solid #E8EAED;
  z-index: 120;
  transition: width 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.kaggle-sidebar.is-collapsed {
  width: 68px;
}

.sidebar-top {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 14px;
}

.sidebar-toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #5F6368;
  font-size: 17px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.sidebar-toggle-btn:hover {
  background: #F1F3F4;
  color: #202124;
}

.sidebar-brand {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
}

.brand-wordmark {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #20BEFF;
  line-height: 1;
}

.brand-accent {
  color: #202124;
}

/* Signature Kaggle "+ Create" Pill Button */
.sidebar-create-wrap {
  padding: 6px 14px 18px;
}

.kaggle-create-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 44px;
  padding: 0 20px 0 14px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  color: #202124;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(32, 33, 36, 0.1), 0 1px 2px rgba(32, 33, 36, 0.06);
  transition: all 0.18s ease;
}

.kaggle-sidebar.is-collapsed .kaggle-create-btn {
  width: 40px;
  height: 40px;
  padding: 0;
}

.kaggle-create-btn:hover {
  background: #F8F9FA;
  border-color: #BDC1C6;
  box-shadow: 0 4px 12px rgba(32, 33, 36, 0.14);
}

.create-plus-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #E6F8FF;
  color: #008ABC;
  font-size: 14px;
  font-weight: 700;
}

/* Navigation Rail */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 10px;
  overflow-y: auto;
}

.rail-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #5F6368;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.kaggle-sidebar.is-collapsed .rail-item {
  justify-content: center;
  padding: 0;
}

.rail-item:hover {
  background: #F1F3F4;
  color: #202124;
}

.rail-item--active {
  background: #E6F8FF;
  color: #202124;
  font-weight: 700;
}

.rail-item--active .rail-icon {
  color: #008ABC;
}

.rail-icon {
  font-size: 17px;
  flex-shrink: 0;
}

.rail-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rail-divider {
  height: 1px;
  margin: 12px 10px;
  background: #E8EAED;
}

.rail-section-label {
  padding: 2px 14px 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #80868B;
}

.sidebar-bottom {
  padding: 14px;
  border-top: 1px solid #E8EAED;
}

.engine-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  background: #F8F9FA;
  border: 1px solid #E8EAED;
  border-radius: 14px;
}

.engine-box-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #5F6368;
}

.engine-icon {
  color: #008ABC;
}

/* Main Content Right Area */
.kaggle-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  margin-left: 240px;
  transition: margin-left 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.kaggle-shell--collapsed .kaggle-body {
  margin-left: 68px;
}

.kaggle-body--chat {
  margin-left: 0 !important;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  width: min(1240px, calc(100% - 64px));
  flex: 1;
  margin: 0 auto;
  padding: 24px 0 72px;
}

.main-content--chat {
  width: 100%;
  max-width: 100%;
  height: calc(100vh - 56px);
  margin: 0;
  padding: 0;
  overflow: hidden;
}

@media (max-width: 960px) {
  .kaggle-sidebar {
    display: none;
  }

  .kaggle-body {
    margin-left: 0 !important;
  }

  .main-content {
    width: calc(100% - 32px);
    padding: 16px 0 48px;
  }
}
</style>
