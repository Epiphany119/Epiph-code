<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="footer-logo-dot"></span>
        <span class="footer-brand-title">Epiph-code Studio</span>
        <span class="footer-tagline">LangChain4j + LangGraph4j 双引擎 AI 全栈应用工作台</span>
      </div>
      <div class="footer-meta">
        <span class="footer-stack-pill">HTML · Multi-File · Vue 3</span>
        <span class="footer-status">
          <span class="status-pulse-dot"></span>
          <span>生成引擎就绪</span>
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
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: min(1360px, calc(100vw - 48px));
  min-height: 60px;
  margin: 0 auto;
  padding: 14px 0;
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
  background: linear-gradient(135deg, #2563EB, #4F46E5);
}

.footer-brand-title {
  color: #0F172A;
  font-weight: 600;
}

.footer-tagline {
  color: #64748B;
  font-size: 12.5px;
}

.footer-meta {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.footer-stack-pill {
  padding: 2px 10px;
  background: #F1F5F9;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 500;
  color: #475569;
}

.footer-sep {
  color: #CBD5E1;
}

.footer-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: #ECFDF5;
  border: 1px solid rgba(5, 150, 105, 0.18);
  border-radius: 999px;
  color: #059669;
  font-size: 12px;
  font-weight: 600;
}

.status-pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
}

@media (max-width: 768px) {
  .footer-inner {
    width: calc(100vw - 32px);
    flex-direction: column;
    align-items: center;
    gap: 8px;
    text-align: center;
  }

  .footer-tagline,
  .footer-stack-pill {
    display: none;
  }
}
</style>
