<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="footer-logo-dot"></span>
        <span class="footer-brand-title">Epiph-code Studio</span>
        <span class="footer-tagline">AI 应用工程化生成工作台</span>
      </div>
      <div class="footer-meta">
        <span class="footer-status">
          <span class="status-pulse-dot"></span>
          <span>服务运行正常</span>
        </span>
        <span class="footer-sep">·</span>
        <span>© {{ currentYear }} Epiph-code</span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.site-footer {
  width: 100%;
  margin-top: auto;
  border-top: 1px solid #E3E3E3;
  background: #FFFFFF;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: min(1380px, calc(100vw - 48px));
  min-height: 60px;
  margin: 0 auto;
  padding: 14px 0;
  color: #747775;
  font-size: 13px;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.footer-logo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0B57D0;
}

.footer-brand-title {
  color: #1F1F1F;
  font-weight: 600;
}

.footer-tagline {
  color: #747775;
  font-size: 12.5px;
}

.footer-meta {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.footer-sep {
  color: #DADCE0;
}

.footer-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: #E6F4EA;
  border-radius: 999px;
  color: #146C2E;
  font-size: 12px;
  font-weight: 500;
}

.status-pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1E8E3E;
}

@media (max-width: 768px) {
  .footer-inner {
    width: calc(100vw - 32px);
    flex-direction: column;
    align-items: center;
    gap: 8px;
    text-align: center;
  }

  .footer-tagline {
    display: none;
  }
}
</style>
