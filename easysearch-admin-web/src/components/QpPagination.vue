<script setup lang="ts">
/**
 * 通用分页组件
 * 显示分页信息和翻页控件
 */
interface Props {
  /** 当前页码 */
  page: number
  /** 每页大小 */
  pageSize: number
  /** 总条数 */
  total: number
  /** 总页数 */
  totalPages: number
  /** 页大小选项 */
  pageSizeOptions?: number[]
  /** 是否禁用 */
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  pageSizeOptions: () => [10, 20, 50],
  disabled: false,
})

const emit = defineEmits<{
  (e: 'update:page', value: number): void
  (e: 'update:pageSize', value: number): void
}>()

function onPageSizeChange(event: Event) {
  const target = event.target as HTMLSelectElement
  emit('update:pageSize', Number(target.value))
}

function onPageInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:page', Number(target.value))
}

function prevPage() {
  if (props.page > 1) {
    emit('update:page', props.page - 1)
  }
}

function nextPage() {
  if (props.page < props.totalPages) {
    emit('update:page', props.page + 1)
  }
}
</script>

<template>
  <div class="pager-bottom">
    <div class="pager-info">
      <span class="pager-stat">共 <strong>{{ total }}</strong> 条</span>
      <span class="pager-divider">|</span>
      <span class="pager-stat">{{ totalPages }} 页</span>
    </div>
    <div class="pager-compact">
      <span class="pager-label">每页</span>
      <select
        :value="pageSize"
        class="select select-sm"
        :disabled="disabled"
        @change="onPageSizeChange"
      >
        <option v-for="opt in pageSizeOptions" :key="opt" :value="opt">{{ opt }}</option>
      </select>
      <span class="pager-label">条</span>
      <span class="pager-divider">|</span>
      <span class="pager-label">第</span>
      <input
        :value="page"
        class="input input-sm"
        type="number"
        min="1"
        :max="totalPages"
        :disabled="disabled"
        @change="onPageInput"
      />
      <span class="pager-label">页</span>
      <button
        class="btn btn-pager"
        type="button"
        :disabled="disabled || page <= 1"
        @click="prevPage"
      >
        ‹ 上一页
      </button>
      <button
        class="btn btn-pager"
        type="button"
        :disabled="disabled || page >= totalPages"
        @click="nextPage"
      >
        下一页 ›
      </button>
    </div>
  </div>
</template>

<style scoped>
.pager-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f5;
}

.pager-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 13px;
}

.pager-stat strong {
  color: #1f2937;
  font-weight: 600;
}

.pager-divider {
  color: #d1d5db;
}

.pager-compact {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pager-label {
  font-size: 12px;
  color: #6b7280;
}

.select-sm {
  height: 28px;
  width: 64px;
  padding: 0 6px;
  font-size: 12px;
  border-radius: 6px;
}

.input-sm {
  height: 28px;
  width: 52px;
  padding: 0 6px;
  font-size: 12px;
  border-radius: 6px;
  text-align: center;
}

.btn-pager {
  height: 28px;
  line-height: 26px;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 6px;
}

.btn-pager:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .pager-bottom {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
