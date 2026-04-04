<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { http } from '../services/http'
import { useToast, usePagination } from '../composables'
import { QpModal, QpPagination, QpToast } from '../components'

// ==================== 类型定义 ====================
type PublishRecord = {
  id: number
  resourceSetId: number
  snapshotId: number
  env: string
  publishStatus: string
  publishMsg?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  operator: string
  resourceSetName?: string | null
  snapshotNo?: number | null
}

type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }

// ==================== 发布状态配置 ====================
const STATUS_CONFIG: Record<string, { label: string; class: string; icon: string }> = {
  SUCCESS: { label: '成功', class: 'status-success', icon: '✓' },
  FAILED: { label: '失败', class: 'status-failed', icon: '✗' },
  PENDING: { label: '进行中', class: 'status-pending', icon: '⋯' },
  ROLLBACK: { label: '已回滚', class: 'status-rollback', icon: '↩' },
}

// ==================== 组合式函数 ====================
const { message: toastMessage, showError } = useToast()

// ==================== 基础状态 ====================
const loading = ref(false)
const resourceSetId = ref<string>('')
const snapshotId = ref<string>('')
const statusFilter = ref<string>('')
const envFilter = ref<string>('')

// ==================== 数据状态 ====================
const allRecords = ref<PublishRecord[]>([])
const selectedRecord = ref<PublishRecord | null>(null)
const detailModalOpen = ref(false)

// ==================== 计算属性 ====================
const filteredRecords = computed(() => {
  let result = allRecords.value

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

  // 按状态过滤
  if (statusFilter.value) {
    result = result.filter((r) => r.publishStatus === statusFilter.value)
  }

  // 按环境过滤
  if (envFilter.value) {
    result = result.filter((r) => r.env === envFilter.value)
  }

  return result
})

// 提取所有可用的环境选项
const envOptions = computed(() => {
  const envSet = new Set(allRecords.value.map((r) => r.env))
  return Array.from(envSet).filter(Boolean).sort()
})

// 提取所有可用的状态选项
const statusOptions = computed(() => {
  const statusSet = new Set(allRecords.value.map((r) => r.publishStatus))
  return Array.from(statusSet).filter(Boolean).sort()
})

// 分页
const pagination = usePagination({ data: filteredRecords, defaultPageSize: 20 })

// ==================== 数据加载 ====================
async function loadRecords() {
  loading.value = true
  try {
    const res = await http.get('/api/publish-records', {
      params: {
        page: 1,
        pageSize: 500, // 一次性加载更多数据，前端分页
      },
    })
    allRecords.value = ((res.data?.data as PageResult<PublishRecord>)?.items ?? []) as PublishRecord[]
    pagination.resetPage()
  } catch (e: any) {
    showError(e, '加载发布记录失败')
  } finally {
    loading.value = false
  }
}

// ==================== 操作函数 ====================
function openDetail(record: PublishRecord) {
  selectedRecord.value = record
  detailModalOpen.value = true
}

function getStatusConfig(status: string) {
  return STATUS_CONFIG[status] || { label: status, class: 'status-default', icon: '○' }
}

function formatDuration(start?: string | null, end?: string | null): string {
  if (!start || !end) return '-'
  try {
    const startTime = new Date(start).getTime()
    const endTime = new Date(end).getTime()
    const duration = endTime - startTime
    if (duration < 1000) return `${duration}ms`
    if (duration < 60000) return `${(duration / 1000).toFixed(1)}s`
    return `${Math.floor(duration / 60000)}m ${Math.floor((duration % 60000) / 1000)}s`
  } catch {
    return '-'
  }
}

function clearFilters() {
  resourceSetId.value = ''
  snapshotId.value = ''
  statusFilter.value = ''
  envFilter.value = ''
  pagination.resetPage()
}

// ==================== 监听器 ====================
watch(
  () => [pagination.pageSize.value, resourceSetId.value, snapshotId.value, statusFilter.value, envFilter.value],
  () => pagination.resetPage()
)

// ==================== 生命周期 ====================
onMounted(loadRecords)
</script>

