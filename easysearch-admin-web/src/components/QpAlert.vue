<script setup lang="ts">
/**
 * 弹窗内提示/警告条组件
 */
type AlertType = 'info' | 'warn'

interface Props {
  /** 类型 */
  type?: AlertType
  /** 图标（emoji） */
  icon?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  icon: 'ℹ',
})
</script>

<template>
  <div class="modal-alert" :class="type">
    <span class="alert-icon">{{ icon }}</span>
    <span><slot /></span>
  </div>
</template>

<style scoped>
.modal-alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 14px;
}

.modal-alert.info {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #93c5fd;
  color: #1e40af;
}

.modal-alert.warn {
  background: linear-gradient(135deg, #fefce8 0%, #fef3c7 100%);
  border: 1px solid #fcd34d;
  color: #92400e;
}

.alert-icon {
  flex-shrink: 0;
  font-size: 14px;
}
</style>
