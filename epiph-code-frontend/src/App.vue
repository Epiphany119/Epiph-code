<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

const theme = {
  token: {
    colorPrimary: '#6366F1',
    colorInfo: '#3B82F6',
    colorSuccess: '#10B981',
    colorWarning: '#F59E0B',
    colorError: '#EF4444',
    colorText: '#0F172A',
    colorTextSecondary: '#475569',
    colorTextTertiary: '#94A3B8',
    colorBgBase: '#F8FAFC',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorBgLayout: '#F8FAFC',
    colorBorder: '#E2E8F0',
    colorBorderSecondary: '#F1F5F9',
    borderRadius: 10,
    borderRadiusLG: 14,
    borderRadiusSM: 6,
    fontFamily:
      "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
    fontSize: 14,
    controlHeight: 38,
  },
  components: {
    Button: {
      colorPrimary: '#6366F1',
      colorPrimaryHover: '#4F46E5',
      colorPrimaryActive: '#4338CA',
      borderRadius: 8,
    },
    Table: {
      headerBg: '#F8FAFC',
      headerColor: '#475569',
      rowHoverBg: '#F8FAFC',
      borderColor: '#E2E8F0',
      borderRadius: 10,
    },
    Menu: {
      itemColor: '#475569',
      itemSelectedColor: '#6366F1',
      itemSelectedBg: 'rgba(99, 102, 241, 0.08)',
      horizontalItemHoverBg: 'rgba(99, 102, 241, 0.04)',
    },
    Input: {
      activeBorderColor: '#6366F1',
      hoverBorderColor: '#818CF8',
      colorBgContainer: '#FFFFFF',
    },
    Card: {
      colorBgContainer: '#FFFFFF',
      colorBorderSecondary: '#E2E8F0',
    },
    Modal: {
      colorBgElevated: '#FFFFFF',
      borderRadiusLG: 16,
    },
    Tag: {
      borderRadiusSM: 6,
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
  --bg-page: #F8FAFC;
  --bg-card: #FFFFFF;
  --bg-muted: #F1F5F9;
  --line: #E2E8F0;
  --line-strong: #CBD5E1;
  --text-main: #0F172A;
  --text-muted: #475569;
  --text-dim: #94A3B8;
  --primary: #6366F1;
  --primary-hover: #4F46E5;
  --primary-light: rgba(99, 102, 241, 0.08);
  --primary-glow: rgba(99, 102, 241, 0.25);
  --accent-cyan: #06B6D4;
  --accent-purple: #8B5CF6;
  --success: #10B981;
  --warning: #F59E0B;
  --danger: #EF4444;
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.04);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
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
    radial-gradient(1000px circle at 50% -80px, rgba(99, 102, 241, 0.07), transparent 70%),
    radial-gradient(800px circle at 90% 150px, rgba(59, 130, 246, 0.05), transparent 60%),
    radial-gradient(700px circle at 10% 400px, rgba(139, 92, 246, 0.04), transparent 60%);
  background-attachment: fixed;
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

::selection {
  background: rgba(99, 102, 241, 0.2);
  color: #4338CA;
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
  background: #CBD5E1;
  border-radius: 999px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94A3B8;
}

/* Ant Design Global Polish */
.ant-btn {
  font-weight: 500;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.ant-btn-primary {
  background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%) !important;
  border-color: transparent !important;
  color: #FFFFFF !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.28) !important;
}

.ant-btn-primary:hover {
  background: linear-gradient(135deg, #4F46E5 0%, #4338CA 100%) !important;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.38) !important;
  transform: translateY(-1px);
}

.ant-btn-primary:active {
  transform: translateY(0);
}

.ant-btn-default {
  border-color: var(--line) !important;
  background: #FFFFFF !important;
  color: var(--text-main) !important;
}

.ant-btn-default:hover {
  border-color: #818CF8 !important;
  color: #4F46E5 !important;
  background: #F8FAFC !important;
}

.ant-input,
.ant-input-affix-wrapper,
.ant-select-selector {
  border-color: var(--line) !important;
  transition: all 0.2s ease !important;
}

.ant-input::placeholder,
.ant-input-affix-wrapper input::placeholder {
  color: #94A3B8 !important;
}

.ant-input:focus,
.ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: #6366F1 !important;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12) !important;
}

.ant-card {
  border-radius: 12px;
  border: 1px solid var(--line) !important;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.ant-modal-content {
  border-radius: 16px !important;
  box-shadow: var(--shadow-xl) !important;
  border: 1px solid var(--line);
}

.ant-segmented {
  background: #F1F5F9 !important;
  padding: 3px;
  border-radius: 8px;
}

.ant-segmented-item-selected {
  background: #FFFFFF !important;
  color: #4F46E5 !important;
  box-shadow: var(--shadow-sm) !important;
  font-weight: 600;
}

.ant-tag {
  border: none;
  font-weight: 500;
}
</style>
