<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { http } from '../services/http'

type Mode = 'sentence' | 'term'
type ViewMode = 'view' | 'edit'
type ViewVersion = 'online' | number

const mode = ref<Mode>('sentence')
const resourceSetId = ref<number | null>(null)

// 统一原型：查看/编辑两态；查看=线上/历史只读，编辑=进入工作区（draft）
const viewMode = ref<ViewMode>('view')
const viewVersion = ref<ViewVersion>('online')

const searchInput = ref<string>('')
const pageSize = ref<number>(20)
const page = ref<number>(1)

const previewInput = ref<string>('')
const previewOutput = ref<string>('尚未预览')

const validateSummary = ref<string>('尚未校验')
const toast = ref<string>('')
const loading = ref(false)

type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }
type ResourceSet = { id: number; name: string; moduleType: string; env: string; scene: string; currentVersionId?: number | null }
type Version = { id: number; versionNo: number; status: string; changeLog?: string | null }
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
const versions = ref<Version[]>([])
const rules = ref<Array<SentenceRule | TermRule>>([])

const selectedIds = ref<Set<number>>(new Set())

const comparePickerOpen = ref(false)
const compareModalOpen = ref(false)
const compareBaseVersionId = ref<number | null>(null)
const diffSummary = ref<string>('-')
const diffAdded = ref<any[]>([])
const diffDeleted = ref<any[]>([])
const diffModified = ref<any[]>([])

const rollbackPickerOpen = ref(false)
const rollbackToVersionId = ref<number | null>(null)

const publishConfirmOpen = ref(false)
const publishChangeLog = ref<string>('')
const publishValidateSummary = ref<string>('校验：未执行')

const publishLog = ref<string[]>([])
const auditLog = ref<string[]>([])

const addModalOpen = ref(false)
const addForm = ref<any>({ sourceText: '', targetText: '', matchType: 'EXACT', priority: 0, enabled: 1 })

const modeLabel = computed(() => {
  return mode.value === 'sentence' ? '整句干预' : '词表干预'
})

const currentResourceSet = computed(() => resourceSets.value.find((r) => r.id === resourceSetId.value) ?? null)
const onlineVersionId = computed<number | null>(() => (currentResourceSet.value?.currentVersionId ?? null) as number | null)
const stagingVersion = computed<Version | null>(() => versions.value.find((v) => v.status === 'draft') ?? null)
const stagingVersionId = computed<number | null>(() => (stagingVersion.value?.id ?? null) as number | null)

const viewingVersionId = computed<number | null>(() => {
  if (viewMode.value === 'edit') return stagingVersionId.value
  if (viewVersion.value === 'online') return onlineVersionId.value
  return Number(viewVersion.value)
})

const currentVersion = computed(() => versions.value.find((v) => v.id === viewingVersionId.value) ?? null)
const isEditable = computed(() => viewMode.value === 'edit')

function versionLabel(id: number | null): string {
  if (!id) return '-'
  const v = versions.value.find((x) => x.id === id)
  if (!v) return '-'
  return `v${v.versionNo} (${v.status})`
}

const topContext = computed(() => {
  const rs = currentResourceSet.value
  if (!rs) return ''
  const onlineV = versions.value.find((v) => v.id === onlineVersionId.value) ?? null
  const stagingV = versions.value.find((v) => v.id === stagingVersionId.value) ?? null
  return `资源集：${rs.name} ｜ scene：${rs.scene} ｜ env：${rs.env} ｜ 线上：${onlineV ? `v${onlineV.versionNo}` : '-'} ｜ 工作区：${stagingV ? `v${stagingV.versionNo}` : '-'}`
})

async function ensureResourceSet(): Promise<number | null> {
  if (resourceSetId.value) return resourceSetId.value
  // Empty DB bootstrap: create a default intervention resource set.
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
      await loadVersions()
      return id
    }
    return null
  } catch (e: any) {
    toast.value = e?.response?.data?.message ?? e?.message ?? '初始化资源集失败'
    return null
  }
}

async function loadResourceSets() {
  const res = await http.get('/api/resource-sets', { params: { moduleType: 'intervention', page: 1, pageSize: 200 } })
  resourceSets.value = (res.data?.data?.items ?? []) as ResourceSet[]
  if (!resourceSetId.value && resourceSets.value.length) {
    resourceSetId.value = resourceSets.value[0].id
    await loadVersions()
  }
}

