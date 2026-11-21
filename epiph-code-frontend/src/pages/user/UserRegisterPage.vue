<template>
  <div id="userRegisterPage">
    <div class="auth-card">
      <!-- Left Brand Showcase -->
      <aside class="auth-showcase">
        <div class="showcase-glow"></div>
        <div class="showcase-content">
          <div class="showcase-tag">
            <span class="tag-dot"></span>
            <span>Epiph-code Studio</span>
          </div>

          <h1 class="showcase-title">开启你的全栈<br />AI 创造之旅</h1>
          <p class="showcase-desc">
            免费注册账号，即可体验 LangChain4j + LangGraph4j 双引擎驱动的下一代智能代码生成系统。
          </p>

          <ul class="feature-bullets">
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>多技术栈：HTML、原生多文件与 Vue 工程项目</span>
            </li>
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>全透明生成：思考过程、工具调用与 TODO 计划实时可视</span>
            </li>
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>长对话不迷失：结构化摘要与三层上下文智能微缩</span>
            </li>
            <li>
              <CheckCircleFilled class="bullet-icon" />
              <span>即时发布：一键生成专属线上公网访问链接</span>
            </li>
          </ul>

          <div class="showcase-footer">
            <span class="showcase-ver">CREATE ACCOUNT / JOIN CREATORS</span>
          </div>
        </div>
      </aside>

      <!-- Right Register Form -->
      <section class="auth-form-panel">
        <div class="panel-header">
          <span class="panel-kicker">GET STARTED</span>
          <h2 class="panel-title">创建新账号</h2>
          <p class="panel-desc">只需设置账号与密码即可免费开启创作体验</p>
        </div>

        <a-form
          class="auth-form"
          :model="formState"
          name="register"
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
              placeholder="请输入账号 (字母或数字)"
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

          <a-form-item
            label="确认密码"
            name="checkPassword"
            :rules="[
              { required: true, message: '请确认密码' },
              { min: 8, message: '密码不能小于 8 位' },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              size="large"
              placeholder="请再次输入密码"
              class="auth-input"
            >
              <template #prefix>
                <SafetyOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="form-options">
            <span class="hint-text">已有平台账号？</span>
            <RouterLink to="/user/login" class="link-highlight">直接登录</RouterLink>
          </div>

          <a-form-item class="submit-item">
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              class="submit-button"
              :loading="loading"
            >
              注 册
            </a-button>
          </a-form-item>
        </a-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, SafetyOutlined, CheckCircleFilled } from '@ant-design/icons-vue'

const router = useRouter()
const loading = ref(false)

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  loading.value = true
  try {
    const res = await userRegister(values)
    if (res.data.code === 0) {
      message.success('注册成功，请使用新账号登录')
      router.push({
        path: '/user/login',
        replace: true,
      })
    } else {
      message.error('注册失败：' + res.data.message)
    }
  } catch {
    message.error('注册请求异常，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
#userRegisterPage {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
  padding: 24px 0;
}

.auth-card {
  display: grid;
  grid-template-columns: minmax(360px, 1.1fr) minmax(380px, 0.9fr);
  width: min(1000px, 100%);
  min-height: 590px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 28px;
  box-shadow: none;
  overflow: hidden;
}

/* Left Showcase */
.auth-showcase {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  background: #F0F4F9;
  border-right: 1px solid #E3E3E3;
  color: #1F1F1F;
  overflow: hidden;
}

.showcase-glow {
  position: absolute;
  top: -100px;
  left: -100px;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(11, 87, 208, 0.07) 0%, transparent 70%);
  pointer-events: none;
}

.showcase-content {
  position: relative;
  z-index: 1;
}

.showcase-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: #FFFFFF;
  border: 1px solid #DADCE0;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #0B57D0;
  margin-bottom: 24px;
}

.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0B57D0;
}

.showcase-title {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  line-height: 1.28;
  letter-spacing: -0.02em;
  color: #1F1F1F;
}

.showcase-desc {
  margin: 16px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #444746;
}

.feature-bullets {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 32px 0 0;
  padding: 0;
  list-style: none;
}

.feature-bullets li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #1F1F1F;
}

.bullet-icon {
  color: #0B57D0;
  font-size: 15px;
}

.showcase-footer {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #DADCE0;
}

.showcase-ver {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #747775;
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
  color: #0B57D0;
}

.panel-title {
  margin: 6px 0 0;
  font-size: 26px;
  font-weight: 600;
  color: #1F1F1F;
  letter-spacing: -0.02em;
}

.panel-desc {
  margin: 6px 0 28px;
  font-size: 14px;
  color: #444746;
}

.auth-form :deep(.ant-form-item-label > label) {
  font-size: 13px;
  font-weight: 600;
  color: #1F1F1F;
}

.auth-input {
  border-radius: 14px !important;
}

.input-icon {
  color: #94A3B8;
  font-size: 15px;
  margin-right: 4px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-bottom: 24px;
  font-size: 13px;
}

.hint-text {
  color: #444746;
}

.link-highlight {
  color: #0B57D0;
  font-weight: 600;
}

.link-highlight:hover {
  color: #0B57D0;
  text-decoration: underline;
}

.submit-button {
  width: 100%;
  height: 44px;
  border-radius: 999px;
  font-size: 15px;
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
