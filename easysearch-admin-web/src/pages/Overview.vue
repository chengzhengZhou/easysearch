<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '../composables'
import { QpToast, QpLogPanel } from '../components'
import {
  fetchResourceSets,
  fetchResourceSetStats,
  fetchRecentPublishRecords,
  fetchRecentAuditLogs,
} from '../services/overview'
import type { ResourceSet, Snapshot, AuditLogItem, PublishRecord } from '../types'

// ==================== 路由 ====================
const router = useRouter()

// ==================== 基础状态 ====================
const loading = ref(true)

// ==================== 组合式函数 ====================
const { message: toastMessage, showError } = useToast()

// ==================== 数据状态 ====================
const resourceSets = ref<ResourceSet[]>([])
const resourceSetStats = ref<Map<number, { ruleCount: number; snapshot: Snapshot | null; hasPendingChanges: boolean }>>(new Map())
const recentPublishRecords = ref<PublishRecord[]>([])
const recentAuditLogs = ref<AuditLogItem[]>([])

// ==================== 计算属性 ====================
const totalResourceSets = computed(() => resourceSets.value.length)
const totalRules = computed(() => {
  let sum = 0
  for (const stats of resourceSetStats.value.values()) {
    sum += stats.ruleCount
  }
  return sum
})
const publishedResourceSets = computed(() => {
  let count = 0
  for (const rs of resourceSets.value) {
    if (rs.currentSnapshotId) count++
  }
  return count
})
const pendingChangesCount = computed(() => {
  let count = 0
  for (const stats of resourceSetStats.value.values()) {
    if (stats.hasPendingChanges) count++
  }
  return count
})

const publishLogLines = computed(() => {
  return recentPublishRecords.value.map((r) => {
    const status = r.publishStatus === 'SUCCESS' ? '✓ 成功' : r.publishStatus === 'FAILED' ? '✗ 失败' : r.publishStatus
    return `${r.startedAt ?? ''} | ${status} | snapshot #${r.snapshotId} | ${r.operator}`
  })
})

const auditLogLines = computed(() => {
  return recentAuditLogs.value.map((l) => {
    return `${l.createdAt ?? ''} | ${l.action} | ${l.userName} | ${l.entityType ?? '-'}:${l.entityId ?? '-'}`
  })
})

// ==================== 数据加载 ====================
async function loadAll() {
  loading.value = true
  try {
    await Promise.all([
      loadResourceSets(),
      loadRecentPublishRecords(),
      loadRecentAuditLogs(),
    ])
  } catch (e: any) {
    showError(e, '加载数据失败')
  } finally {
    loading.value = false
  }
}

async function loadResourceSets() {
  const result = await fetchResourceSets()
  resourceSets.value = result.items
  
  // 并行加载每个资源集的统计信息
  const promises = resourceSets.value.map(async (rs) => {
    const stats = await fetchResourceSetStats(rs.id)
    resourceSetStats.value.set(rs.id, stats)
  })
  await Promise.all(promises)
}

async function loadRecentPublishRecords() {
  try {
    const result = await fetchRecentPublishRecords()
    recentPublishRecords.value = result.items
  } catch { /* ignore */ }
}

async function loadRecentAuditLogs() {
  try {
    const result = await fetchRecentAuditLogs()
    recentAuditLogs.value = result.items
  } catch { /* ignore */ }
}

// ==================== 操作函数 ====================
function getModuleIcon(moduleType: string): string {
  const icons: Record<string, string> = {
    intervention: '🔧',
    synonym: '🔄',
    entity: '🏷️',
    token: '✂️',
  }
  return icons[moduleType] || '📦'
}

function getModuleLabel(moduleType: string): string {
  const labels: Record<string, string> = {
    intervention: '干预规则',
    synonym: '同义词',
    entity: '实体',
    token: '分词词典',
  }
  return labels[moduleType] || moduleType
}

