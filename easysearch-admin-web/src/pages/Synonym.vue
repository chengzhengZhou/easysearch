<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { http } from '../services/http'

type ResourceSet = { id: number; name: string; scene: string; env: string; moduleType: string; currentVersionId?: number | null }
type Version = { id: number; versionNo: number; status: string }
type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }
type SynonymRule = { id: number; sourceText: string; direction: string; targetsJson: string; enabled: number; remark?: string | null }

const resourceSets = ref<ResourceSet[]>([])
const versions = ref<Version[]>([])
const resourceSetId = ref<number | null>(null)
const versionId = ref<number | null>(null)
const changeLog = ref('')

const q = ref('')
const rules = ref<SynonymRule[]>([])
const hint = ref('')

const isReady = computed(() => !!versionId.value)

async function loadResourceSets() {
  const res = await http.get('/api/resource-sets', { params: { moduleType: 'synonym', page: 1, pageSize: 200 } })
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
    params: { module: 'synonym', q: q.value || undefined, page: 1, pageSize: 200 },
  })
  rules.value = ((res.data?.data as PageResult<SynonymRule>)?.items ?? []) as SynonymRule[]
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
  try {
    const res = await http.post(`/api/versions/${versionId.value}/publish`)
    hint.value = `发布成功（recordId=${res.data?.data?.publishRecordId ?? '-'}；reload 预留）`
  } catch (e: any) {
    hint.value = e?.response?.data?.message ?? e?.message ?? '发布失败'
  }
}

async function rollback() {
  if (!resourceSetId.value) return
  const to = prompt('回滚到 versionId：')
  if (!to) return
  await http.post(`/api/resource-sets/${resourceSetId.value}/rollback`, undefined, { params: { toVersion: to } })
  hint.value = '已回滚（仅切指针；reload 预留）'
}

async function compare() {
  if (!versionId.value) return
  const base = prompt('对比基准 baseVersionId：')
  if (!base) return
  const res = await http.get(`/api/versions/${versionId.value}/diff`, { params: { baseVersionId: base, module: 'synonym' } })
  const d = res.data?.data
  hint.value = `diff：added=${d?.added?.length ?? 0} deleted=${d?.deleted?.length ?? 0} modified=${d?.modified?.length ?? 0}`
}

async function preview() {
  if (!versionId.value) return
  const input = prompt('输入 query 预览：') ?? ''
  if (!input) return
  const res = await http.post(`/api/versions/${versionId.value}/preview`, { query: input }, { params: { module: 'synonym' } })
  const hits = (res.data?.data?.hits ?? []) as string[]
  hint.value = `preview hits=${hits.length}`
}

async function addRule() {
  if (!versionId.value) return
  const source = prompt('source：') ?? ''
  const direction = prompt('direction（=>/<=/SYM）：') ?? '=>'
  const targets = prompt('targets（逗号分隔）：') ?? ''
  const arr = targets
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  await http.post(
    `/api/versions/${versionId.value}/rules`,
    { sourceText: source, direction, targetsJson: JSON.stringify(arr), enabled: 1 },
    { params: { module: 'synonym' } },
  )
  await loadRules()
}

async function removeRule(id: number) {
  if (!versionId.value) return
  await http.delete(`/api/versions/${versionId.value}/rules/${id}`, { params: { module: 'synonym' } })
  await loadRules()
}

onMounted(loadResourceSets)
</script>

<template>
  <div class="qp-admin">
    <header class="qpTopbar">
      <strong>同义词管理</strong>
      <span class="qpSub">{{ hint || 'qp_rule_synonym' }}</span>
    </header>

    <section class="qpPad">
      <div class="panel">
        <h3>资源上下文</h3>
        <div class="ctxGrid">
          <select v-model="resourceSetId" class="input" @change="loadVersions">
            <option :value="null">请选择资源集</option>
            <option v-for="r in resourceSets" :key="r.id" :value="r.id">{{ r.id }} - {{ r.name }} ({{ r.env }})</option>
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
            <button class="btn" type="button" :disabled="!resourceSetId" @click="rollback">回滚</button>
            <button class="btn ghost" type="button" :disabled="!isReady" @click="preview">预览</button>
          </div>
        </div>
        <div class="hint">规则表对应 <code>qp_rule_synonym</code>；编辑仅 Draft；Published/Archived 只读。</div>
      </div>

      <div class="panel">
        <div class="ruleToolbar">
          <input v-model="q" class="input" placeholder="搜索 source / targets_json" />
          <button class="btn" type="button" :disabled="!isReady" @click="loadRules">搜索</button>
          <button class="btn primary" type="button" :disabled="!isReady" @click="addRule">新增</button>
        </div>

        <div style="overflow: auto">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>source</th>
                <th>direction</th>
                <th>targets_json</th>
                <th>enabled</th>
                <th>op</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rules" :key="r.id">
                <td>{{ r.id }}</td>
                <td>{{ r.sourceText }}</td>
                <td>{{ r.direction }}</td>
                <td><code>{{ r.targetsJson }}</code></td>
                <td>{{ r.enabled }}</td>
                <td><button class="btn ghost" type="button" @click="removeRule(r.id)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* 仅保留本页差异：资源集选择列宽略倾向第一列 */
.ctxGrid {
  grid-template-columns: 1.2fr 1fr;
}
.table {
  min-width: 820px;
}
</style>

