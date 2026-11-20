<template>
  <div class="user-info">
    <a-avatar :src="user?.userAvatar" :size="size" class="user-avatar">
      {{ displayName.charAt(0).toUpperCase() }}
    </a-avatar>
    <span v-if="showName" class="user-name">{{ displayName }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  user?: API.UserVO
  size?: number | 'small' | 'default' | 'large'
  showName?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'default',
  showName: true,
})

const displayName = computed(() => {
  const name = props.user?.userName?.trim()
  if (name && name !== '无名') return name
  return props.user?.userAccount || '未知用户'
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
  background: #1A73E8;
  color: #FFFFFF;
  font-weight: 600;
}

.user-name {
  min-width: 0;
  max-width: 140px;
  overflow: hidden;
  color: #1F1F1F;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