function getStatusBadge(rs: ResourceSet) {
  const stats = resourceSetStats.value.get(rs.id)
  if (stats?.hasPendingChanges) {
    return { label: '待发布', type: 'warn' }
  }
  if (rs.currentSnapshotId) {
    return { label: '已发布', type: 'success' }
  }
  return { label: '未发布', type: 'muted' }
}

function goToModule(rs: ResourceSet) {
  const routes: Record<string, string> = {
    intervention: '/intervention',
    synonym: '/synonym',
    entity: '/entity',
    token: '/token',
  }
  const path = routes[rs.moduleType] || '/intervention'
  router.push({ path, query: { resourceSetId: rs.id } })
}

function formatTime(timeStr: string | undefined | null): string {
  if (!timeStr) return '-'
  try {
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return timeStr
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="qp-admin">
    <header class="topbar">
      <strong>资源总览</strong>
      <span class="sub">查看所有资源集、规则统计与最近操作</span>
    </header>

    <section class="layout two-col">
      <div class="main-content">
        <!-- 统计卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon stat-icon-primary">📦</div>
            <div class="stat-body">
              <div class="stat-value">{{ totalResourceSets }}</div>
              <div class="stat-label">资源集</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-success">📋</div>
            <div class="stat-body">
              <div class="stat-value">{{ totalRules }}</div>
              <div class="stat-label">规则总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-info">🚀</div>
            <div class="stat-body">
              <div class="stat-value">{{ publishedResourceSets }}</div>
              <div class="stat-label">已发布</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-warn">⚠️</div>
            <div class="stat-body">
              <div class="stat-value">{{ pendingChangesCount }}</div>
              <div class="stat-label">待发布变更</div>
            </div>
          </div>
        </div>

        <!-- 资源集列表 -->
        <div class="panel panel-main">
          <div class="panel-header">
            <h3>📦 资源集列表</h3>
            <span class="panel-count">共 {{ resourceSets.length }} 个</span>
          </div>

          <div v-if="loading" class="loading-box">
            <span class="loading-spinner"></span>
            <span>加载中...</span>
          </div>

          <div v-else-if="resourceSets.length === 0" class="empty-hint">
            暂无资源集，请先创建资源集
          </div>

          <div v-else class="resource-list">
            <div
              v-for="rs in resourceSets"
              :key="rs.id"
              class="resource-card"
              @click="goToModule(rs)"
            >
              <div class="resource-header">
                <span class="resource-icon">{{ getModuleIcon(rs.moduleType) }}</span>
                <span class="resource-name">{{ rs.name }}</span>
                <span
                  class="resource-status"
                  :class="`status-${getStatusBadge(rs).type}`"
                >
                  {{ getStatusBadge(rs).label }}
                </span>
              </div>
              <div class="resource-meta">
                <span class="meta-item">
                  <span class="meta-label">类型</span>
                  <span class="meta-value">{{ getModuleLabel(rs.moduleType) }}</span>
                </span>
                <span class="meta-item">
                  <span class="meta-label">场景</span>
                  <span class="meta-value">{{ rs.scene || '-' }}</span>
                </span>
                <span class="meta-item">
                  <span class="meta-label">环境</span>
                  <span class="meta-value">{{ rs.env || '-' }}</span>
                </span>
              </div>
              <div class="resource-stats">
                <span class="stat-item">
                  <span class="stat-num">{{ resourceSetStats.get(rs.id)?.ruleCount ?? 0 }}</span>
                  <span class="stat-text">规则</span>
                </span>
                <span class="stat-divider">|</span>
                <span class="stat-item">
                  <span class="stat-text">快照</span>
                  <span class="stat-num">
                    {{ resourceSetStats.get(rs.id)?.snapshot ? `#${resourceSetStats.get(rs.id)?.snapshot?.snapshotNo}` : '无' }}
                  </span>
                </span>
                <span class="stat-divider">|</span>
                <span class="stat-item">
                  <span class="stat-text">发布</span>
                  <span class="stat-num">{{ formatTime(resourceSetStats.get(rs.id)?.snapshot?.publishedAt) }}</span>
                </span>
              </div>
              <div class="resource-actions">
                <button class="btn-go" type="button" @click.stop="goToModule(rs)">
                  进入管理 →
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panels">
        <!-- 快捷入口 -->
        <div class="panel panel-side">
          <div class="panel-header">
            <h3>⚡ 快捷入口</h3>
          </div>
          <div class="quick-links">
            <router-link to="/intervention" class="quick-link">
              <span class="quick-icon">🔧</span>
              <span>干预规则</span>
            </router-link>
            <router-link to="/synonym" class="quick-link">
              <span class="quick-icon">🔄</span>
              <span>同义词</span>
            </router-link>
            <router-link to="/entity" class="quick-link">
              <span class="quick-icon">🏷️</span>
              <span>实体管理</span>
            </router-link>
            <router-link to="/token" class="quick-link">
              <span class="quick-icon">✂️</span>
              <span>分词词典</span>
            </router-link>
            <router-link to="/publish" class="quick-link">
              <span class="quick-icon">🚀</span>
              <span>发布管理</span>
            </router-link>
            <router-link to="/audit" class="quick-link">
              <span class="quick-icon">🔍</span>
              <span>审计日志</span>
            </router-link>
          </div>
        </div>

        <!-- 发布记录 -->
        <QpLogPanel
          title="最近发布"
          icon="📝"
          :logs="publishLogLines"
          emptyText="暂无发布记录"
        />

        <!-- 操作审计 -->
        <QpLogPanel
          title="最近操作"
          icon="🔍"
          :logs="auditLogLines"
          emptyText="暂无审计记录"
        />
      </div>
    </section>

    <QpToast :message="toastMessage" />
  </div>
</template>

<style scoped>
/* ==================== 统计卡片 ==================== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.2s ease;
}
.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.stat-icon-primary {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}
.stat-icon-success {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
}
.stat-icon-info {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
}
.stat-icon-warn {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
}

.stat-body {
  flex: 1;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: var(--muted);
  margin-top: 2px;
}

/* ==================== 资源列表 ==================== */
.main-content {
  min-width: 0;
}

.resource-list {
  display: grid;
  gap: 10px;
}

.resource-card {
  background: var(--bg-page);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.resource-card:hover {
  background: #fff;
  border-color: #93c5fd;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.resource-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.resource-icon {
  font-size: 20px;
}
.resource-name {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-secondary);
}
.resource-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}
.status-success {
  background: var(--success-bg);
  color: var(--success);
  border: 1px solid var(--success-border);
}
.status-warn {
  background: var(--warn-bg);
  color: var(--warn);
  border: 1px solid var(--warn-border);
}
.status-muted {
  background: #f3f4f6;
  color: var(--muted);
  border: 1px solid var(--border);
}

.resource-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.meta-label {
  color: var(--muted-light);
}
.meta-value {
  color: var(--text-secondary);
  font-weight: 500;
}

.resource-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #fff;
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.stat-num {
  font-weight: 600;
  color: var(--text-secondary);
}
.stat-text {
  color: var(--muted);
}
.stat-divider {
  color: var(--border);
}

.resource-actions {
  display: flex;
  justify-content: flex-end;
}
.btn-go {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--primary-border);
  background: var(--primary-bg);
  color: var(--primary);
  border-radius: var(--radius-md);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-go:hover {
  background: var(--primary);
  color: #fff;
}

/* ==================== 快捷入口 ==================== */
.quick-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.quick-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--bg-page);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.15s ease;
}
.quick-link:hover {
  background: var(--primary-bg);
  border-color: var(--primary-border);
  color: var(--primary);
}
.quick-icon {
  font-size: 16px;
}

/* ==================== 加载状态 ==================== */
.loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 20px;
  color: var(--muted);
  font-size: 14px;
}
.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ==================== 响应式 ==================== */
@media (max-width: 1024px) {
  .resource-meta {
    flex-direction: column;
    gap: 6px;
  }
  .resource-stats {
    flex-wrap: wrap;
  }
}
</style>
