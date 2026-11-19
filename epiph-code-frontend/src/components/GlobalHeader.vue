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
          {{ item?.title }}
        </button>
      </nav>

      <!-- Right Tools -->
      <div class="header-tools">
        <!-- Global Generation Mode Switcher -->
        <div class="mode-pill" title="切换全局生成模式">
          <ThunderboltOutlined class="mode-icon" />
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
                :size="32"
                :src="loginUserStore.loginUser.userAvatar"
                class="user-avatar"
              >
                {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="user-name">{{ loginUserStore.loginUser.userName ?? '创作者' }}</span>
              <DownOutlined class="user-caret" />
            </button>
            <template #overlay>
              <a-menu class="user-menu-dropdown">
                <a-menu-item key="user" disabled>
                  <span class="menu-role-tag">角色: {{ loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '创作者' }}</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" danger @click="doLogout">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <RouterLink v-else to="/user/login">
          <a-button type="primary" class="login-button">
            登录 / 注册
          </a-button>
        </RouterLink>
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
  height: 64px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.02);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  width: min(1380px, calc(100vw - 48px));
  height: 100%;
  margin: 0 auto;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
  text-decoration: none;
}

.brand-logo-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
}

.brand-logo {
  width: 38px;
  height: 38px;
  object-fit: contain;
}

.brand-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-name {
  color: #0F172A;
  font-size: 18px;
  font-weight: 750;
  letter-spacing: -0.03em;
}

.brand-highlight {
  color: #6366F1;
}

.brand-badge {
  padding: 2px 7px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #6366F1;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 6px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 100%;
}

.nav-link-btn {
  position: relative;
  height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-link-btn:hover {
  color: #0F172A;
  background: rgba(241, 245, 249, 0.9);
}

.nav-link-btn--active {
  color: #4F46E5;
  background: rgba(99, 102, 241, 0.08);
  font-weight: 600;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
}

.mode-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 6px;
  background: #F1F5F9;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
}

.mode-icon {
  color: #6366F1;
  font-size: 13px;
  margin-left: 4px;
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
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.user-profile-btn:hover {
  border-color: #CBD5E1;
  background: #F8FAFC;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.user-avatar {
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: #FFFFFF;
  font-weight: 600;
}

.user-name {
  max-width: 110px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1E293B;
}

.user-caret {
  color: #94A3B8;
  font-size: 10px;
}

.menu-role-tag {
  font-size: 12px;
  color: #64748B;
}

.login-button {
  height: 36px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .header-inner {
    width: calc(100vw - 32px);
    gap: 16px;
  }

  .mode-pill {
    display: none;
  }

  .header-divider {
    display: none;
  }

  .brand-badge {
    display: none;
  }

  .user-name {
    display: none;
  }
}
</style>