async function loadVersions() {
  versions.value = []
  rules.value = []
  selectedIds.value = new Set()
  if (!resourceSetId.value) return
  const res = await http.get(`/api/resource-sets/${resourceSetId.value}/versions`, { params: { page: 1, pageSize: 50 } })
  versions.value = (res.data?.data?.items ?? []) as Version[]
  // 默认：查看线上 current_version_id
  viewMode.value = 'view'
  viewVersion.value = 'online'
  await loadRules()
  await loadSideLogs()
}

async function loadRules() {
  rules.value = []
  if (!viewingVersionId.value) return
  const res = await http.get(`/api/versions/${viewingVersionId.value}/rules`, {
    params: { module: 'intervention', mode: mode.value, q: searchInput.value || undefined, page: 1, pageSize: 200 },
  })
  const pr = res.data?.data as PageResult<any>
  rules.value = pr?.items ?? []
  selectedIds.value = new Set()
  page.value = 1
}

async function ensureStagingExists() {
  if (!resourceSetId.value) {
    const id = await ensureResourceSet()
    if (!id) return
  }
  if (stagingVersionId.value) return
  toast.value = ''
  loading.value = true
  try {
    // createDraft: 如果已存在 draft 会直接返回 existingDraft（后端保证幂等）
    await http.post(`/api/resource-sets/${resourceSetId.value}/versions`, {
      changeLog: publishChangeLog.value || '',
      basedOnVersionId: onlineVersionId.value ?? undefined,
    })
    await loadVersions()
  } catch (e: any) {
    toast.value = e?.response?.data?.message ?? e?.message ?? '初始化工作区失败'
  } finally {
    loading.value = false
  }
}

async function setViewMode(next: ViewMode) {
  if (next === 'edit') {
    await ensureStagingExists()
    if (!stagingVersionId.value) return
    viewMode.value = 'edit'
  } else {
    viewMode.value = 'view'
    viewVersion.value = 'online'
  }
  validateSummary.value = '尚未校验'
  publishValidateSummary.value = '校验：未执行'
  previewOutput.value = '尚未预览'
  selectedIds.value = new Set()
  await loadRules()
}

async function resetStaging() {
  if (!resourceSetId.value) return
  if (!isEditable.value) return
  const ok = window.confirm('将重置工作区为线上版本，未发布变更将丢失。继续？')
  if (!ok) return
  loading.value = true
  toast.value = ''
  try {
    await http.post(`/api/resource-sets/${resourceSetId.value}/staging/reset`, {
      basedOnVersionId: onlineVersionId.value ?? undefined,
      changeLog: `reset staging to online @ ${new Date().toLocaleString()}`,
    })
    auditLog.value.unshift(`${new Date().toLocaleString()} | 重置工作区：resourceSetId=${resourceSetId.value} basedOn=${onlineVersionId.value ?? '-'}`)
    await loadVersions()
    // 重置后仍在编辑模式（工作区）
    viewMode.value = 'edit'
    await loadRules()
  } catch (e: any) {
    toast.value = e?.response?.data?.message ?? e?.message ?? '重置工作区失败'
  } finally {
    loading.value = false
  }
}

async function validate() {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  const res = await http.post(`/api/versions/${stagingVersionId.value}/validate`)
  validateSummary.value = res.data?.data?.summary ?? 'OK'
}

