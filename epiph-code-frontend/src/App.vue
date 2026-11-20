<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

const theme = {
  token: {
    colorPrimary: '#0B57D0',
    colorInfo: '#0B57D0',
    colorSuccess: '#146C2E',
    colorWarning: '#B06000',
    colorError: '#B3261E',
    colorText: '#1F1F1F',
    colorTextSecondary: '#444746',
    colorTextTertiary: '#747775',
    colorBgBase: '#FFFFFF',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorBgLayout: '#FFFFFF',
    colorBorder: '#DADCE0',
    colorBorderSecondary: '#E3E3E3',
    borderRadius: 12,
    borderRadiusLG: 20,
    borderRadiusSM: 8,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif",
    fontSize: 14,
    controlHeight: 38,
  },
  components: {
    Button: {
      colorPrimary: '#0B57D0',
      colorPrimaryHover: '#0842A0',
      colorPrimaryActive: '#062E6F',
      borderRadius: 999,
    },
    Table: {
      headerBg: '#F8F9FA',
      headerColor: '#444746',
      rowHoverBg: '#F0F4F9',
      borderColor: '#E3E3E3',
      borderRadius: 16,
    },
    Menu: {
      itemColor: '#444746',
      itemSelectedColor: '#0B57D0',
      itemSelectedBg: '#E8F0FE',
      horizontalItemHoverBg: '#F0F4F9',
    },
    Input: {
      activeBorderColor: '#0B57D0',
      hoverBorderColor: '#7CACF8',
      colorBgContainer: '#FFFFFF',
    },
    Card: {
      colorBgContainer: '#FFFFFF',
      colorBorderSecondary: '#DADCE0',
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
  --bg-page: #FFFFFF;
  --bg-surface: #F0F4F9;
  --bg-card: #FFFFFF;
  --bg-muted: #F8F9FA;
  --line: #DADCE0;
  --line-soft: #E3E3E3;
  --text-main: #1F1F1F;
  --text-muted: #444746;
  --text-dim: #747775;
  --primary: #0B57D0;
  --primary-hover: #0842A0;
  --primary-light: #E8F0FE;
  --primary-glow: rgba(11, 87, 208, 0.14);
  --success: #146C2E;
  --warning: #B06000;
  --danger: #B3261E;
  --shadow-sm: none;
  --shadow-md: 0 1px 3px rgba(60, 64, 67, 0.08), 0 4px 12px rgba(60, 64, 67, 0.05);
  --shadow-lg: 0 4px 16px rgba(60, 64, 67, 0.1);
  --shadow-xl: 0 12px 32px rgba(60, 64, 67, 0.14);
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
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue',
    'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

::selection {
  background: #D3E3FD;
  color: #062E6F;
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
  background: #DADCE0;
  border-radius: 999px;
}

::-webkit-scrollbar-thumb:hover {
  background: #BDC1C6;
}

/* Ant Design Material 3 Polish */
.ant-btn {
  font-weight: 500;
  border-radius: 999px;
  box-shadow: none !important;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.ant-btn-primary {
  background: #0B57D0 !important;
  border-color: transparent !important;
  color: #FFFFFF !important;
}

.ant-btn-primary:hover:not(:disabled) {
  background: #0842A0 !important;
}

.ant-btn-primary:disabled {
  background: #E3E3E3 !important;
  color: #8E918F !important;
}

.ant-btn-default {
  border-color: #DADCE0 !important;
  background: #FFFFFF !important;
  color: #1F1F1F !important;
}

.ant-btn-default:hover:not(:disabled) {
  border-color: #A8C7FA !important;
  color: #0B57D0 !important;
  background: #F0F4F9 !important;
}

.ant-input,
.ant-input-affix-wrapper,
.ant-select-selector {
  border-color: #DADCE0 !important;
  border-radius: 12px !important;
  transition: border-color 0.18s ease, box-shadow 0.18s ease !important;
}

.ant-input::placeholder,
.ant-input-affix-wrapper input::placeholder {
  color: #747775 !important;
}

.ant-input:focus,
.ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: #0B57D0 !important;
  box-shadow: 0 0 0 2px rgba(11, 87, 208, 0.12) !important;
}

.ant-card {
  border-radius: 20px;
  border: 1px solid #DADCE0 !important;
  box-shadow: none !important;
}

.ant-modal-content {
  border-radius: 24px !important;
  box-shadow: var(--shadow-xl) !important;
  border: 1px solid #E3E3E3;
}

.ant-segmented {
  background: #F0F4F9 !important;
  padding: 3px;
  border-radius: 999px;
}

.ant-segmented-item-selected {
  background: #FFFFFF !important;
  color: #0B57D0 !important;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.12) !important;
  font-weight: 600;
}

.ant-tag {
  border: none;
  border-radius: 999px;
  font-weight: 500;
}
</style>