<template>
  <div class="qp-admin">
    <header class="topbar">
      <strong>发布记录</strong>
      <span class="sub">qp_publish_record</span>
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
              <span class="filter-label">环境</span>
              <select v-model="envFilter" class="select select-filter">
                <option value="">全部环境</option>
                <option v-for="env in envOptions" :key="env" :value="env">{{ env }}</option>
              </select>
            </label>
            <label class="filter-field">
              <span class="filter-label">状态</span>
              <select v-model="statusFilter" class="select select-filter">
                <option value="">全部状态</option>
                <option v-for="status in statusOptions" :key="status" :value="status">
                  {{ getStatusConfig(status).label }}
                </option>
              </select>
            </label>
          </div>
          <div class="filter-actions">
            <button class="btn primary" type="button" @click="loadRecords" :disabled="loading">
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
          <h3>📋 发布记录列表</h3>
          <span class="panel-count">共 {{ filteredRecords.length }} 条</span>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>序号</th>
                <th>ID</th>
                <th>资源集</th>
                <th>快照</th>
                <th>环境</th>
                <th>状态</th>
                <th>操作人</th>
                <th>开始时间</th>
                <th>耗时</th>
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
                  暂无发布记录
                </td>
              </tr>
              <tr
                v-for="(r, idx) in pagination.pagedData.value"
                :key="r.id"
                :class="{ selected: selectedRecord?.id === r.id }"
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
                  <span class="env-badge" :class="`env-${r.env?.toLowerCase()}`">{{ r.env }}</span>
                </td>
                <td>
                  <span class="status-badge" :class="getStatusConfig(r.publishStatus).class">
                    <span class="status-icon">{{ getStatusConfig(r.publishStatus).icon }}</span>
                    {{ getStatusConfig(r.publishStatus).label }}
                  </span>
                </td>
                <td>{{ r.operator }}</td>
                <td>{{ r.startedAt ?? '-' }}</td>
                <td>{{ formatDuration(r.startedAt, r.finishedAt) }}</td>
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
      title="发布详情"
      icon="📋"
      type="info"
      size="md"
      @close="detailModalOpen = false"
    >
      <div v-if="selectedRecord" class="detail-content">
        <div class="detail-header">
          <span class="detail-id">发布 #{{ selectedRecord.id }}</span>
          <span
            class="status-badge"
            :class="getStatusConfig(selectedRecord.publishStatus).class"
          >
            <span class="status-icon">{{ getStatusConfig(selectedRecord.publishStatus).icon }}</span>
            {{ getStatusConfig(selectedRecord.publishStatus).label }}
          </span>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">资源集</span>
            <span class="detail-value">
              {{ selectedRecord.resourceSetName || '-' }}
              <code v-if="selectedRecord.resourceSetId" style="margin-left: 4px; font-size: 11px; color: #9ca3af;">ID: {{ selectedRecord.resourceSetId }}</code>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">快照</span>
            <span class="detail-value">
              {{ selectedRecord.snapshotNo != null ? '#' + selectedRecord.snapshotNo : '-' }}
              <code v-if="selectedRecord.snapshotId" style="margin-left: 4px; font-size: 11px; color: #9ca3af;">ID: {{ selectedRecord.snapshotId }}</code>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">环境</span>
            <span class="detail-value">
              <span class="env-badge" :class="`env-${selectedRecord.env?.toLowerCase()}`">
                {{ selectedRecord.env }}
              </span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">操作人</span>
            <span class="detail-value">{{ selectedRecord.operator }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">开始时间</span>
            <span class="detail-value">{{ selectedRecord.startedAt ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">结束时间</span>
            <span class="detail-value">{{ selectedRecord.finishedAt ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">耗时</span>
            <span class="detail-value">{{ formatDuration(selectedRecord.startedAt, selectedRecord.finishedAt) }}</span>
          </div>
        </div>

        <div v-if="selectedRecord.publishMsg" class="detail-message">
          <span class="message-label">发布消息</span>
          <pre class="message-content">{{ selectedRecord.publishMsg }}</pre>
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

/* ==================== 环境标签 ==================== */
.env-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
}

.env-prod,
.env-production {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #991b1b;
}

.env-pre,
.env-staging {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
}

.env-test,
.env-testing {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
}

.env-dev,
.env-development {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
}

/* ==================== 状态标签 ==================== */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.status-icon {
  font-size: 10px;
}

.status-success {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
}

.status-failed {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #991b1b;
}

.status-pending {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
  animation: pulse 2s infinite;
}

.status-rollback {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
}

.status-default {
  background: #f3f4f6;
  color: #6b7280;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
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
}

.btn-detail:hover {
  background: #eff6ff;
  border-color: #60a5fa;
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

.detail-message {
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.message-label {
  display: block;
  font-size: 11px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.message-content {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  max-height: 200px;
  overflow: auto;
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
}
</style>