async function openPublishConfirm() {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  publishValidateSummary.value = '校验：未执行'
  publishConfirmOpen.value = true
  try {
    const res = await http.post(`/api/versions/${stagingVersionId.value}/validate`)
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
  if (!stagingVersionId.value) return
  const cl = publishChangeLog.value.trim()
  if (!cl) {
    toast.value = '请填写发布说明（change_log）'
    return
  }
  if (!stagingVersion.value?.changeLog) {
    toast.value = '提示：当前后端仅在“建草稿”时写入 changeLog；本次输入用于对齐原型文案'
  }
  try {
    const res = await http.post(`/api/versions/${stagingVersionId.value}/publish`)
    publishLog.value.unshift(`${new Date().toLocaleString()} | 发布成功 recordId=${res.data?.data?.publishRecordId ?? '-'}（reload 预留）`)
    auditLog.value.unshift(`${new Date().toLocaleString()} | 发布：versionId=${stagingVersionId.value} changeLog=${cl}`)
    closePublishConfirm()
    publishChangeLog.value = ''
    await loadResourceSets()
    await loadVersions()
    await loadSideLogs()
  } catch (e: any) {
    toast.value = e?.response?.data?.message ?? e?.message ?? '发布失败'
  }
}

async function rollback() {
  if (!resourceSetId.value) return
  rollbackToVersionId.value = null
  rollbackPickerOpen.value = true
}
function closeRollbackPicker() {
  rollbackPickerOpen.value = false
}
async function confirmRollback() {
  if (!resourceSetId.value) return
  if (!rollbackToVersionId.value) {
    toast.value = '请选择回滚版本'
    return
  }
  const to = rollbackToVersionId.value
  try {
    await http.post(`/api/resource-sets/${resourceSetId.value}/rollback`, undefined, { params: { toVersion: to } })
    publishLog.value.unshift(`${new Date().toLocaleString()} | 回滚到 versionId=${to}（仅切指针；reload 预留）`)
    auditLog.value.unshift(`${new Date().toLocaleString()} | 回滚：resourceSetId=${resourceSetId.value} toVersion=${to}`)
    closeRollbackPicker()
    await loadResourceSets()
    await loadVersions()
  } catch (e: any) {
    toast.value = e?.response?.data?.message ?? e?.message ?? '回滚失败'
  }
}

async function addRule() {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
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
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  const payload: any = {
    sourceText: String(addForm.value.sourceText ?? '').trim(),
    targetText: String(addForm.value.targetText ?? '').trim(),
    priority: Number(addForm.value.priority ?? 0),
    enabled: Number(addForm.value.enabled ?? 1),
  }
  if (!payload.sourceText || !payload.targetText) {
    toast.value = 'source/target 必填'
    return
  }
  if (mode.value === 'sentence') {
    payload.matchType = String(addForm.value.matchType ?? 'EXACT')
  }
  try {
    await http.post(`/api/versions/${stagingVersionId.value}/rules`, payload, {
      params: { module: 'intervention', mode: mode.value },
    })
    addModalOpen.value = false
    auditLog.value.unshift(`${new Date().toLocaleString()} | 新增规则（stagingVersionId=${stagingVersionId.value}）`)
    await loadRules()
  } catch (e: any) {
    toast.value = e?.response?.data?.message ?? e?.message ?? '新增失败'
  }
}

async function removeRule(id: number) {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  await http.delete(`/api/versions/${stagingVersionId.value}/rules/${id}`, { params: { module: 'intervention', mode: mode.value } })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 删除规则 id=${id}（stagingVersionId=${stagingVersionId.value}）`)
  await loadRules()
}

async function updateRuleCell(rule: any) {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  await http.put(`/api/versions/${stagingVersionId.value}/rules/${rule.id}`, rule, { params: { module: 'intervention', mode: mode.value } })
}

async function reload() {
  try {
    await http.post('/api/reload')
    publishLog.value.unshift(`${new Date().toLocaleString()} | Reload（reserved）`)
  } catch (e: any) {
    toast.value = e?.response?.data?.message ?? e?.message ?? 'reload 失败'
  }
}

function openComparePicker() {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  const online = onlineVersionId.value
  const candidates = versions.value.filter((v) => v.status !== 'draft')
  compareBaseVersionId.value = (candidates.find((v) => v.id === online)?.id ?? candidates[0]?.id ?? null) as number | null
  comparePickerOpen.value = true
}

async function runCompare() {
  if (!stagingVersionId.value || !compareBaseVersionId.value) return
  comparePickerOpen.value = false
  const res = await http.get(`/api/versions/${stagingVersionId.value}/diff`, {
    params: { baseVersionId: compareBaseVersionId.value, module: 'intervention', mode: mode.value },
  })
  const d = res.data?.data
  diffAdded.value = d?.added ?? []
  diffDeleted.value = d?.deleted ?? []
  diffModified.value = d?.modified ?? []
  diffSummary.value = `新增 ${diffAdded.value.length} 条，删除 ${diffDeleted.value.length} 条，变更 ${diffModified.value.length} 条（工作区相对基准）`
  compareModalOpen.value = true
}

async function preview() {
  if (!viewingVersionId.value) return
  const query = previewInput.value.trim()
  if (!query) return
  const res = await http.post(
    `/api/versions/${viewingVersionId.value}/preview`,
    { query },
    { params: { module: 'intervention', mode: mode.value } },
  )
  const hits = (res.data?.data?.hits ?? []) as string[]
  const out = res.data?.data?.output ?? query
  previewOutput.value = hits.length ? `命中：\n- ${hits.join('\n- ')}` : '无命中'
  previewOutput.value += `\n\noutput: ${out}`
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

watch(
  () => [pageSize.value, searchInput.value, mode.value, viewMode.value, viewVersion.value],
  async () => {
    page.value = 1
    if (viewingVersionId.value) await loadRules()
  },
)

async function batchEnable(enabled: boolean) {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  const ids = Array.from(selectedIds.value)
  if (!ids.length) return
  await http.post(`/api/versions/${stagingVersionId.value}/rules/${enabled ? 'batch-enable' : 'batch-disable'}`, { ids }, {
    params: { module: 'intervention', mode: mode.value },
  })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 批量${enabled ? '启用' : '停用'} ids=${ids.join(',')}`)
  await loadRules()
}

