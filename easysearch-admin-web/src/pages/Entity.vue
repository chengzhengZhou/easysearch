<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { http } from '../services/http'
import { useToast, usePagination, useSelection } from '../composables'
import { QpModal, QpPagination, QpLogPanel, QpSnapshotList, QpToast, QpAlert } from '../components'
import type { ResourceSet, Snapshot, EntityRule, DiffSummary, PageResult } from '../types'

// ==================== 常量定义 ====================
const ENTITY_TYPES = [
  'CATEGORY',
  'BRAND',
  'MODEL',
  'CPU',
  'RAM',
  'STORAGE',
  'PRICE',
  'CONDITION',
  'COLOR',
  'SIZE',
  'WEIGHT',
  'BATTERY',
  'CAMERA',
  'SCREEN',
  'OS',
  'NETWORK',
  'INTERFACE',
  'FEATURE',
  'ACCESSORY',
  'WARRANTY',
  'TAG',
  'UNKNOWN',
] as const

// ==================== 基础状态 ====================
const resourceSetId = ref<number | null>(null)
const searchInput = ref<string>('')
const entityTypeFilter = ref<string>('')

// ==================== 组合式函数 ====================
const { message: toastMessage, show: showToast, showError } = useToast()

const rules = ref<EntityRule[]>([])
const filteredRows = computed(() => {
  const k = searchInput.value.trim().toLowerCase()
  let result = rules.value
  if (entityTypeFilter.value) {
    result = result.filter((r) => r.entityType === entityTypeFilter.value)
  }
  if (!k) return result
  return result.filter((r) => {
    const s = String(r.entityText ?? '').toLowerCase()
    const n = String(r.normalizedValue ?? '').toLowerCase()
    const a = String(r.aliasesJson ?? '').toLowerCase()
    return s.includes(k) || n.includes(k) || a.includes(k)
  })
})

const pagination = usePagination({ data: filteredRows, defaultPageSize: 20 })
const selection = useSelection<EntityRule>()

// ==================== 数据状态 ====================
const validateSummary = ref<string>('尚未校验')
const resourceSets = ref<ResourceSet[]>([])
const snapshots = ref<Snapshot[]>([])

const diffSummaryData = ref<DiffSummary | null>(null)
const diffSummary = ref<string>('-')
const diffAdded = ref<any[]>([])
const diffDeleted = ref<any[]>([])
const diffModified = ref<any[]>([])

const publishLog = ref<string[]>([])
const auditLog = ref<string[]>([])

// ==================== 弹窗状态 ====================
const compareModalOpen = ref(false)
const rollbackPickerOpen = ref(false)
const rollbackToSnapshotId = ref<number | null>(null)
const publishConfirmOpen = ref(false)
const publishChangeLog = ref<string>('')
const publishValidateSummary = ref<string>('校验：未执行')
const addModalOpen = ref(false)
const addForm = ref<{
  entityText: string
  entityType: string
  normalizedValue: string
  aliases: string[]
  attributesJson: string
  relationsJson: string
  idsJson: string
  enabled: number
}>({
  entityText: '',
  entityType: 'BRAND',
  normalizedValue: '',
  aliases: [],
  attributesJson: '{}',
  relationsJson: '{}',
  idsJson: '[]',
  enabled: 1,
})
const addAliasInput = ref<string>('')

// 编辑弹窗状态
const editModalOpen = ref(false)
const editingRule = ref<EntityRule | null>(null)
const editForm = ref<{
  entityText: string
  entityType: string
  normalizedValue: string
  aliases: string[]
  attributesJson: string
  relationsJson: string
  idsJson: string
  enabled: number
}>({
  entityText: '',
  entityType: 'BRAND',
  normalizedValue: '',
  aliases: [],
  attributesJson: '{}',
  relationsJson: '{}',
  idsJson: '[]',
  enabled: 1,
})
const editAliasInput = ref<string>('')

// 别名操作函数
function parseAliasesJson(json: string | null): string[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    if (Array.isArray(arr)) return arr.filter(item => typeof item === 'string' && item.trim())
    return []
  } catch {
    return []
  }
}

function aliasesToJson(aliases: string[]): string {
  return JSON.stringify(aliases.filter(a => a.trim()))
}

