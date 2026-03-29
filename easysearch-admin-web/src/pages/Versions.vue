<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../services/http'

type ResourceSet = { id: number; name: string; moduleType: string; scene: string; env: string; currentVersionId?: number | null }
type Version = { id: number; versionNo: number; status: string; changeLog?: string | null; publishedAt?: string | null }
type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }

const moduleType = ref<string>('')
const resourceSets = ref<ResourceSet[]>([])
const resourceSetId = ref<number | null>(null)
const versions = ref<Version[]>([])
const hint = ref('')

async function loadResourceSets() {
  const res = await http.get('/api/resource-sets', { params: { moduleType: moduleType.value || undefined, page: 1, pageSize: 200 } })
  resourceSets.value = ((res.data?.data as PageResult<ResourceSet>)?.items ?? []) as ResourceSet[]
}

async function loadVersions() {
  versions.value = []
  if (!resourceSetId.value) return
  const res = await http.get(`/api/resource-sets/${resourceSetId.value}/versions`, { params: { page: 1, pageSize: 100 } })
  versions.value = ((res.data?.data as PageResult<Version>)?.items ?? []) as Version[]
}

async function rollback(toVersionId: number) {
  if (!resourceSetId.value) return
  try {
    await http.post(`/api/resource-sets/${resourceSetId.value}/rollback`, undefined, { params: { toVersion: toVersionId } })
    hint.value = `已回滚到 versionId=${toVersionId}（仅切指针；reload 预留）`
    await loadResourceSets()
  } catch (e: any) {
    hint.value = e?.response?.data?.message ?? e?.message ?? '回滚失败'
  }
}

onMounted(loadResourceSets)
</script>

<template>
  <header class="qpTopbar">
    <strong>版本管理</strong>
    <span class="qpSub">草稿 / 发布 / 回滚</span>
  </header>
  <section class="qpPad">
    <div class="panel">
      <div class="toolbar">
        <select v-model="moduleType" class="input" style="max-width: 180px" @change="loadResourceSets">
          <option value="">全部 module_type</option>
          <option value="intervention">intervention</option>
          <option value="synonym">synonym</option>
          <option value="entity">entity</option>
          <option value="token">token</option>
          <option value="meta">meta</option>
        </select>
        <select v-model="resourceSetId" class="input" style="min-width: 320px" @change="loadVersions">
          <option :value="null">请选择资源集</option>
          <option v-for="r in resourceSets" :key="r.id" :value="r.id">
            {{ r.id }} - {{ r.name }} ({{ r.moduleType }}/{{ r.scene }}/{{ r.env }}) current={{ r.currentVersionId ?? '-' }}
          </option>
        </select>
        <button class="btn" type="button" @click="loadVersions">刷新版本</button>
        <span class="hint">{{ hint }}</span>
      </div>

      <div style="overflow: auto; margin-top: 10px">
        <table class="table">
          <thead>
            <tr>
              <th>versionId</th>
              <th>versionNo</th>
              <th>status</th>
              <th>changeLog</th>
              <th>publishedAt</th>
              <th>op</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in versions" :key="v.id">
              <td>{{ v.id }}</td>
              <td>{{ v.versionNo }}</td>
              <td>{{ v.status }}</td>
              <td style="white-space: normal; min-width: 280px">{{ v.changeLog ?? '-' }}</td>
              <td>{{ v.publishedAt ?? '-' }}</td>
              <td>
                <button class="btn" type="button" @click="rollback(v.id)">回滚到此版本</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.qpTopbar {
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.qpSub {
  font-size: 13px;
  color: var(--muted);
}
.qpPad {
  padding: 16px;
}
.panel {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px;
}
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  min-width: 980px;
}
.table th,
.table td {
  border-bottom: 1px solid #edf1f7;
  padding: 6px;
  text-align: left;
  vertical-align: top;
  white-space: nowrap;
}
.table th {
  background: #f9fbff;
}
</style>

