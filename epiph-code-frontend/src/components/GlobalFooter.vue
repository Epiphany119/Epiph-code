<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="footer-logo-dot"></span>
        <span class="footer-brand-title">Epiph-code Studio</span>
        <span class="footer-tagline">全栈 AI 应用工程化生成平台</span>
      </div>
      <div class="footer-meta">
        <span class="footer-status">
          <span class="status-pulse-dot"></span>
          <span>服务运行正常</span>
        </span>
        <span class="footer-sep">·</span>
        <span>© {{ currentYear }} Epiph-code. All rights reserved.</span>
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
  border-top: 1px solid rgba(226, 232, 240, 0.85);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: min(1380px, calc(100vw - 48px));
  min-height: 64px;
  margin: 0 auto;
  padding: 16px 0;
  color: #64748B;
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
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
}

.footer-brand-title {
  color: #1E293B;
  font-weight: 600;
}

.footer-tagline {
  color: #94A3B8;
  font-size: 12px;
}

.footer-meta {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.footer-sep {
  color: #CBD5E1;
}

.footer-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 999px;
  color: #059669;
  font-size: 12px;
  font-weight: 500;
}

.status-pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  animation: pulse 2s infinite ease-in-out;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.2);
  }
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
