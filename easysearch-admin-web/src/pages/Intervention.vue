<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { http } from '../services/http'

type Mode = 'sentence' | 'term'

const mode = ref<Mode>('sentence')
const resourceSetId = ref<number | null>(null)

// 简化版：去掉 viewMode（查看/编辑两态）、stagingVersionId 等概念
// 页面始终可编辑，规则直接操作当前规则表

const searchInput = ref<string>('')
const pageSize = ref<number>(20)
const page = ref<number>(1)

const previewInput = ref<string>('')
const previewOutput = ref<string>('尚未预览')

const validateSummary = ref<string>('尚未校验')
const toast = ref<string>('')
const loading = ref(false)

// 自动清除 toast
function showToast(msg: string, duration = 3000) {
  toast.value = msg
  if (duration > 0) {
    setTimeout(() => {
      if (toast.value === msg) toast.value = ''
    }, duration)
  }
}

type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }
type ResourceSet = { id: number; name: string; moduleType: string; env: string; scene: string; currentSnapshotId?: number | null }
type Snapshot = { id: number; snapshotNo: number; changeLog?: string | null; ruleCount: number; publishedBy: string; publishedAt: string }
type SentenceRule = {
  id: number
  sourceText: string
  targetText: string
  matchType: string
  priority: number
  enabled: number
  remark?: string | null
}
type TermRule = { id: number; sourceText: string; targetText: string; priority: number; enabled: number; remark?: string | null }

const resourceSets = ref<ResourceSet[]>([])
const snapshots = ref<Snapshot[]>([])
const rules = ref<Array<SentenceRule | TermRule>>([])

const selectedIds = ref<Set<number>>(new Set())

const compareModalOpen = ref(false)
const diffSummary = ref<string>('-')
const diffAdded = ref<any[]>([])
const diffDeleted = ref<any[]>([])
const diffModified = ref<any[]>([])

const rollbackPickerOpen = ref(false)
const rollbackToSnapshotId = ref<number | null>(null)

const publishConfirmOpen = ref(false)
const publishChangeLog = ref<string>('')
const publishValidateSummary = ref<string>('校验：未执行')

const publishLog = ref<string[]>([])
const auditLog = ref<string[]>([])

const addModalOpen = ref(false)
const addForm = ref<any>({ sourceText: '', targetText: '', matchType: 'EXACT', priority: 0, enabled: 1 })

const snapshotViewerOpen = ref(false)
const viewingSnapshot = ref<Snapshot | null>(null)

// 版本对比
const comparePickerOpen = ref(false)
const compareSnapshotA = ref<number | null>(null) // null 表示"当前编辑"
const compareSnapshotB = ref<number | null>(null)
const compareLoading = ref(false)

type DiffSummary = {
  hasChanges: boolean
  addedCount: number
  deletedCount: number
  modifiedCount: number
  currentRuleCount: number
  noSnapshot: boolean
}
const diffSummaryData = ref<DiffSummary | null>(null)

const modeLabel = computed(() => {
  return mode.value === 'sentence' ? '整句干预' : '词表干预'
})

const currentResourceSet = computed(() => resourceSets.value.find((r) => r.id === resourceSetId.value) ?? null)
const currentSnapshotId = computed<number | null>(() => (currentResourceSet.value?.currentSnapshotId ?? null) as number | null)

const currentSnapshot = computed(() => snapshots.value.find((s) => s.id === currentSnapshotId.value) ?? null)

const topContext = computed(() => {
  const rs = currentResourceSet.value
  if (!rs) return ''
  const snap = currentSnapshot.value
  let base = `资源集：${rs.name} ｜ scene：${rs.scene} ｜ env：${rs.env} ｜ 线上快照：${snap ? `#${snap.snapshotNo}` : '无'}`
  const ds = diffSummaryData.value
  if (ds && ds.hasChanges) {
    const parts: string[] = []
    if (ds.addedCount > 0) parts.push(`新增${ds.addedCount}`)
    if (ds.deletedCount > 0) parts.push(`删除${ds.deletedCount}`)
    if (ds.modifiedCount > 0) parts.push(`修改${ds.modifiedCount}`)
    base += ` ｜ ⚠ 未发布变更：${parts.join('、')}`
  }
  return base
})

async function ensureResourceSet(): Promise<number | null> {
  if (resourceSetId.value) return resourceSetId.value
  // 空库初始化：创建默认的 intervention 资源集
  try {
    const res = await http.post('/api/resource-sets', {
      moduleType: 'intervention',
      scene: 'default',
      env: 'dev',
      name: 'intervention-default-dev',
    })
    const id = (res.data?.data?.id ?? null) as number | null
    if (id) {
      resourceSetId.value = id
      await loadResourceSets()
      await loadSnapshots()
      return id
    }
    return null
  } catch (e: any) {
    showToast(e?.response?.data?.message ?? e?.message ?? '初始化资源集失败')
    return null
  }
}

async function loadResourceSets() {
  const res = await http.get('/api/resource-sets', { params: { moduleType: 'intervention', page: 1, pageSize: 200 } })
  resourceSets.value = (res.data?.data?.items ?? []) as ResourceSet[]
  if (!resourceSetId.value && resourceSets.value.length) {
    resourceSetId.value = resourceSets.value[0].id
    await loadSnapshots()
  }
}

async function loadSnapshots() {
  snapshots.value = []
  rules.value = []
  selectedIds.value = new Set()
  if (!resourceSetId.value) return
  const res = await http.get(`/api/resource-sets/${resourceSetId.value}/snapshots`, { params: { page: 1, pageSize: 50 } })
  snapshots.value = (res.data?.data?.items ?? []) as Snapshot[]
  await loadRules()
  await loadSideLogs()
  await loadDiffSummary()
}

async function loadRules() {
  rules.value = []
  if (!resourceSetId.value) return
  // 直接读取当前规则表（无需 versionId）
  const res = await http.get(`/api/resource-sets/${resourceSetId.value}/rules`, {
    params: { module: 'intervention', mode: mode.value, q: searchInput.value || undefined, page: 1, pageSize: 200 },
  })
  const pr = res.data?.data as PageResult<any>
  rules.value = pr?.items ?? []
  selectedIds.value = new Set()
  page.value = 1
}

