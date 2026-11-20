<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

const theme = {
  token: {
    colorPrimary: '#3B7DED',
    colorInfo: '#3B7DED',
    colorSuccess: '#16A34A',
    colorWarning: '#F59E0B',
    colorError: '#EF4444',
    colorText: '#1E293B',
    colorTextSecondary: '#475569',
    colorTextTertiary: '#94A3B8',
    colorBgBase: '#F6F9FE',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorBgLayout: '#F6F9FE',
    colorBorder: '#E6ECF5',
    colorBorderSecondary: '#EFF4FB',
    borderRadius: 14,
    borderRadiusLG: 22,
    borderRadiusSM: 10,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif",
    fontSize: 14,
    controlHeight: 38,
  },
  components: {
    Button: {
      colorPrimary: '#3B7DED',
      colorPrimaryHover: '#2B6CB0',
      colorPrimaryActive: '#2563EB',
      borderRadius: 999,
    },
    Table: {
      headerBg: '#F6F9FE',
      headerColor: '#475569',
      rowHoverBg: '#EFF5FF',
      borderColor: '#E6ECF5',
      borderRadius: 16,
    },
    Menu: {
      itemColor: '#475569',
      itemSelectedColor: '#2563EB',
      itemSelectedBg: '#EBF3FE',
      horizontalItemHoverBg: '#F0F6FF',
    },
    Input: {
      activeBorderColor: '#3B7DED',
      hoverBorderColor: '#93C5FD',
      colorBgContainer: '#FFFFFF',
    },
    Card: {
      colorBgContainer: '#FFFFFF',
      colorBorderSecondary: '#E6ECF5',
    },
    Modal: {
      colorBgElevated: '#FFFFFF',
      borderRadiusLG: 24,
    },
    Tag: {
      borderRadiusSM: 999,
    },
  },
}
</script>

<template>
  <a-config-provider :theme="theme">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
:root {
  --bg-page: #F6F9FE;
  --bg-surface: #EEF4FC;
  --bg-card: #FFFFFF;
  --bg-muted: #F1F5FB;
  --line: #E6ECF5;
  --line-strong: #CBD5E1;
  --text-main: #1E293B;
  --text-muted: #526075;
  --text-dim: #94A3B8;
  --primary: #3B7DED;
  --primary-hover: #2563EB;
  --primary-light: #EBF3FE;
  --primary-glow: rgba(59, 125, 237, 0.16);
  --gemini-gradient: linear-gradient(135deg, #4285F4 0%, #6E8CF8 55%, #A37CE8 100%);
  --success: #16A34A;
  --warning: #F59E0B;
  --danger: #EF4444;
  --shadow-sm: 0 2px 8px rgba(30, 41, 59, 0.04);
  --shadow-md: 0 6px 20px rgba(30, 41, 59, 0.055);
  --shadow-lg: 0 12px 32px rgba(30, 41, 59, 0.08);
  --shadow-xl: 0 20px 48px rgba(30, 41, 59, 0.12);
}

* {
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

html,
body,
#app {
  min-height: 100%;
  margin: 0;
  padding: 0;
}

body {
  color: var(--text-main);
  background-color: var(--bg-page);
  background-image:
    radial-gradient(960px circle at 15% -80px, rgba(66, 133, 244, 0.07), transparent 65%),
    radial-gradient(820px circle at 88% 30px, rgba(163, 124, 232, 0.05), transparent 60%);
  background-attachment: fixed;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue',
    'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

::selection {
  background: #DBEAFE;
  color: #1D4ED8;
}

a {
  color: inherit;
  text-decoration: none;
  transition: color 0.15s ease;
}

button,
input,
textarea,
select {
  font-family: inherit;
}

/* Custom Scrollbars */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #D5DFEC;
  border-radius: 999px;
}

::-webkit-scrollbar-thumb:hover {
  background: #B8C7DB;
}

/* Ant Design Global Soft Gemini Polish */
.ant-btn {
  font-weight: 500;
  border-radius: 999px;
  box-shadow: none;
  transition: all 0.2s cubic-bezier(0.2, 0, 0, 1);
}

.ant-btn-primary {
  background: linear-gradient(135deg, #4285F4 0%, #3B7DED 100%) !important;
  border-color: transparent !important;
  color: #FFFFFF !important;
  box-shadow: 0 3px 10px rgba(59, 125, 237, 0.22) !important;
}

.ant-btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #3B7DED 0%, #2563EB 100%) !important;
  box-shadow: 0 5px 14px rgba(59, 125, 237, 0.3) !important;
  transform: translateY(-1px);
}

.ant-btn-primary:disabled {
  background: #E2E8F0 !important;
  color: #94A3B8 !important;
  box-shadow: none !important;
}

.ant-btn-default {
  border-color: var(--line) !important;
  background: #FFFFFF !important;
  color: var(--text-main) !important;
}

.ant-btn-default:hover:not(:disabled) {
  border-color: #BFDBFE !important;
  color: #2563EB !important;
  background: #F4F8FE !important;
}

.ant-input,
.ant-input-affix-wrapper,
.ant-select-selector {
  border-color: var(--line) !important;
  border-radius: 14px !important;
  transition: all 0.2s ease !important;
}

.ant-input::placeholder,
.ant-input-affix-wrapper input::placeholder {
  color: #94A3B8 !important;
}

.ant-input:focus,
.ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: #3B7DED !important;
  box-shadow: 0 0 0 3px rgba(59, 125, 237, 0.12) !important;
}

.ant-card {
  border-radius: 22px;
  border: 1px solid var(--line) !important;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.ant-modal-content {
  border-radius: 24px !important;
  box-shadow: var(--shadow-xl) !important;
  border: 1px solid var(--line);
}

.ant-segmented {
  background: #EFF4FB !important;
  padding: 3px;
  border-radius: 999px;
}

.ant-segmented-item-selected {
  background: #FFFFFF !important;
  color: #2563EB !important;
  box-shadow: 0 2px 6px rgba(30, 41, 59, 0.08) !important;
  font-weight: 600;
}

.ant-tag {
  border: none;
  border-radius: 999px;
  font-weight: 500;
}
</style>
