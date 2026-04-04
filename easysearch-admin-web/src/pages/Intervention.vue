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

async function reload() {
  try {
    await http.post('/api/reload')
    publishLog.value.unshift(`${new Date().toLocaleString()} | Reload 触发`)
  } catch (e: any) {
    showToast(e?.response?.data?.message ?? e?.message ?? 'reload 失败')
  }
}

function openCompareWithOnline() {
  if (!resourceSetId.value || !currentSnapshotId.value) {
    showToast('无线上快照可对比')
    return
  }
  // 简化：直接对比当前编辑 vs 线上快照（实际实现需后端 diff 接口）
  diffSummary.value = '功能开发中：当前编辑 vs 线上快照对比'
  diffAdded.value = []
  diffDeleted.value = []
  diffModified.value = []
  compareModalOpen.value = true
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
        <div class="panel">
          <h3>资源上下文</h3>
          <div class="context-row context-row-filters">
            <div class="context-col">
              <div class="hint" style="margin-bottom: 6px">资源集</div>
              <select v-model="resourceSetId" class="select" @change="loadSnapshots">
                <option v-for="r in resourceSets" :key="r.id" :value="r.id">{{ r.name }}（scene={{ r.scene }}, env={{ r.env }}）</option>
              </select>
            </div>
            <div class="context-col">
              <div class="hint" style="margin-bottom: 6px">规则类型</div>
              <select v-model="mode" class="select">
                <option value="sentence">整句规则</option>
                <option value="term">词表规则</option>
              </select>
            </div>
          </div>

          <div class="context-actions context-actions-simple">
            <div class="action-group">
              <span class="action-group-title">发布区</span>
              <button class="btn warn" type="button" :disabled="!resourceSetId" @click="validate">校验</button>
              <button class="btn primary" type="button" :disabled="!resourceSetId" @click="openPublishConfirm">
                发布
                <span v-if="diffSummaryData?.hasChanges" class="badge-dot"></span>
              </button>
              <button class="btn" type="button" :disabled="!resourceSetId" @click="rollback">回滚</button>
              <button class="btn ghost" type="button" @click="reload">Reload</button>
            </div>
          </div>

          <div v-if="diffSummaryData?.hasChanges" class="unpublished-banner">
            <span class="unpublished-icon">⚠</span>
            <span>
              当前有未发布的编辑内容：
              <template v-if="diffSummaryData.addedCount > 0">新增 {{ diffSummaryData.addedCount }} 条</template>
              <template v-if="diffSummaryData.addedCount > 0 && (diffSummaryData.deletedCount > 0 || diffSummaryData.modifiedCount > 0)">、</template>
              <template v-if="diffSummaryData.deletedCount > 0">删除 {{ diffSummaryData.deletedCount }} 条</template>
              <template v-if="diffSummaryData.deletedCount > 0 && diffSummaryData.modifiedCount > 0">、</template>
              <template v-if="diffSummaryData.modifiedCount > 0">修改 {{ diffSummaryData.modifiedCount }} 条</template>
              <template v-if="diffSummaryData.noSnapshot">（尚无线上快照）</template>
              ，请及时校验并发布
            </span>
          </div>
        </div>

        <div class="panel">
          <div class="rule-toolbar">
            <input v-model="searchInput" class="input" placeholder="搜索 source / target" />
            <button class="btn primary" type="button" :disabled="!resourceSetId" @click="addRule">新增</button>
            <button class="btn" type="button" :disabled="!resourceSetId" @click="batchEnable(true)">启用</button>
            <button class="btn" type="button" :disabled="!resourceSetId" @click="batchEnable(false)">停用</button>
            <button class="btn danger" type="button" :disabled="!resourceSetId" @click="batchDelete">删除</button>
            <span class="hint" id="editabilityHint">可直接编辑</span>
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
                      <option value="EXACT">EXACT</option>
                      <option value="PREFIX">PREFIX</option>
                      <option value="CONTAINS">CONTAINS</option>
                    </select>
                  </td>
                  <td>
                    <input class="input" :disabled="!resourceSetId" type="number" v-model.number="(r as any).priority" @blur="updateRuleCell(r)" />
                  </td>
                  <td>
                    <input
                      type="checkbox"
                      :disabled="!resourceSetId"
                      :checked="Number((r as any).enabled) === 1"
                      @change="
                        ;(r as any).enabled = ($event.target as HTMLInputElement).checked ? 1 : 0
                        updateRuleCell(r)
                      "
                    />
                  </td>
                  <td class="actions">
                    <button class="btn" type="button" :disabled="!resourceSetId" @click="removeRule((r as any).id)">删除</button>
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
          <h3>校验结果</h3>
          <ul class="list">
            <li>{{ validateSummary }}</li>
          </ul>
        </div>

        <div class="panel">
          <h3>历史快照</h3>
          <div class="log-box">
            <div v-if="snapshots.length === 0" class="hint">尚无快照</div>
            <div v-else v-for="s in snapshots.slice(0, 20)" :key="s.id" class="snapshot-row">
              <span>#{{ s.snapshotNo }} | {{ s.ruleCount }}条 | {{ s.publishedBy }} | {{ s.publishedAt }}</span>
              <button class="btn btn-sm" type="button" @click="openSnapshotViewer(s)">查看</button>
            </div>
          </div>
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

    <!-- rollback picker -->
    <div class="modal-mask" :class="{ show: rollbackPickerOpen }">
      <div class="modal modal-sm">
        <div class="modal-head">
          <strong>选择回滚快照</strong>
          <button class="btn" type="button" @click="closeRollbackPicker">关闭</button>
        </div>
        <div class="modal-body">
          <div class="picker-hint">用历史快照覆盖当前规则并切换线上指针。回滚会丢失未发布的修改。</div>
          <select v-model="rollbackToSnapshotId" class="select">
            <option
              v-for="s in snapshots.filter((x) => x.id !== currentSnapshotId)"
              :key="s.id"
              :value="s.id"
            >
              #{{ s.snapshotNo }} | {{ s.ruleCount }}条 | {{ s.publishedAt }}
            </option>
          </select>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="closeRollbackPicker">取消</button>
          <button class="btn primary" type="button" :disabled="!rollbackToSnapshotId" @click="confirmRollback">确定回滚</button>
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
          <div class="picker-hint">将当前规则打快照并推线上。发布后 QP 需 reload 生效。</div>
          <div class="hint" style="margin-top: 8px">发布说明（change_log）：</div>
          <textarea v-model="publishChangeLog" class="input" style="width: 100%; height: 72px; resize: vertical" placeholder="请填写本次发布说明（可选）" />
          <div class="hint" style="margin-top: 8px">{{ publishValidateSummary }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="closePublishConfirm">取消</button>
          <button class="btn primary" type="button" :disabled="!resourceSetId" @click="confirmPublish">确认发布</button>
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
              <h4>新增规则</h4>
              <pre class="pre">{{ JSON.stringify(diffAdded, null, 2) }}</pre>
            </div>
            <div class="diff-col">
              <h4>删除规则</h4>
              <pre class="pre">{{ JSON.stringify(diffDeleted, null, 2) }}</pre>
            </div>
          </div>
          <div class="diff-col" style="margin-top: 10px">
            <h4>变更规则</h4>
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
          <div class="picker-hint">当前模式：{{ modeLabel }}</div>
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
          <button class="btn primary" type="button" :disabled="!resourceSetId" @click="submitAdd">确定</button>
        </div>
      </div>
    </div>

    <!-- snapshot viewer -->
    <div class="modal-mask" :class="{ show: snapshotViewerOpen }">
      <div class="modal modal-sm">
        <div class="modal-head">
          <strong>快照详情</strong>
          <button class="btn" type="button" @click="snapshotViewerOpen = false">关闭</button>
        </div>
        <div class="modal-body">
          <div v-if="viewingSnapshot">
            <div><strong>快照编号：</strong>#{{ viewingSnapshot.snapshotNo }}</div>
            <div><strong>规则数量：</strong>{{ viewingSnapshot.ruleCount }}</div>
            <div><strong>发布人：</strong>{{ viewingSnapshot.publishedBy }}</div>
            <div><strong>发布时间：</strong>{{ viewingSnapshot.publishedAt }}</div>
            <div><strong>说明：</strong>{{ viewingSnapshot.changeLog || '-' }}</div>
            <div class="hint" style="margin-top: 12px">快照规则详情查看功能开发中...</div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="snapshotViewerOpen = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.context-actions-simple {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.context-actions-simple .action-group {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #f9fafb;
  border-radius: 8px;
}
.context-actions-simple .action-group-title {
  font-size: 12px;
  color: #6b7280;
  margin-right: 6px;
}
.context-actions-simple :is(.btn, .chip) {
  height: 28px;
  line-height: 28px;
  padding: 0 10px;
  font-size: 12px;
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
.snapshot-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f3f4f6;
}
.snapshot-row .btn-sm {
  height: 24px;
  line-height: 24px;
  padding: 0 8px;
  font-size: 11px;
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
  .context-actions-simple {
    flex-direction: column;
  }
  .diff-grid {
    grid-template-columns: 1fr;
  }
}
.unpublished-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 8px 12px;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 6px;
  font-size: 12px;
  color: #92400e;
  line-height: 1.5;
}
.unpublished-icon {
  font-size: 14px;
  flex-shrink: 0;
}
.badge-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  background: #ef4444;
  border-radius: 50%;
  margin-left: 4px;
  vertical-align: middle;
  animation: badge-pulse 1.5s infinite;
}
@keyframes badge-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