// 别名操作函数 - 新增弹窗
function addAliasToAddForm() {
  const value = addAliasInput.value.trim()
  if (value && !addForm.value.aliases.includes(value)) {
    addForm.value.aliases.push(value)
  }
  addAliasInput.value = ''
}

function removeAliasFromAddForm(index: number) {
  addForm.value.aliases.splice(index, 1)
}

function handleAddAliasKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ',') {
    event.preventDefault()
    addAliasToAddForm()
  }
}

// 别名操作函数 - 编辑弹窗
function addAliasToEditForm() {
  const value = editAliasInput.value.trim()
  if (value && !editForm.value.aliases.includes(value)) {
    editForm.value.aliases.push(value)
  }
  editAliasInput.value = ''
}

function removeAliasFromEditForm(index: number) {
  editForm.value.aliases.splice(index, 1)
}

function handleEditAliasKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ',') {
    event.preventDefault()
    addAliasToEditForm()
  }
}
const snapshotViewerOpen = ref(false)
const viewingSnapshot = ref<Snapshot | null>(null)
const comparePickerOpen = ref(false)
const compareSnapshotA = ref<number | null>(null)
const compareSnapshotB = ref<number | null>(null)
const compareLoading = ref(false)

// ==================== 计算属性 ====================
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

// ==================== 数据加载 ====================
async function loadResourceSets() {
  const res = await http.get('/api/resource-sets', { params: { moduleType: 'entity', page: 1, pageSize: 200 } })
  resourceSets.value = (res.data?.data?.items ?? []) as ResourceSet[]
  if (!resourceSetId.value && resourceSets.value.length) {
    resourceSetId.value = resourceSets.value[0].id
    await loadSnapshots()
  }
}

async function loadSnapshots() {
  snapshots.value = []
  rules.value = []
  selection.clearSelection()
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
  const res = await http.get(`/api/resource-sets/${resourceSetId.value}/rules`, {
    params: { 
      module: 'entity', 
      q: searchInput.value || undefined, 
      entityType: entityTypeFilter.value || undefined,
      page: 1, 
      pageSize: 500 
    },
  })
  const pr = res.data?.data as PageResult<EntityRule>
  rules.value = pr?.items ?? []
  selection.clearSelection()
  pagination.resetPage()
}

async function loadSideLogs() {
  if (!resourceSetId.value) return
  try {
    const pub = await http.get('/api/publish-records', { params: { resourceSetId: resourceSetId.value, page: 1, pageSize: 20 } })
    const items = (pub.data?.data?.items ?? []) as any[]
    publishLog.value = items.map((i) => `${i.startedAt ?? ''} | ${i.publishStatus} | snapshot=${i.snapshotId} | ${i.operator}`).slice(0, 30)
  } catch { /* ignore */ }
  try {
    const aud = await http.get('/api/audit-logs', { params: { resourceSetId: resourceSetId.value, page: 1, pageSize: 20 } })
    const items = (aud.data?.data?.items ?? []) as any[]
    auditLog.value = items
      .map((i) => `${i.createdAt ?? ''} | ${i.action} | ${i.userName} | entity=${i.entityType ?? '-'}:${i.entityId ?? '-'}`)
      .slice(0, 30)
  } catch { /* ignore */ }
}

async function loadDiffSummary() {
  if (!resourceSetId.value) {
    diffSummaryData.value = null
    return
  }
  try {
    const res = await http.get(`/api/resource-sets/${resourceSetId.value}/diff-summary`, { params: { module: 'entity' } })
    diffSummaryData.value = (res.data?.data ?? null) as DiffSummary | null
  } catch {
    diffSummaryData.value = null
  }
}

// ==================== 操作函数 ====================
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

async function confirmPublish() {
  if (!resourceSetId.value) return
  const cl = publishChangeLog.value.trim()
  try {
    const res = await http.post(`/api/resource-sets/${resourceSetId.value}/publish`, { changeLog: cl })
    const snapshotId = res.data?.data?.snapshotId ?? '-'
    publishLog.value.unshift(`${new Date().toLocaleString()} | 发布成功 snapshotId=${snapshotId}`)
    auditLog.value.unshift(`${new Date().toLocaleString()} | 发布：changeLog=${cl}`)
    publishConfirmOpen.value = false
    publishChangeLog.value = ''
    await loadResourceSets()
    await loadSnapshots()
    await loadSideLogs()
  } catch (e: any) {
    showError(e, '发布失败')
  }
}