async function validate() {
  if (!resourceSetId.value) return
  const res = await http.post(`/api/resource-sets/${resourceSetId.value}/validate`)
  validateSummary.value = res.data?.data?.summary ?? 'OK'
}

async function openPublishConfirm() {
  if (!resourceSetId.value) return
  publishValidateSummary.value = '校验：未执行'
  publishConfirmOpen.value = true
  try {
    const res = await http.post(`/api/resource-sets/${resourceSetId.value}/validate`)
    const summary = res.data?.data?.summary ?? 'OK'
    validateSummary.value = summary
    publishValidateSummary.value = `校验：${summary}`
  } catch (e: any) {
    publishValidateSummary.value = `校验：失败（${e?.response?.data?.message ?? e?.message ?? 'unknown'}）`
  }
}

function closePublishConfirm() {
  publishConfirmOpen.value = false
}

async function confirmPublish() {
  if (!resourceSetId.value) return
  const cl = publishChangeLog.value.trim()
  try {
    const res = await http.post(`/api/resource-sets/${resourceSetId.value}/publish`, { changeLog: cl })
    const snapshotId = res.data?.data?.snapshotId ?? '-'
    publishLog.value.unshift(`${new Date().toLocaleString()} | 发布成功 snapshotId=${snapshotId}`)
    auditLog.value.unshift(`${new Date().toLocaleString()} | 发布：changeLog=${cl}`)
    closePublishConfirm()
    publishChangeLog.value = ''
    await loadResourceSets()
    await loadSnapshots()
    await loadSideLogs()
  } catch (e: any) {
    showToast(e?.response?.data?.message ?? e?.message ?? '发布失败')
  }
}

async function rollback() {
  if (!resourceSetId.value) return
  rollbackToSnapshotId.value = null
  rollbackPickerOpen.value = true
}

function closeRollbackPicker() {
  rollbackPickerOpen.value = false
}

async function confirmRollback() {
  if (!resourceSetId.value) return
  if (!rollbackToSnapshotId.value) {
    showToast('请选择回滚快照')
    return
  }
  const to = rollbackToSnapshotId.value
  const ok = window.confirm('回滚将用历史快照覆盖当前规则，未发布的修改将丢失。继续？')
  if (!ok) return
  try {
    await http.post(`/api/resource-sets/${resourceSetId.value}/rollback`, undefined, { params: { toSnapshot: to } })
    publishLog.value.unshift(`${new Date().toLocaleString()} | 回滚到 snapshotId=${to}`)
    auditLog.value.unshift(`${new Date().toLocaleString()} | 回滚：snapshotId=${to}`)
    closeRollbackPicker()
    await loadResourceSets()
    await loadSnapshots()
  } catch (e: any) {
    showToast(e?.response?.data?.message ?? e?.message ?? '回滚失败')
  }
}

async function addRule() {
  if (!resourceSetId.value) return
  addForm.value = {
    sourceText: '',
    targetText: '',
    matchType: 'EXACT',
    priority: 0,
    enabled: 1,
  }
  addModalOpen.value = true
}

async function submitAdd() {
  if (!resourceSetId.value) return
  const payload: any = {
    sourceText: String(addForm.value.sourceText ?? '').trim(),
    targetText: String(addForm.value.targetText ?? '').trim(),
    priority: Number(addForm.value.priority ?? 0),
    enabled: Number(addForm.value.enabled ?? 1),
  }
  if (!payload.sourceText || !payload.targetText) {
    showToast('source/target 必填')
    return
  }
  if (mode.value === 'sentence') {
    payload.matchType = String(addForm.value.matchType ?? 'EXACT')
  }
  try {
    await http.post(`/api/resource-sets/${resourceSetId.value}/rules`, payload, {
      params: { module: 'intervention', mode: mode.value },
    })
    addModalOpen.value = false
    auditLog.value.unshift(`${new Date().toLocaleString()} | 新增规则`)
    await loadRules()
    await loadDiffSummary()
  } catch (e: any) {
    showToast(e?.response?.data?.message ?? e?.message ?? '新增失败')
  }
}

async function removeRule(id: number) {
  if (!resourceSetId.value) return
  await http.delete(`/api/resource-sets/${resourceSetId.value}/rules/${id}`, { params: { module: 'intervention', mode: mode.value } })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 删除规则 id=${id}`)
  await loadRules()
  await loadDiffSummary()
}

async function updateRuleCell(rule: any) {
  if (!resourceSetId.value) return
  await http.put(`/api/resource-sets/${resourceSetId.value}/rules/${rule.id}`, rule, { params: { module: 'intervention', mode: mode.value } })
  await loadDiffSummary()
}

function toggleEnabled(rule: any) {
  rule.enabled = Number(rule.enabled) === 1 ? 0 : 1
  updateRuleCell(rule)
}

function openVersionCompare() {
  if (!resourceSetId.value) {
    showToast('请先选择资源集')
    return
  }
  compareSnapshotA.value = null // 默认"当前编辑"
  compareSnapshotB.value = currentSnapshotId.value
  comparePickerOpen.value = true
}

function closeComparePicker() {
  comparePickerOpen.value = false
}

function snapshotLabel(id: number | null): string {
  if (id === null) return '当前编辑'
  const s = snapshots.value.find((x) => x.id === id)
  return s ? `快照 #${s.snapshotNo}` : `快照 id=${id}`
}

