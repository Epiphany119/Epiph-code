<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>

<template>
  <a-config-provider
    :theme="{
      token: {
        colorPrimary: '#b86a42',
        colorInfo: '#b86a42',
        colorSuccess: '#5f8f63',
        colorWarning: '#c28b3f',
        colorError: '#ff4d4f',
        borderRadius: 12,
        fontFamily: 'PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif',
      },
    }"
  >
    <BasicLayout />
  </a-config-provider>
</template>

<style>
:root {
  --zc-bg-page: #f4f5f9;
  --zc-bg-card: #ffffff;
  --zc-bg-soft: #eff1f6;
  --zc-text-primary: #292c37;
  --zc-text-secondary: #7e8492;
  --zc-border: #e4e7ee;
  --zc-primary: #b86a42;
  --zc-shadow: 0 14px 38px rgba(38, 44, 62, 0.065);
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
}

body {
  color: var(--zc-text-primary);
  background:
    radial-gradient(circle at 12% -8%, rgba(207, 154, 123, 0.14), transparent 34%),
    radial-gradient(circle at 92% 8%, rgba(142, 157, 198, 0.12), transparent 30%),
    linear-gradient(180deg, #fafafe 0%, #f3f4f8 100%);
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

a {
  color: inherit;
}
</style>