function openRollbackPicker() {
  if (!resourceSetId.value) return
  rollbackToSnapshotId.value = null
  rollbackPickerOpen.value = true
}

async function confirmRollback() {
  if (!resourceSetId.value || !rollbackToSnapshotId.value) {
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
    rollbackPickerOpen.value = false
    await loadResourceSets()
    await loadSnapshots()
  } catch (e: any) {
    showError(e, '回滚失败')
  }
}

function openAddModal() {
  if (!resourceSetId.value) return
  addForm.value = {
    entityText: '',
    entityType: 'BRAND',
    normalizedValue: '',
    aliases: [],
    attributesJson: '{}',
    relationsJson: '{}',
    idsJson: '[]',
    enabled: 1,
  }
  addAliasInput.value = ''
  addModalOpen.value = true
}

async function submitAdd() {
  if (!resourceSetId.value) return
  const entity = String(addForm.value.entityText ?? '').trim()
  const normalized = String(addForm.value.normalizedValue ?? '').trim()
  if (!entity || !normalized) {
    showToast('实体文本和归一化值必填')
    return
  }
  const payload = {
    entityText: entity,
    entityType: addForm.value.entityType,
    normalizedValue: normalized,
    aliasesJson: aliasesToJson(addForm.value.aliases),
    attributesJson: addForm.value.attributesJson || '{}',
    relationsJson: addForm.value.relationsJson || '{}',
    idsJson: addForm.value.idsJson || '[]',
    enabled: Number(addForm.value.enabled ?? 1),
  }
  try {
    await http.post(`/api/resource-sets/${resourceSetId.value}/rules`, payload, {
      params: { module: 'entity' },
    })
    addModalOpen.value = false
    auditLog.value.unshift(`${new Date().toLocaleString()} | 新增规则`)
    await loadRules()
    await loadDiffSummary()
  } catch (e: any) {
    showError(e, '新增失败')
  }
}

function openEditModal(rule: EntityRule) {
  if (!resourceSetId.value) return
  editingRule.value = rule
  editForm.value = {
    entityText: rule.entityText ?? '',
    entityType: rule.entityType ?? 'BRAND',
    normalizedValue: rule.normalizedValue ?? '',
    aliases: parseAliasesJson(rule.aliasesJson ?? '[]'),
    attributesJson: rule.attributesJson ?? '{}',
    relationsJson: rule.relationsJson ?? '{}',
    idsJson: rule.idsJson ?? '[]',
    enabled: Number(rule.enabled ?? 1),
  }
  editAliasInput.value = ''
  editModalOpen.value = true
}

async function submitEdit() {
  if (!resourceSetId.value || !editingRule.value) return
  const entity = String(editForm.value.entityText ?? '').trim()
  const normalized = String(editForm.value.normalizedValue ?? '').trim()
  if (!entity || !normalized) {
    showToast('实体文本和归一化值必填')
    return
  }
  const payload = {
    ...editingRule.value,
    entityText: entity,
    entityType: editForm.value.entityType,
    normalizedValue: normalized,
    aliasesJson: aliasesToJson(editForm.value.aliases),
    attributesJson: editForm.value.attributesJson || '{}',
    relationsJson: editForm.value.relationsJson || '{}',
    idsJson: editForm.value.idsJson || '[]',
    enabled: Number(editForm.value.enabled ?? 1),
  }
  try {
    await http.put(`/api/resource-sets/${resourceSetId.value}/rules/${editingRule.value.id}`, payload, {
      params: { module: 'entity' },
    })
    editModalOpen.value = false
    auditLog.value.unshift(`${new Date().toLocaleString()} | 编辑规则 id=${editingRule.value.id}`)
    await loadRules()
    await loadDiffSummary()
  } catch (e: any) {
    showError(e, '编辑失败')
  }
}

