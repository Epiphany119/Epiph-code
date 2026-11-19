<template>
  <div class="user-info">
    <a-avatar :src="user?.userAvatar" :size="size" class="user-avatar">
      {{ user?.userName?.charAt(0) || 'U' }}
    </a-avatar>
    <span v-if="showName" class="user-name">{{ user?.userName || '未知用户' }}</span>
  </div>
</template>

<script setup lang="ts">
interface Props {
  user?: API.UserVO
  size?: number | 'small' | 'default' | 'large'
  showName?: boolean
}

withDefaults(defineProps<Props>(), {
  size: 'default',
  showName: true,
})
</script>

<style scoped>
.user-info {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  flex: 0 0 auto;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: #FFFFFF;
  font-weight: 600;
}

.user-name {
  min-width: 0;
  max-width: 140px;
  overflow: hidden;
  color: #1E293B;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
