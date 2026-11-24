<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

const theme = {
  token: {
    colorPrimary: '#202124',
    colorInfo: '#20BEFF',
    colorSuccess: '#1E8E3E',
    colorWarning: '#F9AB00',
    colorError: '#D93025',
    colorText: '#202124',
    colorTextSecondary: '#5F6368',
    colorTextTertiary: '#80868B',
    colorBgBase: '#FFFFFF',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorBgLayout: '#FFFFFF',
    colorBorder: '#DADCE0',
    colorBorderSecondary: '#E8EAED',
    borderRadius: 12,
    borderRadiusLG: 16,
    borderRadiusSM: 8,
    fontFamily:
      "'Inter', -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif",
    fontSize: 14,
    controlHeight: 38,
  },
  components: {
    Button: {
      colorPrimary: '#202124',
      colorPrimaryHover: '#3C4043',
      colorPrimaryActive: '#000000',
      borderRadius: 999,
      fontWeight: 600,
    },
    Table: {
      headerBg: '#F8F9FA',
      headerColor: '#5F6368',
      rowHoverBg: '#F8F9FA',
      borderColor: '#E8EAED',
      borderRadius: 12,
    },
    Menu: {
      itemColor: '#3C4043',
      itemSelectedColor: '#202124',
      itemSelectedBg: '#F1F3F4',
      horizontalItemHoverBg: '#F1F3F4',
    },
    Input: {
      activeBorderColor: '#202124',
      hoverBorderColor: '#80868B',
      colorBgContainer: '#FFFFFF',
    },
    Card: {
      colorBgContainer: '#FFFFFF',
      colorBorderSecondary: '#DADCE0',
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
  --kg-bg: #FFFFFF;
  --kg-surface: #F8F9FA;
  --kg-surface-hover: #F1F3F4;
  --kg-ink: #202124;
  --kg-ink-secondary: #5F6368;
  --kg-ink-muted: #80868B;
  --kg-border: #DADCE0;
  --kg-border-light: #E8EAED;
  --kg-cyan: #20BEFF;
  --kg-cyan-dark: #008ABC;
  --kg-cyan-soft: #E6F8FF;
  --kg-green: #1E8E3E;
  --kg-amber: #F9AB00;
  --kg-red: #D93025;
  --kg-shadow-sm: 0 1px 2px rgba(32, 33, 36, 0.06);
  --kg-shadow-md: 0 2px 8px rgba(32, 33, 36, 0.08), 0 1px 2px rgba(32, 33, 36, 0.04);
  --kg-shadow-lg: 0 8px 24px rgba(32, 33, 36, 0.12), 0 2px 6px rgba(32, 33, 36, 0.06);
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
  background-color: var(--kg-bg);
}

body {
  color: var(--kg-ink);
  background-color: var(--kg-bg);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC',
    'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

::selection {
  background: #D2F4FF;
  color: #202124;
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

/* Kaggle High-Contrast Pill Controls */
.ant-btn {
  font-weight: 600;
  border-radius: 999px;
  box-shadow: none !important;
  transition: all 0.16s ease;
}

.ant-btn-primary {
  background: #202124 !important;
  border: 1px solid #202124 !important;
  color: #FFFFFF !important;
}

.ant-btn-primary:hover:not(:disabled) {
  background: #3C4043 !important;
  border-color: #3C4043 !important;
}

.ant-btn-primary.ant-btn-background-ghost {
  background: #FFFFFF !important;
  border: 1px solid #202124 !important;
  color: #202124 !important;
}

.ant-btn-primary.ant-btn-background-ghost:hover:not(:disabled) {
  background: #F1F3F4 !important;
  border-color: #202124 !important;
  color: #202124 !important;
}

.ant-btn-primary:disabled {
  background: #E8EAED !important;
  border-color: #E8EAED !important;
  color: #9AA0A6 !important;
}

.ant-btn-default {
  border: 1px solid #BDC1C6 !important;
  background: #FFFFFF !important;
  color: #202124 !important;
}

.ant-btn-default:hover:not(:disabled) {
  border-color: #202124 !important;
  color: #202124 !important;
  background: #F8F9FA !important;
}

.ant-input,
.ant-input-affix-wrapper,
.ant-select-selector,
.ant-input-number {
  border-color: #DADCE0 !important;
  border-radius: 12px !important;
  transition: border-color 0.16s ease, box-shadow 0.16s ease !important;
}

.ant-input::placeholder,
.ant-input-affix-wrapper input::placeholder {
  color: #80868B !important;
}

.ant-input:focus,
.ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: #202124 !important;
  box-shadow: 0 0 0 2px rgba(32, 33, 36, 0.08) !important;
}

.ant-card {
  border-radius: 16px;
  border: 1px solid #DADCE0 !important;
  box-shadow: none !important;
}

.ant-modal-content {
  border-radius: 20px !important;
  box-shadow: var(--kg-shadow-lg) !important;
  border: 1px solid #DADCE0;
}

.ant-segmented {
  background: #F1F3F4 !important;
  padding: 3px;
  border-radius: 999px;
}

.ant-segmented-item {
  border-radius: 999px !important;
}

.ant-segmented-item-selected {
  background: #FFFFFF !important;
  color: #202124 !important;
  box-shadow: 0 1px 3px rgba(32, 33, 36, 0.14) !important;
  font-weight: 600;
}

.ant-tag {
  border: none;
  border-radius: 999px;
  font-weight: 600;
}
</style>
