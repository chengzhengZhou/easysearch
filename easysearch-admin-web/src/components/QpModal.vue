<script setup lang="ts">
/**
 * 通用弹窗组件
 * 支持不同尺寸和类型的弹窗
 */
import { computed } from 'vue'

type ModalType = 'info' | 'warn' | 'primary' | 'danger'
type ModalSize = 'sm' | 'md' | 'lg'

interface Props {
  /** 是否显示弹窗 */
  open: boolean
  /** 弹窗标题 */
  title: string
  /** 图标（emoji 或文字） */
  icon?: string
  /** 类型，影响图标背景色 */
  type?: ModalType
  /** 弹窗尺寸 */
  size?: ModalSize
  /** 是否显示底部 */
  showFooter?: boolean
  /** 是否可滚动 */
  scrollable?: boolean
  /** 是否为结果层（更高 z-index） */
  isResult?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  icon: '',
  type: 'primary',
  size: 'sm',
  showFooter: true,
  scrollable: false,
  isResult: false,
})

const emit = defineEmits<{
  (e: 'close'): void
}>()

const modalClass = computed(() => ({
  'modal-sm': props.size === 'sm',
  'modal-md': props.size === 'md',
  'modal-animate': true,
}))

const iconClass = computed(() => ({
  'modal-icon': true,
  [props.type]: true,
}))
</script>

<template>
  <div class="modal-mask" :class="{ show: open, result: isResult }">
    <div class="modal" :class="modalClass">
      <div class="modal-head">
        <div class="modal-title">
          <span v-if="icon" :class="iconClass">{{ icon }}</span>
          <strong>{{ title }}</strong>
        </div>
        <button class="btn-close" type="button" @click="emit('close')" title="关闭">×</button>
      </div>
      <div class="modal-body" :class="{ 'modal-body-scroll': scrollable }">
        <slot />
      </div>
      <div v-if="showFooter" class="modal-foot">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 弹窗动画 */
.modal-animate {
  animation: modal-slide-in 0.2s ease-out;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(0, 0, 0, 0.05);
}

@keyframes modal-slide-in {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-title strong {
  font-size: 15px;
  color: #1f2937;
}

.modal-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  font-size: 14px;
}

.modal-icon.primary {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  color: #4f46e5;
}

.modal-icon.warn {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
}

.modal-icon.info {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #1e40af;
}

.modal-icon.danger {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
}

.btn-close {
  width: 28px;
  height: 28px;
  border: none;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 18px;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.btn-close:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.modal-body-scroll {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
