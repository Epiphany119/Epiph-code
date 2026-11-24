<template>
  <div class="app-shell" :class="{ 'app-shell--chat': isChatPage }">
    <GlobalHeader />
    <main class="main-content" :class="{ 'main-content--chat': isChatPage }">
      <RouterView />
    </main>
    <GlobalFooter v-if="!isChatPage" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()
const isChatPage = computed(() => route.path.startsWith('/app/chat/'))
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.app-shell--chat {
  height: 100vh;
  overflow: hidden;
  background: #F1F5F9;
}

.main-content {
  width: min(1360px, calc(100vw - 48px));
  flex: 1;
  margin: 0 auto;
  padding: 28px 0 72px;
}

.main-content--chat {
  width: 100%;
  max-width: 100%;
  height: calc(100vh - 60px);
  margin: 0;
  padding: 0;
  overflow: hidden;
}

@media (max-width: 768px) {
  .main-content {
    width: calc(100vw - 28px);
    padding: 20px 0 44px;
  }
}
</style>
