<script setup lang="ts">
/**
 * Toast 消息提示组件
 */
interface Props {
  /** 消息内容 */
  message: string
}

defineProps<Props>()
</script>

<template>
  <Transition name="toast">
    <div v-if="message" class="toast">{{ message }}</div>
  </Transition>
</template>

<style scoped>
.toast {
  position: fixed;
  right: 20px;
  bottom: 20px;
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
  color: #fff;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13px;
  max-width: 400px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  z-index: 2000;
}

.toast-enter-active {
  animation: toast-in 0.25s ease;
}

.toast-leave-active {
  animation: toast-out 0.2s ease;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(10px);
  }
}
</style>