async function confirmCompare() {
  if (!resourceSetId.value) return
  if (compareSnapshotA.value === compareSnapshotB.value) {
    showToast('两个版本不能相同')
    return
  }
  compareLoading.value = true
  try {
    const params: any = { mode: mode.value }
    if (compareSnapshotA.value !== null) params.snapshotA = compareSnapshotA.value
    if (compareSnapshotB.value !== null) params.snapshotB = compareSnapshotB.value
    const res = await http.get(`/api/resource-sets/${resourceSetId.value}/snapshot-diff`, { params })
    const data = res.data?.data ?? {}
    diffAdded.value = data.added ?? []
    diffDeleted.value = data.deleted ?? []
    diffModified.value = data.modified ?? []
    const labelA = snapshotLabel(compareSnapshotA.value)
    const labelB = snapshotLabel(compareSnapshotB.value)
    diffSummary.value = `${labelA} vs ${labelB}（${modeLabel.value}）—— 新增 ${diffAdded.value.length} 条、删除 ${diffDeleted.value.length} 条、修改 ${diffModified.value.length} 条`
    closeComparePicker()
    compareModalOpen.value = true
  } catch (e: any) {
    showToast(e?.response?.data?.message ?? e?.message ?? '版本对比失败')
  } finally {
    compareLoading.value = false
  }
}

function toggleAll(checked: boolean) {
  const next = new Set<number>()
  if (checked) {
    for (const r of pagedRows.value.rows) next.add((r as any).id)
  }
  selectedIds.value = next
}

function toggleOne(id: number, checked: boolean) {
  const next = new Set(selectedIds.value)
  if (checked) next.add(id)
  else next.delete(id)
  selectedIds.value = next
}

const filteredRows = computed(() => {
  const k = searchInput.value.trim().toLowerCase()
  if (!k) return rules.value
  return rules.value.filter((r: any) => {
    const s = String(r.sourceText ?? '').toLowerCase()
    const t = String(r.targetText ?? '').toLowerCase()
    return s.includes(k) || t.includes(k)
  })
})

const pagedRows = computed(() => {
  const size = Math.max(1, Number(pageSize.value) || 20)
  const totalPages = Math.max(1, Math.ceil(filteredRows.value.length / size))
  const p = Math.min(Math.max(1, Number(page.value) || 1), totalPages)
  const start = (p - 1) * size
  return { rows: filteredRows.value.slice(start, start + size), totalPages, page: p, total: filteredRows.value.length }
})

// 当 pageSize 或 searchInput 变化时重置页码
watch(
  () => [pageSize.value, searchInput.value],
  () => {
    page.value = 1
  },
)

// 当 mode 变化时重新加载规则
watch(
  () => mode.value,
  async () => {
    page.value = 1
    if (resourceSetId.value) await loadRules()
  },
)

async function batchEnable(enabled: boolean) {
  if (!resourceSetId.value) return
  const ids = Array.from(selectedIds.value)
  if (!ids.length) return
  await http.post(`/api/resource-sets/${resourceSetId.value}/rules/${enabled ? 'batch-enable' : 'batch-disable'}`, { ids }, {
    params: { module: 'intervention', mode: mode.value },
  })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 批量${enabled ? '启用' : '停用'} ids=${ids.join(',')}`)
  await loadRules()
  await loadDiffSummary()
}

async function batchDelete() {
  if (!resourceSetId.value) return
  const ids = Array.from(selectedIds.value)
  if (!ids.length) return
  await http.post(`/api/resource-sets/${resourceSetId.value}/rules/batch-delete`, { ids }, { params: { module: 'intervention', mode: mode.value } })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 批量删除 ids=${ids.join(',')}`)
  await loadRules()
  await loadDiffSummary()
}

async function loadSideLogs() {
  if (!resourceSetId.value) return
  try {
    const pub = await http.get('/api/publish-records', { params: { resourceSetId: resourceSetId.value, page: 1, pageSize: 20 } })
    const items = (pub.data?.data?.items ?? []) as any[]
    publishLog.value = items.map((i) => `${i.startedAt ?? ''} | ${i.publishStatus} | snapshot=${i.snapshotId} | ${i.operator}`).slice(0, 30)
  } catch {
    // ignore
  }
  try {
    const aud = await http.get('/api/audit-logs', { params: { resourceSetId: resourceSetId.value, page: 1, pageSize: 20 } })
    const items = (aud.data?.data?.items ?? []) as any[]
    auditLog.value = items
      .map((i) => `${i.createdAt ?? ''} | ${i.action} | ${i.userName} | entity=${i.entityType ?? '-'}:${i.entityId ?? '-'}`)
      .slice(0, 30)
  } catch {
    // ignore
  }
}

async function loadDiffSummary() {
  if (!resourceSetId.value) {
    diffSummaryData.value = null
    return
  }
  try {
    const res = await http.get(`/api/resource-sets/${resourceSetId.value}/diff-summary`, {
      params: { module: 'intervention' },
    })
    diffSummaryData.value = (res.data?.data ?? null) as DiffSummary | null
  } catch {
    diffSummaryData.value = null
  }
}

function openSnapshotViewer(snap: Snapshot) {
  viewingSnapshot.value = snap
  snapshotViewerOpen.value = true
}

onMounted(async () => {
  await loadResourceSets()
})
</script>