async function batchDelete() {
  if (!isEditable.value) return
  if (!stagingVersionId.value) return
  const ids = Array.from(selectedIds.value)
  if (!ids.length) return
  await http.post(`/api/versions/${stagingVersionId.value}/rules/batch-delete`, { ids }, { params: { module: 'intervention', mode: mode.value } })
  auditLog.value.unshift(`${new Date().toLocaleString()} | 批量删除 ids=${ids.join(',')}`)
  await loadRules()
}

async function loadSideLogs() {
  if (!resourceSetId.value) return
  try {
    const pub = await http.get('/api/publish-records', { params: { resourceSetId: resourceSetId.value, page: 1, pageSize: 20 } })
    const items = (pub.data?.data?.items ?? []) as any[]
    publishLog.value = items.map((i) => `${i.startedAt ?? ''} | ${i.publishStatus} | v=${i.versionId} | ${i.operator}`).slice(0, 30)
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
        <div class="panel">
          <h3>资源上下文</h3>
          <div class="context-row context-row-filters">
            <div class="context-col">
              <div class="hint" style="margin-bottom: 6px">资源集</div>
              <select v-model="resourceSetId" class="select" @change="loadVersions">
                <option v-for="r in resourceSets" :key="r.id" :value="r.id">{{ r.name }}（scene={{ r.scene }}, env={{ r.env }}）</option>
              </select>
            </div>
            <div class="context-col">
              <div class="hint" style="margin-bottom: 6px">版本切换</div>
              <select v-model="viewVersion" class="select" :disabled="viewMode !== 'view'" @change="loadRules">
                <option value="online">线上当前版本（current_version_id）</option>
                <option v-for="v in versions.filter((x) => x.status !== 'draft')" :key="v.id" :value="v.id">
                  历史：v{{ v.versionNo }} [{{ v.status }}]
                </option>
              </select>
            </div>
          </div>
          <div class="context-row context-viewing-summary">
            <div class="hint">
              当前查看：<strong>{{ versionLabel(viewingVersionId) }}</strong>
              <span style="margin-left: 10px">{{ viewMode === 'edit' ? '工作区可编辑' : '只读' }}</span>
            </div>
          </div>

          <div class="context-actions context-actions-three">
            <div class="action-group">
              <span class="action-group-title">模式</span>
              <button class="chip" :class="{ active: viewMode === 'view' }" type="button" @click="setViewMode('view')">查看</button>
              <button class="chip" :class="{ active: viewMode === 'edit' }" type="button" @click="setViewMode('edit')">编辑</button>
            </div>
            <div class="action-group">
              <span class="action-group-title">工作区</span>
              <button class="btn" type="button" :disabled="!isEditable || loading" @click="resetStaging">重置工作区</button>
              <button class="btn" type="button" :disabled="!isEditable" @click="openComparePicker">对比版本</button>
            </div>
            <div class="action-group">
              <span class="action-group-title">发布区</span>
              <button class="btn warn" type="button" :disabled="!isEditable" @click="validate">校验</button>
              <button class="btn primary" type="button" :disabled="!isEditable" @click="openPublishConfirm">发布</button>
              <button class="btn" type="button" :disabled="!resourceSetId" @click="rollback">回滚</button>
              <button class="btn ghost" type="button" @click="reload">Reload</button>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="chip-group">
            <button class="chip" :class="{ active: mode === 'sentence' }" type="button" @click="mode = 'sentence'">整句规则</button>
            <button class="chip" :class="{ active: mode === 'term' }" type="button" @click="mode = 'term'">词表规则</button>
          </div>

          <div class="rule-toolbar">
            <input v-model="searchInput" class="input" placeholder="搜索 source / target" />
            <button class="btn primary" type="button" :disabled="!isEditable" @click="addRule">新增</button>
            <button class="btn" type="button" :disabled="!isEditable" @click="batchEnable(true)">启用</button>
            <button class="btn" type="button" :disabled="!isEditable" @click="batchEnable(false)">停用</button>
            <button class="btn danger" type="button" :disabled="!isEditable" @click="batchDelete">删除</button>
            <span class="hint" id="editabilityHint">{{ isEditable ? '工作区可编辑' : '只读查看' }}</span>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>
                    <input
                      type="checkbox"
                      :disabled="!isEditable"
                      :checked="pagedRows.rows.length > 0 && pagedRows.rows.every((r) => selectedIds.has((r as any).id))"
                      @change="toggleAll(($event.target as HTMLInputElement).checked)"
                    />
                  </th>
                  <th>#</th>
                  <th>source</th>
                  <th>target</th>
                  <th v-if="mode === 'sentence'">matchType</th>
                  <th>priority</th>
                  <th>enabled</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="pagedRows.rows.length === 0">
                  <td :colspan="mode === 'sentence' ? 8 : 7" style="text-align: center; color: #5f6b7a; padding: 16px">
                    暂无规则
                  </td>
                </tr>
                <tr v-for="(r, idx) in pagedRows.rows" :key="(r as any).id" :class="{ selected: selectedIds.has((r as any).id) }">
                  <td>
                    <input
                      type="checkbox"
                      :disabled="!isEditable"
                      :checked="selectedIds.has((r as any).id)"
                      @change="toggleOne((r as any).id, ($event.target as HTMLInputElement).checked)"
                    />
                  </td>
                  <td>{{ (pagedRows.page - 1) * pageSize + idx + 1 }}</td>
                  <td>
                    <input class="input" :disabled="!isEditable" v-model="(r as any).sourceText" @blur="updateRuleCell(r)" />
                  </td>
                  <td>
                    <input class="input" :disabled="!isEditable" v-model="(r as any).targetText" @blur="updateRuleCell(r)" />
                  </td>
                  <td v-if="mode === 'sentence'">
                    <select class="select" :disabled="!isEditable" v-model="(r as any).matchType" @change="updateRuleCell(r)">
                      <option value="EXACT">EXACT</option>
                      <option value="PREFIX">PREFIX</option>
                      <option value="CONTAINS">CONTAINS</option>
                    </select>
                  </td>
                  <td>
                    <input class="input" :disabled="!isEditable" type="number" v-model.number="(r as any).priority" @blur="updateRuleCell(r)" />
                  </td>
                  <td>
                    <input
                      type="checkbox"
                      :disabled="!isEditable"
                      :checked="Number((r as any).enabled) === 1"
                      @change="
                        ;(r as any).enabled = ($event.target as HTMLInputElement).checked ? 1 : 0
                        updateRuleCell(r)
                      "
                    />
                  </td>
                  <td class="actions">
                    <button class="btn" type="button" :disabled="!isEditable" @click="removeRule((r as any).id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pager-bottom">
            <div class="hint">当前：{{ modeLabel }}；共 {{ pagedRows.total }} 条，{{ pagedRows.totalPages }} 页</div>
            <div class="pager-compact">
              <span class="hint">每页</span>
              <select v-model.number="pageSize" class="select" style="width: 72px">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
              <span class="hint">条，第</span>
              <input v-model.number="page" class="input" style="width: 68px" />
              <span class="hint">页</span>
              <button class="btn" type="button" :disabled="page <= 1" @click="page = Math.max(1, page - 1)">上一页</button>
              <button class="btn" type="button" :disabled="page >= pagedRows.totalPages" @click="page = Math.min(pagedRows.totalPages, page + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>

      <div>
        <div class="panel">
          <h3>预览</h3>
          <input v-model="previewInput" class="input" placeholder="输入 query" />
          <button class="btn primary" type="button" style="margin-top: 8px" :disabled="!viewingVersionId" @click="preview">执行预览</button>
          <div class="preview-box">
            <pre style="margin: 0; white-space: pre-wrap">{{ previewOutput }}</pre>
          </div>
        </div>

        <div class="panel">
          <h3>校验结果</h3>
          <ul class="list">
            <li>{{ validateSummary }}</li>
          </ul>
        </div>

        <div class="panel">
          <h3>发布记录</h3>
          <div class="log-box">
            <div v-if="publishLog.length === 0" class="hint">尚无记录</div>
            <div v-else v-for="(l, i) in publishLog.slice(0, 30)" :key="i">{{ l }}</div>
          </div>
        </div>

        <div class="panel">
          <h3>操作审计</h3>
          <div class="log-box">
            <div v-if="auditLog.length === 0" class="hint">尚无记录</div>
            <div v-else v-for="(l, i) in auditLog.slice(0, 30)" :key="i">{{ l }}</div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="toast" class="toast">{{ toast }}</div>

    <!-- compare picker -->
    <div class="modal-mask" :class="{ show: comparePickerOpen }">
      <div class="modal modal-sm">
        <div class="modal-head">
          <strong>选择对比版本</strong>
          <button class="btn" type="button" @click="comparePickerOpen = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="picker-hint">将「工作区（Staging）」的规则与所选「基准版本（线上/历史）」对比（按当前模式：整句/词表）。</div>
          <select v-model="compareBaseVersionId" class="select">
            <option v-for="v in versions.filter((x) => x.status !== 'draft')" :key="v.id" :value="v.id">
              {{ v.id === onlineVersionId ? '线上' : '历史' }}：v{{ v.versionNo }} [{{ v.status }}]
            </option>
          </select>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="comparePickerOpen = false">取消</button>
          <button class="btn primary" type="button" :disabled="!compareBaseVersionId" @click="runCompare">确定</button>
        </div>
      </div>
    </div>

    <!-- rollback picker -->
    <div class="modal-mask" :class="{ show: rollbackPickerOpen }">
      <div class="modal modal-sm">
        <div class="modal-head">
          <strong>选择回滚版本</strong>
          <button class="btn" type="button" @click="closeRollbackPicker">关闭</button>
        </div>
        <div class="modal-body">
          <div class="picker-hint">将线上生效版本切换为所选历史版本（published/archived），并触发 reload。建议回滚后同步重置工作区=线上。</div>
          <select v-model="rollbackToVersionId" class="select">
            <option
              v-for="v in versions.filter((x) => x.status !== 'draft' && x.id !== onlineVersionId)"
              :key="v.id"
              :value="v.id"
            >
              v{{ v.versionNo }} [{{ v.status }}]
            </option>
          </select>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="closeRollbackPicker">取消</button>
          <button class="btn primary" type="button" :disabled="!rollbackToVersionId" @click="confirmRollback">确定回滚</button>
        </div>
      </div>
    </div>

    <!-- publish confirm -->
    <div class="modal-mask" :class="{ show: publishConfirmOpen }">
      <div class="modal modal-sm">
        <div class="modal-head">
          <strong>确认发布</strong>
          <button class="btn" type="button" @click="closePublishConfirm">关闭</button>
        </div>
        <div class="modal-body">
          <div class="picker-hint">将工作区发布为线上生效版本（切换 current_version_id 并触发 reload），发布后会自动生成新的工作区。</div>
          <div class="hint" style="margin-top: 8px">发布说明（change_log）：</div>
          <textarea v-model="publishChangeLog" class="input" style="width: 100%; height: 72px; resize: vertical" placeholder="请填写本次发布说明（必填）" />
          <div class="hint" style="margin-top: 8px">{{ publishValidateSummary }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="closePublishConfirm">取消</button>
          <button class="btn primary" type="button" :disabled="!stagingVersionId" @click="confirmPublish">确认发布</button>
        </div>
      </div>
    </div>

    <!-- compare result -->
    <div class="modal-mask result" :class="{ show: compareModalOpen }">
      <div class="modal">
        <div class="modal-head">
          <strong>版本差异</strong>
          <button class="btn" type="button" @click="compareModalOpen = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="hint">{{ diffSummary }}</div>
          <div class="diff-grid">
            <div class="diff-col">
              <h4>新增规则（工作区相对基准）</h4>
              <pre class="pre">{{ JSON.stringify(diffAdded, null, 2) }}</pre>
            </div>
            <div class="diff-col">
              <h4>删除规则（工作区相对基准）</h4>
              <pre class="pre">{{ JSON.stringify(diffDeleted, null, 2) }}</pre>
            </div>
          </div>
          <div class="diff-col" style="margin-top: 10px">
            <h4>同 source 内容变更</h4>
            <pre class="pre">{{ JSON.stringify(diffModified, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </div>

    <!-- add rule -->
    <div class="modal-mask" :class="{ show: addModalOpen }">
      <div class="modal modal-sm">
        <div class="modal-head">
          <strong>新增规则</strong>
          <button class="btn" type="button" @click="addModalOpen = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="picker-hint">当前模式：{{ modeLabel }}（仅 Draft 可提交）。</div>
          <div style="display: grid; grid-template-columns: 1fr; gap: 8px">
            <input v-model="addForm.sourceText" class="input" placeholder="source_text" />
            <input v-model="addForm.targetText" class="input" placeholder="target_text" />
            <div v-if="mode === 'sentence'" style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px">
              <select v-model="addForm.matchType" class="select">
                <option value="EXACT">EXACT</option>
                <option value="PREFIX">PREFIX</option>
                <option value="CONTAINS">CONTAINS</option>
              </select>
              <input v-model.number="addForm.priority" class="input" type="number" placeholder="priority" />
            </div>
            <div v-else style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px">
              <input v-model.number="addForm.priority" class="input" type="number" placeholder="priority" />
              <select v-model.number="addForm.enabled" class="select">
                <option :value="1">enabled=1</option>
                <option :value="0">enabled=0</option>
              </select>
            </div>
            <div v-if="mode === 'sentence'" style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px">
              <select v-model.number="addForm.enabled" class="select">
                <option :value="1">enabled=1</option>
                <option :value="0">enabled=0</option>
              </select>
              <div class="hint" style="align-self: center">priority 范围建议 -999~999</div>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="addModalOpen = false">取消</button>
          <button class="btn primary" type="button" :disabled="!isEditable" @click="submitAdd">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.context-actions-three {
  grid-template-columns:
    minmax(100px, 0.5fr)
    minmax(240px, 1.1fr)
    minmax(340px, 1.25fr);
}
.context-actions-three :is(.btn, .chip) {
  height: 28px;
  line-height: 28px;
  padding: 0 10px;
  font-size: 12px;
}
.context-actions-three .chip {
  padding-top: 0;
  padding-bottom: 0;
}
.context-actions-three .action-group-title {
  line-height: 28px;
}
.context-actions-three .action-group:first-child {
  padding: 6px 8px;
  gap: 6px;
}
.context-actions-three .action-group:first-child .action-group-title {
  padding-right: 8px;
  margin-right: 0;
}
.rule-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) auto auto auto auto auto;
  gap: 6px;
  align-items: center;
  margin-bottom: 6px;
}
.rule-toolbar :is(.btn, .chip) {
  height: 28px;
  line-height: 28px;
  padding: 0 10px;
  font-size: 12px;
}
.pager-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}
.pager-compact {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pager-compact .btn {
  height: 28px;
  line-height: 28px;
  padding: 0 10px;
  font-size: 12px;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  min-width: 920px;
}
.toast {
  position: fixed;
  right: 16px;
  bottom: 16px;
  background: #111827;
  color: #fff;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 12px;
  max-width: 420px;
}
@media (max-width: 1024px) {
  .context-row-filters {
    grid-template-columns: 1fr;
  }
  .context-actions-three {
    grid-template-columns: 1fr;
  }
  .diff-grid {
    grid-template-columns: 1fr;
  }
}
</style>

