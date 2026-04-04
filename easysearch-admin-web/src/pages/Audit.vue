<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { http } from '../services/http'
import { useToast, usePagination } from '../composables'
import { QpModal, QpPagination, QpToast } from '../components'

// ==================== 类型定义 ====================
type AuditLog = {
  id: number
  userName: string
  action: string
  resourceSetId: number
  snapshotId?: number | null
  batchId?: string | null
  entityType?: string | null
  entityId?: number | null
  beforeJson?: string | null
  afterJson?: string | null
  createdAt?: string | null
  // 展示字段
  resourceSetName?: string | null
  snapshotNo?: number | null
}

type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }

// ==================== 操作类型配置 ====================
const ACTION_CONFIG: Record<string, { label: string; class: string; icon: string }> = {
  CREATE: { label: '新建', class: 'action-create', icon: '➕' },
  UPDATE: { label: '更新', class: 'action-update', icon: '✏️' },
  DELETE: { label: '删除', class: 'action-delete', icon: '🗑️' },
  PUBLISH: { label: '发布', class: 'action-publish', icon: '🚀' },
  ROLLBACK: { label: '回滚', class: 'action-rollback', icon: '↩️' },
}

// ==================== 组合式函数 ====================
const { message: toastMessage, showError } = useToast()

// ==================== 基础状态 ====================
const loading = ref(false)
const resourceSetId = ref<string>('')
const snapshotId = ref<string>('')
const actionFilter = ref<string>('')
const entityTypeFilter = ref<string>('')

// ==================== 数据状态 ====================
const allLogs = ref<AuditLog[]>([])
const selectedLog = ref<AuditLog | null>(null)
const detailModalOpen = ref(false)

// ==================== 计算属性 ====================
const filteredLogs = computed(() => {
  let result = allLogs.value

  // 按 resourceSetId 过滤
  if (resourceSetId.value) {
    const id = Number(resourceSetId.value)
    if (!isNaN(id)) {
      result = result.filter((r) => r.resourceSetId === id)
    }
  }

  // 按 snapshotId 过滤
  if (snapshotId.value) {
    const id = Number(snapshotId.value)
    if (!isNaN(id)) {
      result = result.filter((r) => r.snapshotId === id)
    }
  }

  // 按操作类型过滤
  if (actionFilter.value) {
    result = result.filter((r) => r.action === actionFilter.value)
  }

  // 按实体类型过滤
  if (entityTypeFilter.value) {
    result = result.filter((r) => r.entityType === entityTypeFilter.value)
  }

  return result
})

// 提取所有可用的操作类型选项
const actionOptions = computed(() => {
  const actionSet = new Set(allLogs.value.map((r) => r.action))
  return Array.from(actionSet).filter(Boolean).sort()
})

// 提取所有可用的实体类型选项
const entityTypeOptions = computed(() => {
  const entityTypeSet = new Set(allLogs.value.map((r) => r.entityType))
  return Array.from(entityTypeSet).filter(Boolean).sort() as string[]
})

// 分页
const pagination = usePagination({ data: filteredLogs, defaultPageSize: 20 })

// ==================== 数据加载 ====================
async function loadLogs() {
  loading.value = true
  try {
    const res = await http.get('/api/audit-logs', {
      params: {
        page: 1,
        pageSize: 500, // 一次性加载更多数据，前端分页
      },
    })
    allLogs.value = ((res.data?.data as PageResult<AuditLog>)?.items ?? []) as AuditLog[]
    pagination.resetPage()
  } catch (e: any) {
    showError(e, '加载审计日志失败')
  } finally {
    loading.value = false
  }
}

// ==================== 操作函数 ====================
function openDetail(log: AuditLog) {
  selectedLog.value = log
  detailModalOpen.value = true
}

function getActionConfig(action: string) {
  return ACTION_CONFIG[action] || { label: action, class: 'action-default', icon: '○' }
}

