<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

const theme = {
  token: {
    colorPrimary: '#2563EB',
    colorInfo: '#2563EB',
    colorSuccess: '#059669',
    colorWarning: '#D97706',
    colorError: '#DC2626',
    colorText: '#0F172A',
    colorTextSecondary: '#475569',
    colorTextTertiary: '#64748B',
    colorBgBase: '#FFFFFF',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorBgLayout: '#F8FAFC',
    colorBorder: '#E2E8F0',
    colorBorderSecondary: '#F1F5F9',
    borderRadius: 10,
    borderRadiusLG: 16,
    borderRadiusSM: 8,
    fontFamily:
      "'Inter', -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif",
    fontSize: 14,
    controlHeight: 38,
  },
  components: {
    Button: {
      colorPrimary: '#2563EB',
      colorPrimaryHover: '#1D4ED8',
      colorPrimaryActive: '#1E40AF',
      borderRadius: 10,
      fontWeight: 500,
    },
    Table: {
      headerBg: '#F8FAFC',
      headerColor: '#475569',
      rowHoverBg: '#F8FAFC',
      borderColor: '#F1F5F9',
      borderRadius: 14,
    },
    Menu: {
      itemColor: '#475569',
      itemSelectedColor: '#2563EB',
      itemSelectedBg: '#EFF6FF',
      horizontalItemHoverBg: '#F1F5F9',
    },
    Input: {
      activeBorderColor: '#2563EB',
      hoverBorderColor: '#93C5FD',
      colorBgContainer: '#FFFFFF',
    },
    Card: {
      colorBgContainer: '#FFFFFF',
      colorBorderSecondary: '#E2E8F0',
    },
    Modal: {
      colorBgElevated: '#FFFFFF',
      borderRadiusLG: 20,
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
  --bg-page: #F8FAFC;
  --bg-surface: #F1F5F9;
  --bg-card: #FFFFFF;
  --bg-muted: #F8FAFC;
  --line: #E2E8F0;
  --line-soft: #F1F5F9;
  --text-main: #0F172A;
  --text-muted: #475569;
  --text-dim: #64748B;
  --primary: #2563EB;
  --primary-hover: #1D4ED8;
  --primary-light: #EFF6FF;
  --primary-glow: rgba(37, 99, 235, 0.14);
  --accent-indigo: #4F46E5;
  --success: #059669;
  --warning: #D97706;
  --danger: #DC2626;
  --shadow-xs: 0 1px 2px rgba(15, 23, 42, 0.04);
  --shadow-sm: 0 1px 3px rgba(15, 23, 42, 0.05), 0 1px 2px rgba(15, 23, 42, 0.03);
  --shadow-md: 0 4px 12px -2px rgba(15, 23, 42, 0.06), 0 2px 6px -1px rgba(15, 23, 42, 0.04);
  --shadow-lg: 0 12px 28px -6px rgba(15, 23, 42, 0.08), 0 4px 12px -2px rgba(15, 23, 42, 0.04);
  --shadow-xl: 0 20px 44px -10px rgba(15, 23, 42, 0.14), 0 8px 20px -4px rgba(15, 23, 42, 0.06);
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
    radial-gradient(at 15% 0%, rgba(37, 99, 235, 0.055) 0px, transparent 50%),
    radial-gradient(at 85% 0%, rgba(79, 70, 229, 0.05) 0px, transparent 50%);
  background-attachment: fixed;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC',
    'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

::selection {
  background: #DBEAFE;
  color: #1E3A8A;
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
  background: rgba(148, 163, 184, 0.45);
  border-radius: 999px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 116, 139, 0.65);
}

/* Ant Design Product Polish */
.ant-btn {
  font-weight: 500;
  border-radius: 10px;
  box-shadow: none !important;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
}

.ant-btn-primary {
  background: linear-gradient(180deg, #3B82F6 0%, #2563EB 100%) !important;
  border: 1px solid #1D4ED8 !important;
  color: #FFFFFF !important;
  box-shadow: 0 1px 2px rgba(37, 99, 235, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.18) !important;
}

.ant-btn-primary:hover:not(:disabled) {
  background: linear-gradient(180deg, #2563EB 0%, #1D4ED8 100%) !important;
  border-color: #1E40AF !important;
  transform: translateY(-0.5px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
}

.ant-btn-primary.ant-btn-background-ghost {
  background: rgba(37, 99, 235, 0.05) !important;
  border: 1px solid rgba(37, 99, 235, 0.35) !important;
  color: #2563EB !important;
  box-shadow: none !important;
}

.ant-btn-primary.ant-btn-background-ghost:hover:not(:disabled) {
  background: rgba(37, 99, 235, 0.1) !important;
  border-color: #2563EB !important;
  color: #1D4ED8 !important;
}

.ant-btn-primary:disabled {
  background: #E2E8F0 !important;
  border-color: #E2E8F0 !important;
  color: #94A3B8 !important;
  box-shadow: none !important;
}

.ant-btn-default {
  border-color: #E2E8F0 !important;
  background: #FFFFFF !important;
  color: #1E293B !important;
  box-shadow: var(--shadow-xs) !important;
}

.ant-btn-default:hover:not(:disabled) {
  border-color: #CBD5E1 !important;
  color: #0F172A !important;
  background: #F8FAFC !important;
}

.ant-input,
.ant-input-affix-wrapper,
.ant-select-selector,
.ant-input-number {
  border-color: #E2E8F0 !important;
  border-radius: 10px !important;
  transition: border-color 0.18s ease, box-shadow 0.18s ease !important;
}

.ant-input::placeholder,
.ant-input-affix-wrapper input::placeholder {
  color: #94A3B8 !important;
}

.ant-input:focus,
.ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: #2563EB !important;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12) !important;
}

.ant-card {
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08) !important;
  box-shadow: var(--shadow-sm) !important;
}

.ant-modal-content {
  border-radius: 20px !important;
  box-shadow: var(--shadow-xl) !important;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.ant-segmented {
  background: #F1F5F9 !important;
  padding: 3px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.05);
}

.ant-segmented-item {
  border-radius: 999px !important;
}

.ant-segmented-item-selected {
  background: #FFFFFF !important;
  color: #2563EB !important;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1) !important;
  font-weight: 600;
}

.ant-tag {
  border: none;
  border-radius: 999px;
  font-weight: 500;
}
</style>
