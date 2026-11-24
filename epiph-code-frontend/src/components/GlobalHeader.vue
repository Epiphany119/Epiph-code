<template>
  <header class="site-header">
    <div class="header-inner">
      <!-- Brand Logo & Title -->
      <RouterLink to="/" class="brand-link" aria-label="回到首页">
        <div class="brand-logo-wrap">
          <img src="@/assets/logo-ai-studio.svg" alt="Epiph-code Logo" class="brand-logo" />
        </div>
        <div class="brand-text">
          <span class="brand-name">Epiph<span class="brand-highlight">-code</span></span>
          <span class="brand-badge">STUDIO</span>
        </div>
      </RouterLink>

      <!-- Navigation Menu -->
      <nav class="nav-links" aria-label="主导航">
        <button
          v-for="item in menuItems"
          :key="String(item?.key)"
          type="button"
          class="nav-link-btn"
          :class="{ 'nav-link-btn--active': selectedKeys.includes(String(item?.key)) }"
          @click="handleMenuClick({ key: item?.key as string })"
        >
          <span class="nav-link-label">{{ item?.title }}</span>
        </button>
      </nav>

      <!-- Right Tools -->
      <div class="header-tools">
        <!-- Global Generation Mode Switcher -->
        <div class="mode-pill" title="切换全局代码生成引擎模式">
          <span class="mode-label">
            <ThunderboltOutlined class="mode-icon" />
            <span>引擎</span>
          </span>
          <a-segmented
            v-model:value="globalGenerationMode"
            :options="generationModeOptions"
            size="small"
            @change="onGlobalModeChange"
          />
        </div>

        <span class="header-divider"></span>

        <!-- User Profile / Login -->
        <div v-if="loginUserStore.loginUser.id" class="user-zone">
          <a-dropdown trigger="click" placement="bottomRight">
            <button type="button" class="user-profile-btn">
              <a-avatar
                :size="28"
                :src="loginUserStore.loginUser.userAvatar"
                class="user-avatar"
              >
                {{ displayUserName.charAt(0).toUpperCase() }}
              </a-avatar>
              <span class="user-name">{{ displayUserName }}</span>
              <span v-if="loginUserStore.loginUser.userRole === 'admin'" class="role-chip">Admin</span>
              <DownOutlined class="user-caret" />
            </button>
            <template #overlay>
              <a-menu class="user-menu-dropdown">
                <a-menu-item key="user" disabled>
                  <div class="menu-user-meta">
                    <span class="menu-user-account">{{ loginUserStore.loginUser.userAccount || displayUserName }}</span>
                    <span class="menu-role-tag">
                      {{ loginUserStore.loginUser.userRole === 'admin' ? '平台管理员' : '认证创作者' }}
                    </span>
                  </div>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="workspace" @click="router.push('/')">
                  <span>返回工作台</span>
                </a-menu-item>
                <a-menu-item key="logout" danger @click="doLogout">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else class="auth-actions">
          <RouterLink to="/user/login">
            <a-button type="text" class="signin-link-btn">登录</a-button>
          </RouterLink>
          <RouterLink to="/user/register">
            <a-button type="primary" class="login-button">免费开始</a-button>
          </RouterLink>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, DownOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import {
  getGlobalChatGenMode,
  setGlobalChatGenMode,
  type ChatGenMode,
} from '@/utils/chatGenMode'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const selectedKeys = ref<string[]>([router.currentRoute.value.path])
const globalGenerationMode = ref<ChatGenMode>(getGlobalChatGenMode())

const generationModeOptions = [
  { label: '标准', value: 'classic' },
  { label: '工作流', value: 'workflow' },
]

router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

interface NavItem {
  key: string
  title: string
}

const originItems: NavItem[] = [
  { key: '/', title: '工作台' },
  { key: '/admin/userManage', title: '用户管理' },
  { key: '/admin/appManage', title: '应用管理' },
  { key: '/admin/chatManage', title: '对话管理' },
]

const filterMenus = (menus: NavItem[]) => {
  return menus.filter((menu) => {
    if (menu.key.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const displayUserName = computed(() => {
  const user = loginUserStore.loginUser
  const name = user?.userName?.trim()
  if (name && name !== '无名') return name
  return user?.userAccount || '创作者'
})

const menuItems = computed<NavItem[]>(() => filterMenus(originItems))

const handleMenuClick = (e: { key: string }) => {
  const key = e.key
  if (!key) return
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

const onGlobalModeChange = (value: string | number) => {
  const mode: ChatGenMode = value === 'workflow' ? 'workflow' : 'classic'
  globalGenerationMode.value = mode
  setGlobalChatGenMode(mode)
}
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  width: 100%;
  height: 60px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 24px;
  width: min(1380px, calc(100vw - 40px));
  height: 100%;
  margin: 0 auto;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
  text-decoration: none;
}

.brand-logo-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #EFF6FF 0%, #E0E7FF 100%);
  border: 1px solid rgba(37, 99, 235, 0.16);
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.1);
}

.brand-logo {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.brand-text {
  display: flex;
  align-items: center;
  gap: 7px;
}

.brand-name {
  color: #0F172A;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.025em;
}

.brand-highlight {
  color: #2563EB;
}

.brand-badge {
  padding: 2px 7px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2563EB;
  background: #EFF6FF;
  border: 1px solid rgba(37, 99, 235, 0.2);
  border-radius: 6px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  background: rgba(241, 245, 249, 0.7);
  border: 1px solid rgba(15, 23, 42, 0.05);
  border-radius: 11px;
}

.nav-link-btn {
  position: relative;
  height: 30px;
  padding: 0 13px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 500;
  transition: all 0.16s ease;
}

.nav-link-btn:hover {
  color: #0F172A;
  background: rgba(255, 255, 255, 0.6);
}

.nav-link-btn--active {
  color: #2563EB;
  background: #FFFFFF;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.mode-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 5px 3px 10px;
  background: #F1F5F9;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 999px;
}

.mode-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.mode-icon {
  color: #2563EB;
  font-size: 12px;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: #E2E8F0;
}

.user-profile-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border: 1px solid #E2E8F0;
  border-radius: 999px;
  background: #FFFFFF;
  color: #0F172A;
  cursor: pointer;
  box-shadow: var(--shadow-xs);
  transition: all 0.16s ease;
}

.user-profile-btn:hover {
  border-color: #CBD5E1;
  background: #F8FAFC;
  box-shadow: var(--shadow-sm);
}

.user-avatar {
  background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
  color: #FFFFFF;
  font-weight: 600;
  font-size: 13px;
}

.user-name {
  max-width: 110px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0F172A;
}

.role-chip {
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 700;
  color: #4F46E5;
  background: #EEF2FF;
  border-radius: 999px;
}

.user-caret {
  color: #64748B;
  font-size: 10px;
}

.menu-user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 0;
}

.menu-user-account {
  font-size: 13px;
  font-weight: 600;
  color: #0F172A;
}

.menu-role-tag {
  font-size: 11.5px;
  color: #64748B;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.signin-link-btn {
  color: #475569;
  font-size: 13.5px;
  font-weight: 500;
}

.login-button {
  height: 34px;
  padding: 0 16px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .header-inner {
    width: calc(100vw - 24px);
    gap: 12px;
  }

  .mode-pill {
    display: none;
  }
}
</style>
