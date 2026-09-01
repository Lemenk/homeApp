<script setup lang="ts">
/**
 * 通用账户图标组件：优先渲染品牌 SVG（simple-icons），无 SVG 时回退 emoji
 * 用法：<AppIcon icon="wechat" :size="22" />
 */
import { computed } from 'vue'
import { iconOption } from '@/utils/accountIcon'

const props = withDefaults(defineProps<{ icon?: string; size?: number }>(), { size: 22 })

const opt = computed(() => iconOption(props.icon))
</script>

<template>
  <svg
    v-if="opt.svg"
    class="app-brand-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    role="img"
    :aria-label="opt.label"
  >
    <path :d="opt.svg.path" :fill="opt.svg.color" />
  </svg>
  <span v-else class="app-emoji-icon" :style="{ fontSize: size + 'px' }">{{ opt.emoji }}</span>
</template>

<style scoped>
.app-brand-icon {
  display: block;
  flex-shrink: 0;
}
.app-emoji-icon {
  line-height: 1;
}
</style>