async function removeRule(id: number) {
  if (!resourceSetId.value) return
  await http.delete(`/api/resource-sets/${resourceSetId.value}/rules/${id}`, { params: { module: 'entity' } })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 删除规则 id=${id}`)
  await loadRules()
  await loadDiffSummary()
}

async function updateRuleCell(rule: EntityRule) {
  if (!resourceSetId.value) return
  await http.put(`/api/resource-sets/${resourceSetId.value}/rules/${rule.id}`, rule, { params: { module: 'entity' } })
  await loadDiffSummary()
}

function toggleEnabled(rule: EntityRule) {
  rule.enabled = Number(rule.enabled) === 1 ? 0 : 1
  updateRuleCell(rule)
}

async function batchEnable(enabled: boolean) {
  if (!resourceSetId.value) return
  const ids = selection.getSelectedIds()
  if (!ids.length) return
  await http.post(`/api/resource-sets/${resourceSetId.value}/rules/${enabled ? 'batch-enable' : 'batch-disable'}`, { ids }, {
    params: { module: 'entity' },
  })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 批量${enabled ? '启用' : '停用'} ids=${ids.join(',')}`)
  await loadRules()
  await loadDiffSummary()
}

async function batchDelete() {
  if (!resourceSetId.value) return
  const ids = selection.getSelectedIds()
  if (!ids.length) return
  await http.post(`/api/resource-sets/${resourceSetId.value}/rules/batch-delete`, { ids }, { params: { module: 'entity' } })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 批量删除 ids=${ids.join(',')}`)
  await loadRules()
  await loadDiffSummary()
}

// ==================== 版本对比 ====================
function openVersionCompare() {
  if (!resourceSetId.value) {
    showToast('请先选择资源集')
    return
  }
  compareSnapshotA.value = null
  compareSnapshotB.value = currentSnapshotId.value
  comparePickerOpen.value = true
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
    const params: any = { module: 'entity' }
    if (compareSnapshotA.value !== null) params.snapshotA = compareSnapshotA.value
    if (compareSnapshotB.value !== null) params.snapshotB = compareSnapshotB.value
    const res = await http.get(`/api/resource-sets/${resourceSetId.value}/snapshot-diff`, { params })
    const data = res.data?.data ?? {}
    diffAdded.value = data.added ?? []
    diffDeleted.value = data.deleted ?? []
    diffModified.value = data.modified ?? []
    const labelA = snapshotLabel(compareSnapshotA.value)
    const labelB = snapshotLabel(compareSnapshotB.value)
    diffSummary.value = `${labelA} vs ${labelB}（实体）—— 新增 ${diffAdded.value.length} 条、删除 ${diffDeleted.value.length} 条、修改 ${diffModified.value.length} 条`
    comparePickerOpen.value = false
    compareModalOpen.value = true
  } catch (e: any) {
    showError(e, '版本对比失败')
  } finally {
    compareLoading.value = false
  }
}

function openSnapshotViewer(snap: Snapshot) {
  viewingSnapshot.value = snap
  snapshotViewerOpen.value = true
}

// 解析 JSON 为可读字符串
function formatJson(json: string | null): string {
  if (!json) return '-'
  try {
    const arr = JSON.parse(json)
    if (Array.isArray(arr)) return arr.join(', ') || '-'
    return JSON.stringify(arr)
  } catch {
    return json
  }
}

// ==================== 监听器 ====================
watch(
  () => [pagination.pageSize.value, searchInput.value, entityTypeFilter.value],
  () => pagination.resetPage()
)

// ==================== 生命周期 ====================
onMounted(async () => {
  await loadResourceSets()
})
</script>

<template>
  <div class="qp-admin">
    <header class="topbar">
      <strong>实体词典管理</strong>
      <span class="sub">{{ topContext }}</span>
    </header>

    <section class="layout two-col">
      <div>
        <!-- 资源上下文栏 -->
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
              <select v-model="entityTypeFilter" class="select select-compact" @change="loadRules">
                <option value="">全部类型</option>
                <option v-for="t in ENTITY_TYPES" :key="t" :value="t">{{ t }}</option>
              </select>
            </label>
            <span class="context-sep"></span>
            <button class="btn btn-ctx warn" type="button" :disabled="!resourceSetId" @click="validate">校验</button>
            <button class="btn btn-ctx primary" type="button" :disabled="!resourceSetId" @click="openPublishConfirm">
              发布
              <span v-if="diffSummaryData?.hasChanges" class="badge-dot"></span>
            </button>
            <button class="btn btn-ctx" type="button" :disabled="!resourceSetId" @click="openRollbackPicker">回滚</button>
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

        <!-- 规则列表 -->
        <div class="panel panel-main">
          <div class="panel-header">
            <h3>规则列表</h3>
            <span class="panel-badge">实体词典</span>
          </div>
          <div class="rule-toolbar">
            <input v-model="searchInput" class="input search-input" placeholder="🔍 搜索实体 / 归一化值 / 别名..." />
            <div class="toolbar-actions">
              <button class="btn primary" type="button" :disabled="!resourceSetId" @click="openAddModal">
                <span class="btn-icon">+</span> 新增
              </button>
              <button class="btn" type="button" :disabled="!resourceSetId" @click="batchEnable(true)">批量启用</button>
              <button class="btn" type="button" :disabled="!resourceSetId" @click="batchEnable(false)">批量停用</button>
              <button class="btn btn-danger" type="button" :disabled="!resourceSetId" @click="batchDelete">批量删除</button>
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
                      :checked="selection.isAllSelected(pagination.pagedData.value as any)"
                      @change="selection.toggleAll(pagination.pagedData.value as any, ($event.target as HTMLInputElement).checked)"
                    />
                  </th>
                  <th>序号</th>
                  <th>实体文本</th>
                  <th>类型</th>
                  <th>归一化值</th>
                  <th>别名</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="pagination.pagedData.value.length === 0">
                  <td colspan="7" style="text-align: center; color: #5f6b7a; padding: 16px">
                    暂无规则
                  </td>
                </tr>
                <tr v-for="(r, idx) in pagination.pagedData.value" :key="r.id" :class="{ selected: selection.isSelected(r.id) }">
                  <td>
                    <input
                      type="checkbox"
                      :disabled="!resourceSetId"
                      :checked="selection.isSelected(r.id)"
                      @change="selection.toggleOne(r.id, ($event.target as HTMLInputElement).checked)"
                    />
                  </td>
                  <td>{{ (pagination.page.value - 1) * pagination.pageSize.value + idx + 1 }}</td>
                  <td>
                    <input class="input" :disabled="!resourceSetId" v-model="r.entityText" @blur="updateRuleCell(r)" />
                  </td>
                  <td>
                    <select class="select" :disabled="!resourceSetId" v-model="r.entityType" @change="updateRuleCell(r)">
                      <option v-for="t in ENTITY_TYPES" :key="t" :value="t">{{ t }}</option>
                    </select>
                  </td>
                  <td>
                    <input class="input" :disabled="!resourceSetId" v-model="r.normalizedValue" @blur="updateRuleCell(r)" />
                  </td>
                  <td>
                    <code class="aliases-code">{{ formatJson(r.aliasesJson) }}</code>
                  </td>
                  <td class="actions">
                    <button
                      class="btn btn-status"
                      :class="Number(r.enabled) === 1 ? 'btn-status-on' : 'btn-status-off'"
                      type="button"
                      :disabled="!resourceSetId"
                      @click="toggleEnabled(r)"
                      :title="Number(r.enabled) === 1 ? '点击停用' : '点击启用'"
                    >
                      {{ Number(r.enabled) === 1 ? '✓ 启用' : '✗ 停用' }}
                    </button>
                    <button class="btn btn-edit" type="button" :disabled="!resourceSetId" @click="openEditModal(r)" title="编辑此规则">编辑</button>
                    <button class="btn btn-del" type="button" :disabled="!resourceSetId" @click="removeRule(r.id)" title="删除此规则">删除</button>
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
            :disabled="!resourceSetId"
          />
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panels">
        <div class="panel panel-side">
          <div class="panel-header">
            <h3>📋 校验结果</h3>
          </div>
          <div class="validate-box" :class="{ 'validate-ok': validateSummary.includes('通过') || validateSummary.includes('ok'), 'validate-fail': validateSummary.includes('失败') || validateSummary.includes('错误') }">
            {{ validateSummary }}
          </div>
        </div>

        <QpSnapshotList :snapshots="snapshots" @view="openSnapshotViewer" />
        <QpLogPanel title="发布记录" icon="📝" :logs="publishLog" emptyText="暂无发布记录" />
        <QpLogPanel title="操作审计" icon="🔍" :logs="auditLog" emptyText="暂无审计记录" />
      </div>
    </section>

    <QpToast :message="toastMessage" />

    <!-- 回滚选择弹窗 -->
    <QpModal :open="rollbackPickerOpen" title="选择回滚快照" icon="↩" type="warn" @close="rollbackPickerOpen = false">
      <QpAlert type="warn" icon="⚠">回滚将用历史快照覆盖当前规则，未发布的修改将丢失。</QpAlert>
      <div class="form-field">
        <label class="form-label">选择目标快照</label>
        <select v-model="rollbackToSnapshotId" class="select select-full">
          <option :value="null" disabled>请选择要回滚到的快照...</option>
          <option v-for="s in snapshots.filter((x) => x.id !== currentSnapshotId)" :key="s.id" :value="s.id">
            #{{ s.snapshotNo }} · {{ s.ruleCount }}条规则 · {{ s.publishedAt }}
          </option>
        </select>
      </div>
      <template #footer>
        <button class="btn btn-cancel" type="button" @click="rollbackPickerOpen = false">取消</button>
        <button class="btn btn-warn" type="button" :disabled="!rollbackToSnapshotId" @click="confirmRollback">
          <span class="btn-icon-sm">↩</span> 确定回滚
        </button>
      </template>
    </QpModal>

    <!-- 发布确认弹窗 -->
    <QpModal :open="publishConfirmOpen" title="确认发布" icon="🚀" type="primary" @close="publishConfirmOpen = false">
      <QpAlert type="info" icon="ℹ">将当前规则打快照并推送到线上。</QpAlert>
      <div class="form-field">
        <label class="form-label">发布说明 <span class="form-optional">（可选）</span></label>
        <textarea v-model="publishChangeLog" class="textarea" rows="3" placeholder="请简要描述本次发布的变更内容..."></textarea>
      </div>
      <div class="validate-status" :class="{ 
        'status-pending': publishValidateSummary.includes('未执行'),
        'status-ok': publishValidateSummary.includes('通过') || publishValidateSummary.includes('ok') || publishValidateSummary.includes('OK'),
        'status-fail': publishValidateSummary.includes('失败') || publishValidateSummary.includes('错误')
      }">
        <span class="status-dot"></span>
        {{ publishValidateSummary }}
      </div>
      <template #footer>
        <button class="btn btn-cancel" type="button" @click="publishConfirmOpen = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="!resourceSetId" @click="confirmPublish">
          <span class="btn-icon-sm">🚀</span> 确认发布
        </button>
      </template>
    </QpModal>

    <!-- 版本对比选择弹窗 -->
    <QpModal :open="comparePickerOpen" title="版本对比" icon="⚖" type="primary" @close="comparePickerOpen = false">
      <QpAlert type="info" icon="ℹ">选择两个版本进行差异对比，对比维度：<strong>实体词典</strong></QpAlert>
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
      <template #footer>
        <button class="btn btn-cancel" type="button" @click="comparePickerOpen = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="compareLoading" @click="confirmCompare">
          <span v-if="compareLoading" class="btn-loading"></span>
          <span v-else class="btn-icon-sm">⚖</span>
          {{ compareLoading ? '对比中...' : '开始对比' }}
        </button>
      </template>
    </QpModal>

    <!-- 对比结果弹窗 -->
    <QpModal :open="compareModalOpen" title="版本差异" icon="📊" type="primary" size="md" :scrollable="true" :isResult="true" @close="compareModalOpen = false" :showFooter="false">
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
                <th>实体</th>
                <th>类型</th>
                <th>归一化</th>
                <th>别名</th>
                <th>enabled</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(r, i) in diffAdded" :key="i" class="diff-row-added">
                <td>{{ r.entityText }}</td>
                <td>{{ r.entityType }}</td>
                <td>{{ r.normalizedValue }}</td>
                <td><code>{{ formatJson(r.aliasesJson) }}</code></td>
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
                <th>实体</th>
                <th>类型</th>
                <th>归一化</th>
                <th>别名</th>
                <th>enabled</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(r, i) in diffDeleted" :key="i" class="diff-row-deleted">
                <td>{{ r.entityText }}</td>
                <td>{{ r.entityType }}</td>
                <td>{{ r.normalizedValue }}</td>
                <td><code>{{ formatJson(r.aliasesJson) }}</code></td>
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
                  <th>实体</th>
                  <th>类型</th>
                  <th>归一化</th>
                  <th>别名</th>
                  <th>enabled</th>
                </tr>
              </thead>
              <tbody>
                <tr class="diff-row-before">
                  <td class="diff-label">旧</td>
                  <td :class="{ 'diff-changed': m.before?.entityText !== m.after?.entityText }">{{ m.before?.entityText }}</td>
                  <td :class="{ 'diff-changed': m.before?.entityType !== m.after?.entityType }">{{ m.before?.entityType }}</td>
                  <td :class="{ 'diff-changed': m.before?.normalizedValue !== m.after?.normalizedValue }">{{ m.before?.normalizedValue }}</td>
                  <td :class="{ 'diff-changed': m.before?.aliasesJson !== m.after?.aliasesJson }"><code>{{ formatJson(m.before?.aliasesJson ?? '') }}</code></td>
                  <td :class="{ 'diff-changed': m.before?.enabled !== m.after?.enabled }">{{ m.before?.enabled }}</td>
                </tr>
                <tr class="diff-row-after">
                  <td class="diff-label">新</td>
                  <td :class="{ 'diff-changed': m.before?.entityText !== m.after?.entityText }">{{ m.after?.entityText }}</td>
                  <td :class="{ 'diff-changed': m.before?.entityType !== m.after?.entityType }">{{ m.after?.entityType }}</td>
                  <td :class="{ 'diff-changed': m.before?.normalizedValue !== m.after?.normalizedValue }">{{ m.after?.normalizedValue }}</td>
                  <td :class="{ 'diff-changed': m.before?.aliasesJson !== m.after?.aliasesJson }"><code>{{ formatJson(m.after?.aliasesJson ?? '') }}</code></td>
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
    </QpModal>

    <!-- 新增规则弹窗 -->
    <QpModal :open="addModalOpen" title="新增实体规则" icon="＋" type="primary" size="md" @close="addModalOpen = false">
      <QpAlert type="info" icon="📝">配置实体词典规则，用于实体识别和归一化。</QpAlert>
      <div class="form-grid">
        <div class="form-field">
          <label class="form-label">实体文本 <span class="form-required">*</span></label>
          <input v-model="addForm.entityText" class="input input-full" placeholder="输入实体文本，如：苹果" />
        </div>
        <div class="form-field">
          <label class="form-label">实体类型</label>
          <select v-model="addForm.entityType" class="select select-full">
            <option v-for="t in ENTITY_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="form-field">
          <label class="form-label">归一化值 <span class="form-required">*</span></label>
          <input v-model="addForm.normalizedValue" class="input input-full" placeholder="输入归一化值，如：Apple" />
        </div>
        <div class="form-field">
          <label class="form-label">状态</label>
          <select v-model.number="addForm.enabled" class="select select-full">
            <option :value="1">✓ 启用</option>
            <option :value="0">✗ 停用</option>
          </select>
        </div>
        <div class="form-field form-field-full">
          <label class="form-label">别名</label>
          <div class="alias-tags-container">
            <div class="alias-tags">
              <span v-for="(alias, idx) in addForm.aliases" :key="idx" class="alias-tag">
                {{ alias }}
                <button type="button" class="alias-tag-remove" @click="removeAliasFromAddForm(idx)">&times;</button>
              </span>
              <input
                v-model="addAliasInput"
                class="alias-input"
                placeholder="输入别名后按 Enter 或逗号添加"
                @keydown="handleAddAliasKeydown($event)"
                @blur="addAliasToAddForm()"
              />
            </div>
          </div>
          <div class="form-hint">按 Enter 或逗号分隔添加多个别名</div>
        </div>
        <div class="form-field form-field-full">
          <label class="form-label">属性（JSON 对象）</label>
          <textarea v-model="addForm.attributesJson" class="textarea" rows="2" placeholder='{"key":"value"}'></textarea>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-cancel" type="button" @click="addModalOpen = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="!resourceSetId" @click="submitAdd">
          <span class="btn-icon-sm">✓</span> 确定添加
        </button>
      </template>
    </QpModal>

    <!-- 编辑规则弹窗 -->
    <QpModal :open="editModalOpen" title="编辑实体规则" icon="✎" type="primary" size="md" @close="editModalOpen = false">
      <QpAlert type="info" icon="📝">修改实体词典规则，更改将在发布后生效。</QpAlert>
      <div class="form-grid">
        <div class="form-field">
          <label class="form-label">实体文本 <span class="form-required">*</span></label>
          <input v-model="editForm.entityText" class="input input-full" placeholder="输入实体文本，如：苹果" />
        </div>
        <div class="form-field">
          <label class="form-label">实体类型</label>
          <select v-model="editForm.entityType" class="select select-full">
            <option v-for="t in ENTITY_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="form-field">
          <label class="form-label">归一化值 <span class="form-required">*</span></label>
          <input v-model="editForm.normalizedValue" class="input input-full" placeholder="输入归一化值，如：Apple" />
        </div>
        <div class="form-field">
          <label class="form-label">状态</label>
          <select v-model.number="editForm.enabled" class="select select-full">
            <option :value="1">✓ 启用</option>
            <option :value="0">✗ 停用</option>
          </select>
        </div>
        <div class="form-field form-field-full">
          <label class="form-label">别名</label>
          <div class="alias-tags-container">
            <div class="alias-tags">
              <span v-for="(alias, idx) in editForm.aliases" :key="idx" class="alias-tag">
                {{ alias }}
                <button type="button" class="alias-tag-remove" @click="removeAliasFromEditForm(idx)">&times;</button>
              </span>
              <input
                v-model="editAliasInput"
                class="alias-input"
                placeholder="输入别名后按 Enter 或逗号添加"
                @keydown="handleEditAliasKeydown($event)"
                @blur="addAliasToEditForm()"
              />
            </div>
          </div>
          <div class="form-hint">按 Enter 或逗号分隔添加多个别名</div>
        </div>
        <div class="form-field form-field-full">
          <label class="form-label">属性（JSON 对象）</label>
          <textarea v-model="editForm.attributesJson" class="textarea" rows="2" placeholder='{"key":"value"}'></textarea>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-cancel" type="button" @click="editModalOpen = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="!resourceSetId" @click="submitEdit">
          <span class="btn-icon-sm">✓</span> 确定保存
        </button>
      </template>
    </QpModal>

    <!-- 快照详情弹窗 -->
    <QpModal :open="snapshotViewerOpen" title="快照详情" icon="📦" type="primary" @close="snapshotViewerOpen = false">
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
      <template #footer>
        <button class="btn btn-cancel" type="button" @click="snapshotViewerOpen = false">关闭</button>
      </template>
    </QpModal>
  </div>
</template>

<style scoped>
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

/* ==================== 表格样式 ==================== */
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 800px;
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

/* 别名显示 */
.aliases-code {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 2px 6px;
  background: #f3f4f6;
  border-radius: 4px;
  font-size: 12px;
  color: #4b5563;
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
.btn-edit {
  height: 26px;
  line-height: 24px;
  padding: 0 8px;
  font-size: 11px;
  background: #fff;
  color: #6b7280;
  border-color: #e5e7eb;
  border-radius: 4px;
  margin-right: 6px;
  transition: all 0.15s ease;
}
.btn-edit:hover {
  background: #eff6ff;
  color: #2563eb;
  border-color: #93c5fd;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1024px) {
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
}

/* ==================== 别名标签输入 ==================== */
.alias-tags-container {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  padding: 6px;
  transition: all 0.2s ease;
}
.alias-tags-container:focus-within {
  background: #fff;
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.alias-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.alias-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #93c5fd;
  border-radius: 6px;
  font-size: 13px;
  color: #1e40af;
  transition: all 0.15s ease;
}
.alias-tag:hover {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}
.alias-tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  margin-left: 2px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #3b82f6;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.alias-tag-remove:hover {
  background: #3b82f6;
  color: #fff;
}
.alias-input {
  flex: 1;
  min-width: 150px;
  border: none;
  background: transparent;
  padding: 4px 6px;
  font-size: 13px;
  outline: none;
}
.alias-input::placeholder {
  color: #9ca3af;
}
.form-hint {
  margin-top: 4px;
  font-size: 11px;
  color: #9ca3af;
}
</style>
