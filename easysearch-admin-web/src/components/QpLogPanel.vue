<script setup lang="ts">
/**
 * 日志面板组件
 * 用于显示发布记录、操作审计等日志列表
 */
interface Props {
  /** 面板标题 */
  title: string
  /** 图标（emoji） */
  icon?: string
  /** 日志列表 */
  logs: string[]
  /** 最大显示条数 */
  maxItems?: number
  /** 空状态提示文本 */
  emptyText?: string
}

const props = withDefaults(defineProps<Props>(), {
  icon: '📝',
  maxItems: 30,
  emptyText: '暂无记录',
})
</script>

<template>
  <div class="panel panel-side">
    <div class="panel-header">
      <h3>{{ icon }} {{ title }}</h3>
      <span class="panel-count">{{ logs.length }}</span>
    </div>
    <div class="log-box">
      <div v-if="logs.length === 0" class="empty-hint">{{ emptyText }}</div>
      <div v-else v-for="(log, i) in logs.slice(0, maxItems)" :key="i" class="log-item">
        {{ log }}
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

.log-item {
  padding: 4px 0;
  font-size: 11px;
  color: #4b5563;
  border-bottom: 1px solid #f9fafb;
  line-height: 1.5;
}

.log-item:last-child {
  border-bottom: none;
}
</style>