<template>
  <div class="qp-admin">
    <header class="topbar">
      <strong>干预规则管理</strong>
      <span class="sub">{{ topContext }}</span>
    </header>

    <section class="layout two-col">
      <div>
        <div class="panel panel-context">
          <div class="context-bar">
            <span class="context-title">资源上下文</span>
            <label class="context-field">
              <span class="context-label">资源集</span>
              <select v-model="resourceSetId" class="select select-compact" @change="loadSnapshots">
                <option v-for="r in resourceSets" :key="r.id" :value="r.id">{{ r.name }}（{{ r.scene }}/{{ r.env }}）</option>
              </select>
            </label>
            <label class="context-field">
              <span class="context-label">类型</span>
              <select v-model="mode" class="select select-compact">
                <option value="sentence">整句</option>
                <option value="term">词表</option>
              </select>
            </label>
            <span class="context-sep"></span>
            <button class="btn btn-ctx warn" type="button" :disabled="!resourceSetId" @click="validate">校验</button>
            <button class="btn btn-ctx primary" type="button" :disabled="!resourceSetId" @click="openPublishConfirm">
              发布
              <span v-if="diffSummaryData?.hasChanges" class="badge-dot"></span>
            </button>
            <button class="btn btn-ctx" type="button" :disabled="!resourceSetId" @click="rollback">回滚</button>
            <button class="btn btn-ctx" type="button" :disabled="!resourceSetId" @click="openVersionCompare">对比</button>
          </div>
          <div v-if="diffSummaryData?.hasChanges" class="unpublished-bar">
            ⚠ 未发布变更：
            <template v-if="diffSummaryData.addedCount > 0">+{{ diffSummaryData.addedCount }}</template>
            <template v-if="diffSummaryData.addedCount > 0 && (diffSummaryData.deletedCount > 0 || diffSummaryData.modifiedCount > 0)"> · </template>
            <template v-if="diffSummaryData.deletedCount > 0">-{{ diffSummaryData.deletedCount }}</template>
            <template v-if="diffSummaryData.deletedCount > 0 && diffSummaryData.modifiedCount > 0"> · </template>
            <template v-if="diffSummaryData.modifiedCount > 0">~{{ diffSummaryData.modifiedCount }}</template>
            <template v-if="diffSummaryData.noSnapshot">（无快照）</template>
            ，请及时发布
          </div>
        </div>

        <div class="panel panel-main">
          <div class="panel-header">
            <h3>规则列表</h3>
            <span class="panel-badge">{{ modeLabel }}</span>
          </div>
          <div class="rule-toolbar">
            <input v-model="searchInput" class="input search-input" placeholder="🔍 搜索源文本 / 目标文本..." />
            <div class="toolbar-actions">
              <button class="btn primary" type="button" :disabled="!resourceSetId" @click="addRule">
                <span class="btn-icon">+</span> 新增
              </button>
              <button class="btn" type="button" :disabled="!resourceSetId" @click="batchEnable(true)">批量启用</button>
              <button class="btn" type="button" :disabled="!resourceSetId" @click="batchEnable(false)">批量停用</button>
              <button class="btn danger" type="button" :disabled="!resourceSetId" @click="batchDelete">批量删除</button>
            </div>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>
                    <input
                      type="checkbox"
                      :disabled="!resourceSetId"
                      :checked="pagedRows.rows.length > 0 && pagedRows.rows.every((r) => selectedIds.has((r as any).id))"
                      @change="toggleAll(($event.target as HTMLInputElement).checked)"
                    />
                  </th>
                  <th>序号</th>
                  <th>源文本</th>
                  <th>目标文本</th>
                  <th v-if="mode === 'sentence'">匹配类型</th>
                  <th>优先级</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="pagedRows.rows.length === 0">
                  <td :colspan="mode === 'sentence' ? 7 : 6" style="text-align: center; color: #5f6b7a; padding: 16px">
                    暂无规则
                  </td>
                </tr>
                <tr v-for="(r, idx) in pagedRows.rows" :key="(r as any).id" :class="{ selected: selectedIds.has((r as any).id) }">
                  <td>
                    <input
                      type="checkbox"
                      :disabled="!resourceSetId"
                      :checked="selectedIds.has((r as any).id)"
                      @change="toggleOne((r as any).id, ($event.target as HTMLInputElement).checked)"
                    />
                  </td>
                  <td>{{ (pagedRows.page - 1) * pageSize + idx + 1 }}</td>
                  <td>
                    <input class="input" :disabled="!resourceSetId" v-model="(r as any).sourceText" @blur="updateRuleCell(r)" />
                  </td>
                  <td>
                    <input class="input" :disabled="!resourceSetId" v-model="(r as any).targetText" @blur="updateRuleCell(r)" />
                  </td>
                  <td v-if="mode === 'sentence'">
                    <select class="select" :disabled="!resourceSetId" v-model="(r as any).matchType" @change="updateRuleCell(r)">
                      <option value="EXACT">精确</option>
                      <option value="PREFIX">前缀</option>
                      <option value="CONTAINS">包含</option>
                    </select>
                  </td>
                  <td>
                    <input class="input" :disabled="!resourceSetId" type="number" v-model.number="(r as any).priority" @blur="updateRuleCell(r)" />
                  </td>
                  <td class="actions">
                    <button
                      class="btn btn-status"
                      :class="Number((r as any).enabled) === 1 ? 'btn-status-on' : 'btn-status-off'"
                      type="button"
                      :disabled="!resourceSetId"
                      @click="toggleEnabled(r)"
                      :title="Number((r as any).enabled) === 1 ? '点击停用' : '点击启用'"
                    >
                      {{ Number((r as any).enabled) === 1 ? '✓ 启用' : '✗ 停用' }}
                    </button>
                    <button class="btn btn-del" type="button" :disabled="!resourceSetId" @click="removeRule((r as any).id)" title="删除此规则">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pager-bottom">
            <div class="pager-info">
              <span class="pager-stat">共 <strong>{{ pagedRows.total }}</strong> 条</span>
              <span class="pager-divider">|</span>
              <span class="pager-stat">{{ pagedRows.totalPages }} 页</span>
            </div>
            <div class="pager-compact">
              <span class="pager-label">每页</span>
              <select v-model.number="pageSize" class="select select-sm">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
              <span class="pager-label">条</span>
              <span class="pager-divider">|</span>
              <span class="pager-label">第</span>
              <input v-model.number="page" class="input input-sm" type="number" min="1" :max="pagedRows.totalPages" />
              <span class="pager-label">页</span>
              <button class="btn btn-pager" type="button" :disabled="page <= 1" @click="page = Math.max(1, page - 1)">‹ 上一页</button>
              <button class="btn btn-pager" type="button" :disabled="page >= pagedRows.totalPages" @click="page = Math.min(pagedRows.totalPages, page + 1)">下一页 ›</button>
            </div>
          </div>
        </div>
      </div>

      <div class="side-panels">
        <div class="panel panel-side">
          <div class="panel-header">
            <h3>📋 校验结果</h3>
          </div>
          <div class="validate-box" :class="{ 'validate-ok': validateSummary.includes('通过'), 'validate-fail': validateSummary.includes('失败') || validateSummary.includes('错误') }">
            {{ validateSummary }}
          </div>
        </div>

        <div class="panel panel-side">
          <div class="panel-header">
            <h3>📦 历史快照</h3>
            <span class="panel-count">{{ snapshots.length }}</span>
          </div>
          <div class="log-box">
            <div v-if="snapshots.length === 0" class="empty-hint">暂无快照记录</div>
            <div v-else v-for="s in snapshots.slice(0, 20)" :key="s.id" class="snapshot-row">
              <div class="snapshot-info">
                <span class="snapshot-no">#{{ s.snapshotNo }}</span>
                <span class="snapshot-meta">{{ s.ruleCount }}条 · {{ s.publishedBy }}</span>
                <span class="snapshot-time">{{ s.publishedAt }}</span>
              </div>
              <button class="btn btn-sm" type="button" @click="openSnapshotViewer(s)">查看</button>
            </div>
          </div>
        </div>

        <div class="panel panel-side">
          <div class="panel-header">
            <h3>📝 发布记录</h3>
            <span class="panel-count">{{ publishLog.length }}</span>
          </div>
          <div class="log-box">
            <div v-if="publishLog.length === 0" class="empty-hint">暂无发布记录</div>
            <div v-else v-for="(l, i) in publishLog.slice(0, 30)" :key="i" class="log-item">{{ l }}</div>
          </div>
        </div>

        <div class="panel panel-side">
          <div class="panel-header">
            <h3>🔍 操作审计</h3>
            <span class="panel-count">{{ auditLog.length }}</span>
          </div>
          <div class="log-box">
            <div v-if="auditLog.length === 0" class="empty-hint">暂无审计记录</div>
            <div v-else v-for="(l, i) in auditLog.slice(0, 30)" :key="i" class="log-item">{{ l }}</div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="toast" class="toast">{{ toast }}</div>

    <!-- rollback picker -->
    <div class="modal-mask" :class="{ show: rollbackPickerOpen }">
      <div class="modal modal-sm modal-animate">
        <div class="modal-head">
          <div class="modal-title">
            <span class="modal-icon warn">↩</span>
            <strong>选择回滚快照</strong>
          </div>
          <button class="btn-close" type="button" @click="closeRollbackPicker" title="关闭">×</button>
        </div>
        <div class="modal-body">
          <div class="modal-alert warn">
            <span class="alert-icon">⚠</span>
            <span>回滚将用历史快照覆盖当前规则，未发布的修改将丢失。</span>
          </div>
          <div class="form-field">
            <label class="form-label">选择目标快照</label>
            <select v-model="rollbackToSnapshotId" class="select select-full">
              <option :value="null" disabled>请选择要回滚到的快照...</option>
              <option
                v-for="s in snapshots.filter((x) => x.id !== currentSnapshotId)"
                :key="s.id"
                :value="s.id"
              >
                #{{ s.snapshotNo }} · {{ s.ruleCount }}条规则 · {{ s.publishedAt }}
              </option>
            </select>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-cancel" type="button" @click="closeRollbackPicker">取消</button>
          <button class="btn btn-warn" type="button" :disabled="!rollbackToSnapshotId" @click="confirmRollback">
            <span class="btn-icon-sm">↩</span> 确定回滚
          </button>
        </div>
      </div>
    </div>

    <!-- publish confirm -->
    <div class="modal-mask" :class="{ show: publishConfirmOpen }">
      <div class="modal modal-sm modal-animate">
        <div class="modal-head">
          <div class="modal-title">
            <span class="modal-icon primary">🚀</span>
            <strong>确认发布</strong>
          </div>
          <button class="btn-close" type="button" @click="closePublishConfirm" title="关闭">×</button>
        </div>
        <div class="modal-body">
          <div class="modal-alert info">
            <span class="alert-icon">ℹ</span>
            <span>将当前规则打快照并推送到线上。</span>
          </div>
          <div class="form-field">
            <label class="form-label">发布说明 <span class="form-optional">（可选）</span></label>
            <textarea 
              v-model="publishChangeLog" 
              class="textarea" 
              rows="3"
              placeholder="请简要描述本次发布的变更内容..."
            ></textarea>
          </div>
          <div class="validate-status" :class="{ 
            'status-pending': publishValidateSummary.includes('未执行'),
            'status-ok': publishValidateSummary.includes('通过') || publishValidateSummary.includes('OK'),
            'status-fail': publishValidateSummary.includes('失败') || publishValidateSummary.includes('错误')
          }">
            <span class="status-dot"></span>
            {{ publishValidateSummary }}
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-cancel" type="button" @click="closePublishConfirm">取消</button>
          <button class="btn btn-primary" type="button" :disabled="!resourceSetId" @click="confirmPublish">
            <span class="btn-icon-sm">🚀</span> 确认发布
          </button>
        </div>
      </div>
    </div>

    <!-- version compare picker -->
    <div class="modal-mask" :class="{ show: comparePickerOpen }">
      <div class="modal modal-sm modal-animate">
        <div class="modal-head">
          <div class="modal-title">
            <span class="modal-icon primary">⚖</span>
            <strong>版本对比</strong>
          </div>
          <button class="btn-close" type="button" @click="closeComparePicker" title="关闭">×</button>
        </div>
        <div class="modal-body">
          <div class="modal-alert info">
            <span class="alert-icon">ℹ</span>
            <span>选择两个版本进行差异对比，对比维度：<strong>{{ modeLabel }}</strong></span>
          </div>
          <div class="compare-grid">
            <div class="form-field">
              <label class="form-label">版本 A（基准）</label>
              <select v-model="compareSnapshotA" class="select select-full">
                <option :value="null">📝 当前编辑</option>
                <option v-for="s in snapshots" :key="s.id" :value="s.id">
                  #{{ s.snapshotNo }} · {{ s.ruleCount }}条 · {{ s.publishedAt }}
                </option>
              </select>
            </div>
            <div class="compare-vs">VS</div>
            <div class="form-field">
              <label class="form-label">版本 B（对比）</label>
              <select v-model="compareSnapshotB" class="select select-full">
                <option :value="null">📝 当前编辑</option>
                <option v-for="s in snapshots" :key="s.id" :value="s.id">
                  #{{ s.snapshotNo }} · {{ s.ruleCount }}条 · {{ s.publishedAt }}
                </option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-cancel" type="button" @click="closeComparePicker">取消</button>
          <button class="btn btn-primary" type="button" :disabled="compareLoading" @click="confirmCompare">
            <span v-if="compareLoading" class="btn-loading"></span>
            <span v-else class="btn-icon-sm">⚖</span>
            {{ compareLoading ? '对比中...' : '开始对比' }}
          </button>
        </div>
      </div>
    </div>

    <!-- compare result -->
    <div class="modal-mask result" :class="{ show: compareModalOpen }">
      <div class="modal modal-animate">
        <div class="modal-head">
          <div class="modal-title">
            <span class="modal-icon primary">📊</span>
            <strong>版本差异</strong>
          </div>
          <button class="btn-close" type="button" @click="compareModalOpen = false" title="关闭">×</button>
        </div>
        <div class="modal-body modal-body-scroll">
          <div class="diff-summary-bar">
            <span class="diff-summary-icon">📊</span>
            {{ diffSummary }}
          </div>

          <div v-if="diffAdded.length > 0" class="diff-section">
            <h4 class="diff-section-title diff-added-title">
              <span class="diff-title-icon">＋</span>
              新增规则（{{ diffAdded.length }}）
            </h4>
            <div class="table-wrap">
              <table class="diff-table">
                <thead>
                  <tr>
                    <th>source</th>
                    <th>target</th>
                    <th v-if="mode === 'sentence'">matchType</th>
                    <th>priority</th>
                    <th>enabled</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(r, i) in diffAdded" :key="i" class="diff-row-added">
                    <td>{{ r.sourceText }}</td>
                    <td>{{ r.targetText }}</td>
                    <td v-if="mode === 'sentence'">{{ r.matchType }}</td>
                    <td>{{ r.priority }}</td>
                    <td>{{ r.enabled }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-if="diffDeleted.length > 0" class="diff-section">
            <h4 class="diff-section-title diff-deleted-title">
              <span class="diff-title-icon">－</span>
              删除规则（{{ diffDeleted.length }}）
            </h4>
            <div class="table-wrap">
              <table class="diff-table">
                <thead>
                  <tr>
                    <th>source</th>
                    <th>target</th>
                    <th v-if="mode === 'sentence'">matchType</th>
                    <th>priority</th>
                    <th>enabled</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(r, i) in diffDeleted" :key="i" class="diff-row-deleted">
                    <td>{{ r.sourceText }}</td>
                    <td>{{ r.targetText }}</td>
                    <td v-if="mode === 'sentence'">{{ r.matchType }}</td>
                    <td>{{ r.priority }}</td>
                    <td>{{ r.enabled }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-if="diffModified.length > 0" class="diff-section">
            <h4 class="diff-section-title diff-modified-title">
              <span class="diff-title-icon">～</span>
              修改规则（{{ diffModified.length }}）
            </h4>
            <div v-for="(m, i) in diffModified" :key="i" class="diff-modified-item">
              <div class="diff-modified-key">
                <span class="diff-key-icon">🔑</span>
                规则 ID: {{ m.key }}
              </div>
              <div class="table-wrap">
                <table class="diff-table">
                  <thead>
                    <tr>
                      <th class="th-label"></th>
                      <th>source</th>
                      <th>target</th>
                      <th v-if="mode === 'sentence'">matchType</th>
                      <th>priority</th>
                      <th>enabled</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr class="diff-row-before">
                      <td class="diff-label">旧</td>
                      <td :class="{ 'diff-changed': m.before?.sourceText !== m.after?.sourceText }">{{ m.before?.sourceText }}</td>
                      <td :class="{ 'diff-changed': m.before?.targetText !== m.after?.targetText }">{{ m.before?.targetText }}</td>
                      <td v-if="mode === 'sentence'" :class="{ 'diff-changed': m.before?.matchType !== m.after?.matchType }">{{ m.before?.matchType }}</td>
                      <td :class="{ 'diff-changed': m.before?.priority !== m.after?.priority }">{{ m.before?.priority }}</td>
                      <td :class="{ 'diff-changed': m.before?.enabled !== m.after?.enabled }">{{ m.before?.enabled }}</td>
                    </tr>
                    <tr class="diff-row-after">
                      <td class="diff-label">新</td>
                      <td :class="{ 'diff-changed': m.before?.sourceText !== m.after?.sourceText }">{{ m.after?.sourceText }}</td>
                      <td :class="{ 'diff-changed': m.before?.targetText !== m.after?.targetText }">{{ m.after?.targetText }}</td>
                      <td v-if="mode === 'sentence'" :class="{ 'diff-changed': m.before?.matchType !== m.after?.matchType }">{{ m.after?.matchType }}</td>
                      <td :class="{ 'diff-changed': m.before?.priority !== m.after?.priority }">{{ m.after?.priority }}</td>
                      <td :class="{ 'diff-changed': m.before?.enabled !== m.after?.enabled }">{{ m.after?.enabled }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div v-if="diffAdded.length === 0 && diffDeleted.length === 0 && diffModified.length === 0" class="diff-empty">
            <span class="diff-empty-icon">✓</span>
            <span>两个版本内容完全一致，无差异</span>
          </div>
        </div>
      </div>
    </div>

    <!-- add rule -->
    <div class="modal-mask" :class="{ show: addModalOpen }">
      <div class="modal modal-sm modal-animate">
        <div class="modal-head">
          <div class="modal-title">
            <span class="modal-icon primary">＋</span>
            <strong>新增规则</strong>
          </div>
          <button class="btn-close" type="button" @click="addModalOpen = false" title="关闭">×</button>
        </div>
        <div class="modal-body">
          <div class="modal-alert info">
            <span class="alert-icon">📝</span>
            <span>当前模式：<strong>{{ modeLabel }}</strong></span>
          </div>
          <div class="form-grid">
            <div class="form-field form-field-full">
              <label class="form-label">源文本 <span class="form-required">*</span></label>
              <input v-model="addForm.sourceText" class="input input-full" placeholder="输入需要匹配的源文本" />
            </div>
            <div class="form-field form-field-full">
              <label class="form-label">目标文本 <span class="form-required">*</span></label>
              <input v-model="addForm.targetText" class="input input-full" placeholder="输入替换后的目标文本" />
            </div>
            <div v-if="mode === 'sentence'" class="form-field">
              <label class="form-label">匹配类型</label>
              <select v-model="addForm.matchType" class="select select-full">
                <option value="EXACT">精确匹配</option>
                <option value="PREFIX">前缀匹配</option>
                <option value="CONTAINS">包含匹配</option>
              </select>
            </div>
            <div class="form-field">
              <label class="form-label">优先级</label>
              <input v-model.number="addForm.priority" class="input input-full" type="number" placeholder="0" />
              <span class="form-hint">建议范围 -999 ~ 999</span>
            </div>
            <div class="form-field">
              <label class="form-label">状态</label>
              <select v-model.number="addForm.enabled" class="select select-full">
                <option :value="1">✓ 启用</option>
                <option :value="0">✗ 停用</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-cancel" type="button" @click="addModalOpen = false">取消</button>
          <button class="btn btn-primary" type="button" :disabled="!resourceSetId" @click="submitAdd">
            <span class="btn-icon-sm">✓</span> 确定添加
          </button>
        </div>
      </div>
    </div>

    <!-- snapshot viewer -->
    <div class="modal-mask" :class="{ show: snapshotViewerOpen }">
      <div class="modal modal-sm modal-animate">
        <div class="modal-head">
          <div class="modal-title">
            <span class="modal-icon primary">📦</span>
            <strong>快照详情</strong>
          </div>
          <button class="btn-close" type="button" @click="snapshotViewerOpen = false" title="关闭">×</button>
        </div>
        <div class="modal-body">
          <div v-if="viewingSnapshot" class="snapshot-detail">
            <div class="snapshot-detail-header">
              <span class="snapshot-detail-no">#{{ viewingSnapshot.snapshotNo }}</span>
              <span class="snapshot-detail-count">{{ viewingSnapshot.ruleCount }} 条规则</span>
            </div>
            <div class="snapshot-detail-grid">
              <div class="detail-item">
                <span class="detail-label">发布人</span>
                <span class="detail-value">{{ viewingSnapshot.publishedBy }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">发布时间</span>
                <span class="detail-value">{{ viewingSnapshot.publishedAt }}</span>
              </div>
              <div class="detail-item detail-item-full">
                <span class="detail-label">发布说明</span>
                <span class="detail-value">{{ viewingSnapshot.changeLog || '无' }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-cancel" type="button" @click="snapshotViewerOpen = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 布局与面板 ==================== */
.panel-main {
  padding: 14px;
}
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
.panel-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  color: #4f46e5;
  border-radius: 12px;
  font-weight: 500;
}
.panel-count {
  font-size: 11px;
  padding: 2px 8px;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 10px;
}
.side-panels {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ==================== 资源上下文（紧凑版） ==================== */
.panel-context {
  padding: 8px 12px !important;
  margin-bottom: 10px;
}
.context-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.context-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  padding-right: 4px;
}
.context-field {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.context-label {
  font-size: 11px;
  color: #6b7280;
  white-space: nowrap;
}
.select-compact {
  height: 28px;
  padding: 0 8px;
  font-size: 12px;
  border-radius: 6px;
}
.context-sep {
  width: 1px;
  height: 20px;
  background: #d1d5db;
  margin: 0 4px;
  flex-shrink: 0;
}
.btn-ctx {
  height: 28px !important;
  line-height: 26px !important;
  padding: 0 10px !important;
  font-size: 12px !important;
  border-radius: 6px !important;
}
.unpublished-bar {
  margin-top: 6px;
  padding: 5px 10px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #f59e0b;
  border-radius: 6px;
  font-size: 12px;
  color: #92400e;
  line-height: 1.4;
}

/* ==================== 规则工具栏 ==================== */
.rule-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.search-input {
  flex: 1;
  min-width: 200px;
  max-width: 320px;
  height: 34px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  transition: all 0.2s ease;
}
.search-input:focus {
  background: #fff;
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.toolbar-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.toolbar-actions .btn {
  height: 32px;
  line-height: 32px;
  padding: 0 12px;
  font-size: 12px;
  border-radius: 6px;
  transition: all 0.15s ease;
}
.btn-icon {
  font-weight: 700;
  margin-right: 2px;
}

/* ==================== 表格样式 ==================== */
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 860px;
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
}
table tbody td {
  padding: 8px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}
table tbody tr {
  transition: background 0.15s ease;
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

/* 表格内输入框 */
table .input {
  width: 100%;
  height: 28px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  transition: all 0.15s ease;
}
table .input:hover {
  background: #f8fafc;
  border-color: #e5e7eb;
}
table .input:focus {
  background: #fff;
  border-color: #93c5fd;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}
table .select {
  height: 28px;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 12px;
}

/* ==================== 操作按钮 ==================== */
.actions {
  white-space: nowrap;
  text-align: center;
}
.btn-status {
  min-width: 60px;
  height: 26px;
  line-height: 24px;
  padding: 0 8px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
  margin-right: 6px;
  transition: all 0.15s ease;
}
.btn-status-on {
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
}
.btn-status-on:hover {
  background: #bbf7d0;
  border-color: #4ade80;
}
.btn-status-off {
  background: #fef3c7;
  color: #92400e;
  border-color: #fcd34d;
}
.btn-status-off:hover {
  background: #fde68a;
  border-color: #fbbf24;
}
.btn-del {
  height: 26px;
  line-height: 24px;
  padding: 0 8px;
  font-size: 11px;
  background: #fff;
  color: #6b7280;
  border-color: #e5e7eb;
  border-radius: 4px;
  transition: all 0.15s ease;
}
.btn-del:hover {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fca5a5;
}
.btn.danger {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;
}
.btn.danger:hover {
  background: #fee2e2;
  border-color: #fca5a5;
}

/* ==================== 分页区 ==================== */
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

/* ==================== 右侧面板 ==================== */
.validate-box {
  padding: 10px 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #6b7280;
}
.validate-box.validate-ok {
  background: #f0fdf4;
  border-color: #86efac;
  color: #166534;
}
.validate-box.validate-fail {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
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
.snapshot-row .btn-sm {
  height: 24px;
  line-height: 22px;
  padding: 0 10px;
  font-size: 11px;
  border-radius: 4px;
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

/* ==================== Toast ==================== */
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
  animation: toast-in 0.25s ease;
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

/* ==================== 发布状态标记 ==================== */
.badge-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  margin-left: 4px;
  vertical-align: middle;
  animation: badge-pulse 1.5s infinite;
}
@keyframes badge-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}

/* ==================== 弹窗优化 ==================== */
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

/* ==================== 弹窗内警告/提示 ==================== */
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

/* ==================== 表单元素 ==================== */
.form-field:last-child {
  margin-bottom: 0;
}
.form-field-full {
  grid-column: 1 / -1;
}
.form-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}
.form-required {
  color: #ef4444;
}
.form-optional {
  color: #9ca3af;
  font-weight: 400;
}
.form-hint {
  display: block;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.input-full,
.select-full {
  width: 100%;
}
.textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  resize: vertical;
  outline: none;
  font-family: inherit;
  transition: all 0.15s ease;
}
.textarea:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.textarea::placeholder {
  color: #9ca3af;
}

/* ==================== 弹窗按钮 ==================== */
.btn-cancel {
  background: #f9fafb;
  border-color: #e5e7eb;
  color: #6b7280;
}
.btn-cancel:hover {
  background: #f3f4f6;
  color: #374151;
}
.btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: transparent;
  color: #fff;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.3);
}
.btn-primary:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.4);
}
.btn-primary:disabled {
  background: #93c5fd;
  box-shadow: none;
  cursor: not-allowed;
}
.btn-warn {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-color: transparent;
  color: #fff;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(217, 119, 6, 0.3);
}
.btn-warn:hover {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  box-shadow: 0 2px 6px rgba(217, 119, 6, 0.4);
}
.btn-warn:disabled {
  background: #fcd34d;
  box-shadow: none;
  cursor: not-allowed;
}
.btn-icon-sm {
  margin-right: 4px;
}
.btn-loading {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: btn-spin 0.6s linear infinite;
  margin-right: 6px;
}
@keyframes btn-spin {
  to { transform: rotate(360deg); }
}

/* ==================== 校验状态 ==================== */
.validate-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  color: #6b7280;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ca3af;
}
.validate-status.status-pending .status-dot {
  background: #9ca3af;
}
.validate-status.status-ok {
  background: #f0fdf4;
  border-color: #86efac;
  color: #166534;
}
.validate-status.status-ok .status-dot {
  background: #22c55e;
}
.validate-status.status-fail {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
}
.validate-status.status-fail .status-dot {
  background: #ef4444;
}

/* ==================== 版本对比选择器 ==================== */
.compare-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 12px;
  align-items: end;
}
.compare-vs {
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  text-align: center;
  padding-bottom: 10px;
}

/* ==================== 快照详情 ==================== */
.snapshot-detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}
.snapshot-detail-no {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}
.snapshot-detail-count {
  font-size: 13px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4px 10px;
  border-radius: 12px;
}
.snapshot-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 14px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-item-full {
  grid-column: 1 / -1;
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
.snapshot-detail-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 8px;
  font-size: 12px;
  color: #6b7280;
}
.tip-icon {
  font-size: 14px;
}

