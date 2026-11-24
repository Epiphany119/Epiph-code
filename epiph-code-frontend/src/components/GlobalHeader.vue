<template>
  <header class="kaggle-topbar" :class="{ 'kaggle-topbar--compact': compact }">
    <div class="topbar-inner">
      <!-- Compact Brand (Shown on IDE chat page or mobile) -->
      <RouterLink to="/" class="mobile-brand" :class="{ 'always-show': compact }" aria-label="回到首页">
        <span class="brand-wordmark">epiph<span class="brand-accent">-code</span></span>
        <span class="studio-pill">STUDIO</span>
      </RouterLink>

      <!-- Kaggle-Style Global Search Pill (On non-chat pages) -->
      <div v-if="!compact" class="global-search-wrap">
        <SearchOutlined class="global-search-icon" />
        <input
          v-model="quickSearch"
          type="text"
          class="global-search-input"
          placeholder="搜索应用、输入灵感关键词，按回车直达构建台..."
          @keydown.enter="handleGlobalSearch"
        />
        <span class="search-kbd">/</span>
      </div>

      <!-- Right User & Auth Zone -->
      <div class="topbar-right">
        <div v-if="compact" class="compact-mode-pill" title="切换全局代码生成引擎">
          <ThunderboltOutlined class="compact-mode-icon" />
          <a-segmented
            v-model:value="globalGenerationMode"
            :options="generationModeOptions"
            size="small"
            @change="onGlobalModeChange"
          />
        </div>

        <div v-if="loginUserStore.loginUser.id" class="user-zone">
          <a-dropdown trigger="click" placement="bottomRight">
            <button type="button" class="user-profile-btn">
              <a-avatar
                :size="32"
                :src="loginUserStore.loginUser.userAvatar"
                class="user-avatar"
              >
                {{ displayUserName.charAt(0).toUpperCase() }}
              </a-avatar>
              <span class="user-name">{{ displayUserName }}</span>
              <span v-if="loginUserStore.loginUser.userRole === 'admin'" class="role-badge">Admin</span>
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
                <a-menu-item key="home" @click="router.push('/')">
                  <span>返回首页工作台</span>
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
            <a-button type="default" class="signin-btn">登录</a-button>
          </RouterLink>
          <RouterLink to="/user/register">
            <a-button type="primary" class="register-btn">注册</a-button>
          </RouterLink>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import {
  LogoutOutlined,
  DownOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import {
  getGlobalChatGenMode,
  setGlobalChatGenMode,
  type ChatGenMode,
} from '@/utils/chatGenMode'

withDefaults(defineProps<{ compact?: boolean }>(), {
  compact: false,
})

const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()
const quickSearch = ref('')

const globalGenerationMode = ref<ChatGenMode>(getGlobalChatGenMode())
const generationModeOptions = [
  { label: '标准', value: 'classic' },
  { label: '工作流', value: 'workflow' },
]

const displayUserName = computed(() => {
  const user = loginUserStore.loginUser
  const name = user?.userName?.trim()
  if (name && name !== '无名') return name
  return user?.userAccount || '创作者'
})

const handleGlobalSearch = async () => {
  const kw = quickSearch.value.trim()
  if (!kw) return
  if (route.path !== '/') {
    await router.push({ path: '/', query: { q: kw } })
  } else {
    router.replace({ path: '/', query: { q: kw } })
    document.getElementById('featuredGallerySection')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
.kaggle-topbar {
  position: sticky;
  top: 0;
  z-index: 90;
  width: 100%;
  height: 64px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid #E8EAED;
}

.kaggle-topbar--compact {
  height: 56px;
}

.topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: 100%;
  height: 100%;
  padding: 0 32px;
}

.kaggle-topbar--compact .topbar-inner {
  padding: 0 18px;
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.mobile-brand.always-show {
  display: inline-flex;
}

.brand-wordmark {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #20BEFF;
}

.brand-accent {
  color: #202124;
}

.studio-pill {
  padding: 2px 7px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #008ABC;
  background: #E6F8FF;
  border-radius: 6px;
}

/* Kaggle Wide Search Pill */
.global-search-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  width: min(560px, 100%);
  height: 42px;
  padding: 0 16px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.global-search-wrap:focus-within {
  border-color: #202124;
  box-shadow: 0 2px 8px rgba(32, 33, 36, 0.08);
}

.global-search-icon {
  color: #5F6368;
  font-size: 15px;
}

.global-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13.5px;
  color: #202124;
}

.global-search-input::placeholder {
  color: #80868B;
}

.search-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  font-size: 11px;
  font-weight: 600;
  color: #80868B;
  border: 1px solid #DADCE0;
  border-radius: 6px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.compact-mode-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 4px 2px 10px;
  background: #F1F3F4;
  border-radius: 999px;
}

.compact-mode-icon {
  color: #008ABC;
  font-size: 12px;
}

.user-profile-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 12px 3px 4px;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  background: #FFFFFF;
  color: #202124;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.user-profile-btn:hover {
  background: #F8F9FA;
  border-color: #202124;
}

.user-avatar {
  background: #202124;
  color: #FFFFFF;
  font-weight: 700;
  font-size: 13px;
}

.user-name {
  max-width: 110px;
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #202124;
}

.role-badge {
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 700;
  color: #008ABC;
  background: #E6F8FF;
  border-radius: 999px;
}

.user-caret {
  color: #5F6368;
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
  font-weight: 700;
  color: #202124;
}

.menu-role-tag {
  font-size: 11.5px;
  color: #5F6368;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.signin-btn,
.register-btn {
  height: 36px;
  padding: 0 20px;
  font-size: 13.5px;
}

@media (max-width: 960px) {
  .topbar-inner {
    padding: 0 16px;
  }

  .mobile-brand {
    display: inline-flex;
  }

  .global-search-wrap {
    display: none;
  }
}
</style>
