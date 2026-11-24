<template>
  <div id="userLoginPage">
    <div class="auth-card">
      <!-- Left Brand Showcase -->
      <aside class="auth-showcase">
        <div class="showcase-grid" aria-hidden="true"></div>
        <div class="showcase-glow showcase-glow--top" aria-hidden="true"></div>
        <div class="showcase-glow showcase-glow--bottom" aria-hidden="true"></div>
        <div class="showcase-content">
          <div class="showcase-tag">
            <span class="tag-dot"></span>
            <span>Epiph-code Studio · v2.0</span>
          </div>

          <h1 class="showcase-title">探索 AI 驱动的<br />全栈构建工作流</h1>
          <p class="showcase-desc">
            登录后即可继续你的应用迭代工作流，管理生成的网页版本快照、配置推理模型并一键部署至公网。
          </p>

          <ul class="feature-bullets">
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>结构化 TODO 任务规划与依赖追踪</span>
            </li>
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>三层长对话上下文压缩与记忆管理</span>
            </li>
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>所见即所得的可视化局部点选修改</span>
            </li>
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>多版本不可变快照与稳定一键上线</span>
            </li>
          </ul>

          <div class="showcase-footer">
            <span class="showcase-ver">LANGCHAIN4J + LANGGRAPH4J ENGINE</span>
          </div>
        </div>
      </aside>

      <!-- Right Login Form -->
      <section class="auth-form-panel">
        <div class="panel-header">
          <span class="panel-kicker">WELCOME BACK</span>
          <h2 class="panel-title">登录工作台</h2>
          <p class="panel-desc">请输入你的平台账号与密码以进入 AI 应用构建台</p>
        </div>

        <a-form
          class="auth-form"
          :model="formState"
          name="login"
          autocomplete="off"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item
            label="账号"
            name="userAccount"
            :rules="[{ required: true, message: '请输入账号' }]"
          >
            <a-input
              v-model:value="formState.userAccount"
              size="large"
              placeholder="请输入账号"
              class="auth-input"
            >
              <template #prefix>
                <UserOutlined class="input-icon" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            label="密码"
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              size="large"
              placeholder="请输入密码（至少 8 位）"
              class="auth-input"
            >
              <template #prefix>
                <LockOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="form-options">
            <a class="forgot-link" @click.prevent="showForgotTip">忘记密码？</a>
            <div class="register-hint">
              <span class="hint-text">还没有账号？</span>
              <RouterLink to="/user/register" class="link-highlight">免费注册</RouterLink>
            </div>
          </div>

          <a-form-item class="submit-item">
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              class="submit-button"
              :loading="loading"
            >
              立即登录
            </a-button>
          </a-form-item>
        </a-form>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { UserOutlined, LockOutlined, CheckCircleFilled } from '@ant-design/icons-vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const loading = ref(false)
const router = useRouter()
const loginUserStore = useLoginUserStore()

const showForgotTip = () => {
  Modal.info({
    title: '忘记密码怎么办？',
    content:
      '请联系平台管理员（如 test 管理员账号）在顶部「用户管理」页面点击「重置密码」，重置后即可使用新密码登录。',
    okText: '我知道了',
  })
}

const handleSubmit = async (values: API.UserLoginRequest) => {
  loading.value = true
  try {
    const res = await userLogin(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功，欢迎回来')
      router.push({
        path: '/',
        replace: true,
      })
    } else {
      message.error('登录失败：' + res.data.message)
    }
  } catch {
    message.error('登录请求异常，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
#userLoginPage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
  padding: 20px 0;
}

.auth-card {
  display: grid;
  grid-template-columns: minmax(360px, 1.08fr) minmax(380px, 0.92fr);
  width: min(1020px, 100%);
  min-height: 570px;
  background: #FFFFFF;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 24px;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

/* Left Showcase */
.auth-showcase {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(145deg, #0F172A 0%, #1E1B4B 55%, #172554 100%);
  color: #F8FAFC;
  overflow: hidden;
}

.showcase-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
}

.showcase-glow {
  position: absolute;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}

.showcase-glow--top {
  top: -120px;
  left: -80px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.35) 0%, transparent 70%);
}

.showcase-glow--bottom {
  bottom: -120px;
  right: -80px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.35) 0%, transparent 70%);
}

.showcase-content {
  position: relative;
  z-index: 1;
}

.showcase-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 13px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(8px);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #93C5FD;
  margin-bottom: 24px;
}

.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #60A5FA;
  box-shadow: 0 0 8px #60A5FA;
}

.showcase-title {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.025em;
  color: #FFFFFF;
}

.showcase-desc {
  margin: 16px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #CBD5E1;
}

.feature-bullets {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 28px 0 0;
  padding: 0;
  list-style: none;
}

.feature-bullets li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  color: #F1F5F9;
}

.bullet-icon {
  color: #60A5FA;
  font-size: 15px;
}

.showcase-footer {
  margin-top: 34px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.showcase-ver {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #94A3B8;
}

/* Right Form */
.auth-form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  background: #FFFFFF;
}

.panel-kicker {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #2563EB;
}

.panel-title {
  margin: 6px 0 0;
  font-size: 26px;
  font-weight: 700;
  color: #0F172A;
  letter-spacing: -0.025em;
}

.panel-desc {
  margin: 6px 0 28px;
  font-size: 13.5px;
  color: #64748B;
}

.auth-form :deep(.ant-form-item-label > label) {
  font-size: 13px;
  font-weight: 600;
  color: #0F172A;
}

.auth-input {
  border-radius: 11px !important;
}

.input-icon {
  color: #94A3B8;
  font-size: 15px;
  margin-right: 4px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 24px;
  font-size: 13px;
}

.register-hint {
  display: flex;
  align-items: center;
  gap: 6px;
}

.forgot-link {
  color: #64748B;
  cursor: pointer;
}

.forgot-link:hover {
  color: #2563EB;
  text-decoration: underline;
}

.hint-text {
  color: #64748B;
}

.link-highlight {
  color: #2563EB;
  font-weight: 600;
}

.link-highlight:hover {
  color: #1D4ED8;
  text-decoration: underline;
}

.submit-button {
  width: 100%;
  height: 44px;
  border-radius: 11px;
  font-size: 14.5px;
  font-weight: 600;
}

@media (max-width: 820px) {
  .auth-card {
    grid-template-columns: 1fr;
  }

  .auth-showcase {
    display: none;
  }

  .auth-form-panel {
    padding: 32px 24px;
  }
}
</style>