/* ==================== 差异对比优化 ==================== */
.diff-summary-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #93c5fd;
  border-radius: 8px;
  font-size: 13px;
  color: #1e40af;
  margin-bottom: 16px;
}
.diff-summary-icon {
  font-size: 16px;
}
.diff-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 8px 0;
  padding: 6px 10px;
  border-radius: 6px;
}
.diff-title-icon {
  font-weight: 700;
}
.diff-modified-key {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
  padding: 4px 8px;
  background: #f9fafb;
  border-radius: 4px;
}
.diff-key-icon {
  font-size: 11px;
}
.th-label {
  width: 40px;
}
.diff-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 30px 20px;
  color: #6b7280;
  font-size: 14px;
}
.diff-empty-icon {
  font-size: 32px;
  color: #22c55e;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1024px) {
  .context-row-filters {
    grid-template-columns: 1fr;
  }
  .context-actions-simple {
    flex-direction: column;
  }
  .rule-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .search-input {
    max-width: none;
  }
  .toolbar-actions {
    justify-content: flex-start;
  }
  .pager-bottom {
    flex-direction: column;
    gap: 10px;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .compare-grid {
    grid-template-columns: 1fr;
  }
  .compare-vs {
    padding: 4px 0;
  }
}

/* ==================== 差异表格样式 ==================== */
.diff-section {
  margin-bottom: 16px;
}
.diff-added-title {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
}
.diff-deleted-title {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #991b1b;
}
.diff-modified-title {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
}
.diff-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.diff-table th,
.diff-table td {
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  text-align: left;
}
.diff-table th {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  font-weight: 600;
  font-size: 11px;
  color: #475569;
}
.diff-row-added td {
  background: #f0fdf4;
}
.diff-row-deleted td {
  background: #fef2f2;
}
.diff-row-before td {
  background: #fef2f2;
}
.diff-row-after td {
  background: #f0fdf4;
}
.diff-changed {
  font-weight: 700;
  color: #b45309;
  background: rgba(251, 191, 36, 0.15) !important;
}
.diff-label {
  font-weight: 600;
  color: #6b7280;
  width: 40px;
  text-align: center;
  font-size: 11px;
}
.diff-modified-item {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e5e7eb;
}
.diff-modified-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}
</style>
