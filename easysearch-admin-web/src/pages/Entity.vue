<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { http } from '../services/http'

type ResourceSet = { id: number; name: string; scene: string; env: string; moduleType: string }
type Version = { id: number; versionNo: number; status: string }
type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }
type EntityRule = {
  id: number
  entityText: string
  entityType: string
  normalizedValue: string
  aliasesJson: string | null
  attributesJson: string | null
  relationsJson: string | null
  idsJson: string | null
  enabled: number
}

const entityTypes = [
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

const resourceSets = ref<ResourceSet[]>([])
const versions = ref<Version[]>([])
const resourceSetId = ref<number | null>(null)
const versionId = ref<number | null>(null)
const changeLog = ref('')
const q = ref('')
const entityTypeFilter = ref<string>('')
const rules = ref<EntityRule[]>([])
const hint = ref('')

const isReady = computed(() => !!versionId.value)

const editOpen = ref(false)
const editTitle = ref('新增实体')
const edit = ref<Partial<EntityRule>>({
  entityType: 'BRAND',
  enabled: 1,
  aliasesJson: '[]',
  attributesJson: '{}',
  relationsJson: '{}',
  idsJson: '[]',
})

async function loadResourceSets() {
  const res = await http.get('/api/resource-sets', { params: { moduleType: 'entity', page: 1, pageSize: 200 } })
  resourceSets.value = (res.data?.data?.items ?? []) as ResourceSet[]
}

async function loadVersions() {
  versionId.value = null
  versions.value = []
  rules.value = []
  if (!resourceSetId.value) return
  const res = await http.get(`/api/resource-sets/${resourceSetId.value}/versions`, { params: { page: 1, pageSize: 50 } })
  versions.value = (res.data?.data?.items ?? []) as Version[]
}

async function loadRules() {
  if (!versionId.value) return
  const res = await http.get(`/api/versions/${versionId.value}/rules`, {
    params: {
      module: 'entity',
      q: q.value || undefined,
      entityType: entityTypeFilter.value || undefined,
      page: 1,
      pageSize: 200,
    },
  })
  rules.value = ((res.data?.data as PageResult<EntityRule>)?.items ?? []) as EntityRule[]
}

async function createDraft() {
  if (!resourceSetId.value) return
  const res = await http.post(`/api/resource-sets/${resourceSetId.value}/versions`, { changeLog: changeLog.value })
  versionId.value = (res.data?.data?.id ?? null) as number | null
  await loadVersions()
  await loadRules()
}

async function validate() {
  if (!versionId.value) return
  const res = await http.post(`/api/versions/${versionId.value}/validate`)
  hint.value = res.data?.data?.summary ?? 'OK'
}

async function publish() {
  if (!versionId.value) return
  const res = await http.post(`/api/versions/${versionId.value}/publish`)
  hint.value = `发布成功（recordId=${res.data?.data?.publishRecordId ?? '-'}；reload 预留）`
}

async function compare() {
  if (!versionId.value) return
  const base = prompt('对比基准 baseVersionId：')
  if (!base) return
  const res = await http.get(`/api/versions/${versionId.value}/diff`, { params: { baseVersionId: base, module: 'entity' } })
  const d = res.data?.data
  hint.value = `diff：added=${d?.added?.length ?? 0} deleted=${d?.deleted?.length ?? 0} modified=${d?.modified?.length ?? 0}`
}

async function preview() {
  if (!versionId.value) return
  const input = prompt('输入 query 预览：') ?? ''
  if (!input) return
  const res = await http.post(`/api/versions/${versionId.value}/preview`, { query: input }, { params: { module: 'entity' } })
  const hits = (res.data?.data?.hits ?? []) as string[]
  hint.value = `preview hits=${hits.length}`
}

function openCreate() {
  editTitle.value = '新增实体'
  edit.value = {
    entityType: 'BRAND',
    enabled: 1,
    aliasesJson: '[]',
    attributesJson: '{}',
    relationsJson: '{}',
    idsJson: '[]',
  }
  editOpen.value = true
}

function openEdit(r: EntityRule) {
  editTitle.value = `编辑实体 id=${r.id}`
  edit.value = { ...r }
  editOpen.value = true
}

async function saveEdit() {
  if (!versionId.value) return
  const payload = { ...edit.value }
  if (!payload.entityText || !payload.entityType || !payload.normalizedValue) {
    hint.value = 'entityText/entityType/normalizedValue 必填'
    return
  }
  if ((payload as any).id) {
    await http.put(`/api/versions/${versionId.value}/rules/${(payload as any).id}`, payload, { params: { module: 'entity' } })
  } else {
    await http.post(`/api/versions/${versionId.value}/rules`, payload, { params: { module: 'entity' } })
  }
  editOpen.value = false
  await loadRules()
}

async function removeRule(id: number) {
  if (!versionId.value) return
  await http.delete(`/api/versions/${versionId.value}/rules/${id}`, { params: { module: 'entity' } })
  await loadRules()
}

onMounted(loadResourceSets)
</script>

<template>
  <div class="qp-admin">
    <header class="qpTopbar">
      <strong>实体词典管理</strong>
      <span class="qpSub">{{ hint || 'qp_rule_entity' }}</span>
    </header>

    <section class="qpPad">
    <div class="panel">
      <h3>资源上下文</h3>
      <div class="ctxGrid3">
        <select v-model="resourceSetId" class="input" @change="loadVersions">
          <option :value="null">请选择资源集</option>
          <option v-for="r in resourceSets" :key="r.id" :value="r.id">{{ r.id }} - {{ r.name }} ({{ r.env }})</option>
        </select>
        <select v-model="entityTypeFilter" class="input" :disabled="!isReady" @change="loadRules">
          <option value="">全部类型</option>
          <option v-for="t in entityTypes" :key="t" :value="t">{{ t }}</option>
        </select>
        <select v-model="versionId" class="input" @change="loadRules">
          <option :value="null">请选择版本</option>
          <option v-for="v in versions" :key="v.id" :value="v.id">v{{ v.versionNo }} [{{ v.status }}] (id={{ v.id }})</option>
        </select>
      </div>

      <div class="ctxActions">
        <div class="actionGroup">
          <span class="actionTitle">编辑动作</span>
          <input v-model="changeLog" class="input" style="max-width: 260px" placeholder="变更说明" />
          <button class="btn" type="button" :disabled="!resourceSetId" @click="createDraft">建草稿</button>
          <button class="btn" type="button" :disabled="!isReady" @click="compare">对比</button>
        </div>
        <div class="actionGroup">
          <span class="actionTitle">发布动作</span>
          <button class="btn warn" type="button" :disabled="!isReady" @click="validate">校验</button>
          <button class="btn primary" type="button" :disabled="!isReady" @click="publish">发布</button>
          <button class="btn ghost" type="button" :disabled="!isReady" @click="preview">预览</button>
        </div>
      </div>
      <div class="hint">新增和编辑统一走弹窗表单；编辑仅 Draft。</div>
    </div>

    <div class="panel">
      <div class="ruleToolbar">
        <input v-model="q" class="input" placeholder="搜索 entity / normalizedValue / aliases" />
        <button class="btn" type="button" :disabled="!isReady" @click="loadRules">搜索</button>
        <button class="btn primary" type="button" :disabled="!isReady" @click="openCreate">新增一条</button>
      </div>

      <div style="overflow: auto">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>entity</th>
              <th>type</th>
              <th>normalizedValue</th>
              <th>aliases_json</th>
              <th>attributes_json</th>
              <th>op</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rules" :key="r.id">
              <td>{{ r.id }}</td>
              <td>{{ r.entityText }}</td>
              <td>{{ r.entityType }}</td>
              <td>{{ r.normalizedValue }}</td>
              <td><code>{{ r.aliasesJson }}</code></td>
              <td><code>{{ r.attributesJson }}</code></td>
              <td>
                <button class="btn" type="button" @click="openEdit(r)">编辑</button>
                <button class="btn danger" type="button" @click="removeRule(r.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="editOpen" class="modalMask">
      <div class="modal">
        <div class="modalHead">
          <strong>{{ editTitle }}</strong>
          <button class="btn" type="button" @click="editOpen = false">关闭</button>
        </div>
        <div class="modalBody">
          <div class="formGrid2">
            <label class="field">
              <span class="label">实体 *</span>
              <input v-model="edit.entityText" class="input" />
            </label>
            <label class="field">
              <span class="label">类型 *</span>
              <select v-model="edit.entityType" class="input">
                <option v-for="t in entityTypes" :key="t" :value="t">{{ t }}</option>
              </select>
            </label>
          </div>
          <label class="field" style="margin-top: 8px">
            <span class="label">归一化 *</span>
            <input v-model="edit.normalizedValue" class="input" />
          </label>

          <label class="field" style="margin-top: 8px">
            <span class="label">aliases_json (JSON 数组)</span>
            <textarea v-model="edit.aliasesJson" class="textarea"></textarea>
          </label>
          <label class="field" style="margin-top: 8px">
            <span class="label">attributes_json (JSON 对象)</span>
            <textarea v-model="edit.attributesJson" class="textarea"></textarea>
          </label>
          <label class="field" style="margin-top: 8px">
            <span class="label">ids_json (JSON 数组)</span>
            <textarea v-model="edit.idsJson" class="textarea"></textarea>
          </label>
        </div>
        <div class="modalFoot">
          <button class="btn" type="button" @click="editOpen = false">取消</button>
          <button class="btn primary" type="button" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>
    </section>
  </div>
</template>

<style scoped>
.ctxGrid3 {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 8px;
  margin-bottom: 8px;
}
.table {
  min-width: 980px;
}
.modalMask {
  position: fixed;
  inset: 0;
  background: rgba(10, 16, 28, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}
.modal {
  width: min(820px, 96vw);
  max-height: 90vh;
  overflow: auto;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
}
.modalHead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
}
.modalBody {
  padding: 12px;
}
.modalFoot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--border);
}
.formGrid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.label {
  font-size: 12px;
  color: var(--muted);
}
.textarea {
  min-height: 72px;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}
</style>

