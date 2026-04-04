<script setup lang="ts">
/**
 * 快照列表组件
 * 用于显示历史快照列表
 */
import type { Snapshot } from '../types'

interface Props {
  /** 快照列表 */
  snapshots: Snapshot[]
  /** 最大显示条数 */
  maxItems?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxItems: 20,
})

const emit = defineEmits<{
  (e: 'view', snapshot: Snapshot): void
}>()
</script>

<template>
  <div class="panel panel-side">
    <div class="panel-header">
      <h3>📦 历史快照</h3>
      <span class="panel-count">{{ snapshots.length }}</span>
    </div>
    <div class="log-box">
      <div v-if="snapshots.length === 0" class="empty-hint">暂无快照记录</div>
      <div v-else v-for="s in snapshots.slice(0, maxItems)" :key="s.id" class="snapshot-row">
        <div class="snapshot-info">
          <span class="snapshot-no">#{{ s.snapshotNo }}</span>
          <span class="snapshot-meta">{{ s.ruleCount }}条 · {{ s.publishedBy }}</span>
          <span class="snapshot-time">{{ s.publishedAt }}</span>
        </div>
        <button class="btn btn-sm" type="button" @click="emit('view', s)">查看</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel-side {
  padding: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f2f5;
}

.panel-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.panel-count {
  font-size: 11px;
  padding: 2px 8px;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 10px;
}

.empty-hint {
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  padding: 16px 0;
}

.snapshot-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.snapshot-row:last-child {
  border-bottom: none;
}

.snapshot-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}

.snapshot-no {
  font-weight: 600;
  color: #1f2937;
}

.snapshot-meta {
  color: #6b7280;
  font-size: 11px;
}

.snapshot-time {
  color: #9ca3af;
  font-size: 10px;
}

.btn-sm {
  height: 24px;
  line-height: 22px;
  padding: 0 10px;
  font-size: 11px;
  border-radius: 4px;
}
</style>