function formatJson(json?: string | null): string {
  if (!json) return '—'
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

function clearFilters() {
  resourceSetId.value = ''
  snapshotId.value = ''
  actionFilter.value = ''
  entityTypeFilter.value = ''
  pagination.resetPage()
}

// ==================== 监听器 ====================
watch(
  () => [pagination.pageSize.value, resourceSetId.value, snapshotId.value, actionFilter.value, entityTypeFilter.value],
  () => pagination.resetPage()
)

// ==================== 生命周期 ====================
onMounted(loadLogs)
</script>

<template>
  <div class="qp-admin">
    <header class="topbar">
      <strong>操作审计</strong>
      <span class="sub">qp_operation_log</span>
    </header>

    <section class="layout">
      <!-- 筛选面板 -->
      <div class="panel panel-filter">
        <div class="filter-bar">
          <span class="filter-title">🔍 筛选条件</span>
          <div class="filter-fields">
            <label class="filter-field">
              <span class="filter-label">资源集 ID</span>
              <input
                v-model="resourceSetId"
                class="input input-filter"
                placeholder="输入资源集 ID"
                type="number"
              />
            </label>
            <label class="filter-field">
              <span class="filter-label">快照 ID</span>
              <input
                v-model="snapshotId"
                class="input input-filter"
                placeholder="输入快照 ID"
                type="number"
              />
            </label>
            <label class="filter-field">
              <span class="filter-label">操作类型</span>
              <select v-model="actionFilter" class="select select-filter">
                <option value="">全部操作</option>
                <option v-for="action in actionOptions" :key="action" :value="action">
                  {{ getActionConfig(action).label }}
                </option>
              </select>
            </label>
            <label class="filter-field">
              <span class="filter-label">实体类型</span>
              <select v-model="entityTypeFilter" class="select select-filter">
                <option value="">全部实体</option>
                <option v-for="et in entityTypeOptions" :key="et" :value="et">{{ et }}</option>
              </select>
            </label>
          </div>
          <div class="filter-actions">
            <button class="btn primary" type="button" @click="loadLogs" :disabled="loading">
              <span v-if="loading" class="btn-loading"></span>
              {{ loading ? '加载中...' : '刷新' }}
            </button>
            <button class="btn" type="button" @click="clearFilters">清空筛选</button>
          </div>
        </div>
      </div>

      <!-- 记录列表 -->
      <div class="panel panel-main">
        <div class="panel-header">
          <h3>📋 审计日志列表</h3>
          <span class="panel-count">共 {{ filteredLogs.length }} 条</span>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>序号</th>
                <th>ID</th>
                <th>资源集</th>           
                <th>快照</th>     
                <th>操作人</th>
                <th>操作</th>
                <th>实体类型</th>
                <th>实体 ID</th>
                <th>时间</th>                
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading && pagination.pagedData.value.length === 0">
                <td colspan="10" class="empty-cell">
                  <span class="loading-spinner"></span>
                  加载中...
                </td>
              </tr>
              <tr v-else-if="pagination.pagedData.value.length === 0">
                <td colspan="10" class="empty-cell">
                  暂无审计日志
                </td>
              </tr>
              <tr
                v-for="(r, idx) in pagination.pagedData.value"
                :key="r.id"
                :class="{ selected: selectedLog?.id === r.id }"
                @click="openDetail(r)"
              >
                <td>{{ (pagination.page.value - 1) * pagination.pageSize.value + idx + 1 }}</td>
                <td>
                  <code class="id-code">{{ r.id }}</code>
                </td>
                <td>
                  <span :title="'ID: ' + r.resourceSetId">{{ r.resourceSetName || r.resourceSetId }}</span>
                </td>       
                <td>
                  <span :title="'ID: ' + r.snapshotId">{{ r.snapshotNo != null ? '#' + r.snapshotNo : (r.snapshotId ?? '-') }}</span>
                </td>         
                <td>
                  <span>{{ r.userName }}</span>
                </td>
                <td>
                  <span class="action-badge" :class="getActionConfig(r.action).class">
                    <span class="action-icon">{{ getActionConfig(r.action).icon }}</span>
                    {{ getActionConfig(r.action).label }}
                  </span>
                </td>
                <td>
                  <span v-if="r.entityType" class="entity-badge">{{ r.entityType }}</span>
                  <span v-else>-</span>
                </td>
                <td>
                  <code v-if="r.entityId" class="id-code">{{ r.entityId }}</code>
                  <span v-else>-</span>
                </td>
                <td class="nowrap">{{ r.createdAt ?? '-' }}</td>
                <td class="actions">
                  <button class="btn btn-detail" type="button" @click.stop="openDetail(r)" title="查看详情">
                    详情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <QpPagination
          v-model:page="pagination.page.value"
          v-model:pageSize="pagination.pageSize.value"
          :total="pagination.total.value"
          :totalPages="pagination.totalPages.value"
          :disabled="loading"
        />
      </div>
    </section>

    <QpToast :message="toastMessage" />

    <!-- 详情弹窗 -->
    <QpModal
      :open="detailModalOpen"
      title="审计详情"
      icon="📋"
      type="info"
      size="lg"
      @close="detailModalOpen = false"
    >
      <div v-if="selectedLog" class="detail-content">
        <div class="detail-header">
          <span class="detail-id">审计日志 #{{ selectedLog.id }}</span>
          <span class="action-badge" :class="getActionConfig(selectedLog.action).class">
            <span class="action-icon">{{ getActionConfig(selectedLog.action).icon }}</span>
            {{ getActionConfig(selectedLog.action).label }}
          </span>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">操作时间</span>
            <span class="detail-value">{{ selectedLog.createdAt ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">操作用户</span>
            <span class="detail-value">
              <span class="user-badge">{{ selectedLog.userName }}</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">资源集</span>
            <span class="detail-value">
              {{ selectedLog.resourceSetName || '-' }}
              <code v-if="selectedLog.resourceSetId" style="margin-left: 4px; font-size: 11px; color: #9ca3af;">ID: {{ selectedLog.resourceSetId }}</code>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">快照</span>
            <span class="detail-value">
              {{ selectedLog.snapshotNo != null ? '#' + selectedLog.snapshotNo : '-' }}
              <code v-if="selectedLog.snapshotId" style="margin-left: 4px; font-size: 11px; color: #9ca3af;">ID: {{ selectedLog.snapshotId }}</code>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">实体类型</span>
            <span class="detail-value">
              <span v-if="selectedLog.entityType" class="entity-badge">{{ selectedLog.entityType }}</span>
              <span v-else>-</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">实体 ID</span>
            <span class="detail-value">
              <code v-if="selectedLog.entityId">{{ selectedLog.entityId }}</code>
              <span v-else>-</span>
            </span>
          </div>
          <div v-if="selectedLog.batchId" class="detail-item full-width">
            <span class="detail-label">批次 ID</span>
            <span class="detail-value">
              <code>{{ selectedLog.batchId }}</code>
            </span>
          </div>
        </div>

        <div class="modal-diff">
          <div class="diff-col">
            <h4 class="diff-title">
              <span class="diff-icon before">◀</span>
              变更前 (before_json)
            </h4>
            <pre class="diff-pre before">{{ formatJson(selectedLog.beforeJson) }}</pre>
          </div>
          <div class="diff-col">
            <h4 class="diff-title">
              <span class="diff-icon after">▶</span>
              变更后 (after_json)
            </h4>
            <pre class="diff-pre after">{{ formatJson(selectedLog.afterJson) }}</pre>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-cancel" type="button" @click="detailModalOpen = false">关闭</button>
      </template>
    </QpModal>
  </div>
</template>

<style scoped>
/* ==================== 筛选面板 ==================== */
.panel-filter {
  padding: 10px 14px !important;
  margin-bottom: 12px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
}

.filter-fields {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}

.filter-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.input-filter {
  width: 120px;
  height: 30px;
  padding: 0 10px;
  font-size: 13px;
  border-radius: 6px;
}

.select-filter {
  width: 120px;
  height: 30px;
  padding: 0 8px;
  font-size: 13px;
  border-radius: 6px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

/* ==================== 表格样式 ==================== */
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 900px;
}

table thead th {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  font-weight: 600;
  color: #475569;
  font-size: 12px;
  padding: 10px 8px;
  border-bottom: 2px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 1;
  white-space: nowrap;
}

table tbody td {
  padding: 10px 8px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

table tbody tr {
  transition: background 0.15s ease;
  cursor: pointer;
}

table tbody tr:hover {
  background: #f8fafc;
}

table tbody tr.selected {
  background: #eff6ff;
}

table tbody tr:last-child td {
  border-bottom: none;
}

.empty-cell {
  text-align: center;
  color: #9ca3af;
  padding: 32px !important;
}

.nowrap {
  white-space: nowrap;
}

/* ==================== ID 显示 ==================== */
.id-code {
  display: inline-block;
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 4px;
  font-size: 12px;
  color: #4b5563;
  font-family: ui-monospace, monospace;
}

/* ==================== 用户标签 ==================== */
.user-badge {
  display: inline-block;
  padding: 3px 10px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #3730a3;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

/* ==================== 实体类型标签 ==================== */
.entity-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  color: #374151;
  border-radius: 4px;
  font-size: 11px;
  font-family: ui-monospace, monospace;
}

/* ==================== 操作类型标签 ==================== */
.action-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.action-icon {
  font-size: 10px;
}

.action-create {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
}

.action-update {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
}

.action-delete {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #991b1b;
}

.action-publish {
  background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%);
  color: #7c3aed;
}

.action-rollback {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
}

.action-default {
  background: #f3f4f6;
  color: #6b7280;
}

/* ==================== 操作按钮 ==================== */
.actions {
  white-space: nowrap;
}

.btn-detail {
  height: 26px;
  line-height: 24px;
  padding: 0 10px;
  font-size: 11px;
  background: #fff;
  color: #3b82f6;
  border: 1px solid #93c5fd;
  border-radius: 4px;
  transition: all 0.15s ease;
  cursor: pointer;
}

.btn-detail:hover {
  background: #eff6ff;
  border-color: #60a5fa;
}

/* ==================== JSON 对比区域（弹窗内） ==================== */
.diff-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.diff-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.diff-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  font-size: 10px;
}

.diff-icon.before {
  background: #fee2e2;
  color: #dc2626;
}

.diff-icon.after {
  background: #dcfce7;
  color: #16a34a;
}

.diff-pre {
  flex: 1;
  margin: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  overflow: auto;
  max-height: 200px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.diff-pre.before {
  border-left: 3px solid #fca5a5;
}

.diff-pre.after {
  border-left: 3px solid #86efac;
}

/* ==================== 详情弹窗 ==================== */
.detail-content {
  padding: 4px 0;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.detail-id {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full-width {
  grid-column: span 2;
}

.detail-label {
  font-size: 11px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-value {
  font-size: 13px;
  color: #1f2937;
}

.detail-value code {
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 4px;
  font-size: 12px;
}

.modal-diff {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.modal-diff .diff-pre {
  max-height: 250px;
}

/* ==================== 加载状态 ==================== */
.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 8px;
  vertical-align: middle;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ==================== 响应式 ==================== */
@media (max-width: 1024px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-fields {
    flex-direction: column;
  }
  
  .filter-field {
    flex-direction: column;
    align-items: stretch;
  }
  
  .input-filter,
  .select-filter {
    width: 100%;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .detail-item.full-width {
    grid-column: span 1;
  }
  
  .modal-diff {
    grid-template-columns: 1fr;
  }
}
</style>
