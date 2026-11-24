<template>
  <footer class="kaggle-footer">
    <div class="footer-inner">
      <div class="footer-left">
        <span class="footer-logo">epiph<span>-code</span></span>
        <span class="footer-sep">·</span>
        <span class="footer-desc">AI 全栈应用构建与版本管理工作台</span>
      </div>
      <div class="footer-right">
        <span class="footer-engine-pill">LangChain4j · LangGraph4j · Vue 3</span>
        <span>© {{ currentYear }} Epiph-code Studio</span>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.kaggle-footer {
  width: 100%;
  margin-top: auto;
  border-top: 1px solid #E8EAED;
  background: #FFFFFF;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: min(1240px, calc(100% - 64px));
  min-height: 60px;
  margin: 0 auto;
  padding: 14px 0;
  color: #5F6368;
  font-size: 13px;
}

.footer-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.footer-logo {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #20BEFF;
}

.footer-logo span {
  color: #202124;
}

.footer-sep {
  color: #DADCE0;
}

.footer-right {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.footer-engine-pill {
  padding: 2px 10px;
  background: #F8F9FA;
  border: 1px solid #E8EAED;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  color: #5F6368;
}

@media (max-width: 768px) {
  .footer-inner {
    width: calc(100% - 32px);
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }

  .footer-desc,
  .footer-sep,
  .footer-engine-pill {
    display: none;
  }
}
</style>
