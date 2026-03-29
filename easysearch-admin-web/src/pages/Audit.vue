<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { http } from '../services/http'

type AuditLog = {
  id: number
  userName: string
  action: string
  resourceSetId: number
  versionId?: number | null
  entityType?: string | null
  entityId?: number | null
  beforeJson?: string | null
  afterJson?: string | null
  createdAt?: string | null
}
type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }

const page = ref(1)
const pageSize = ref(20)
const resourceSetId = ref<string>('')
const versionId = ref<string>('')
const items = ref<AuditLog[]>([])
const selected = ref<AuditLog | null>(null)

const selectedBefore = computed(() => (selected.value?.beforeJson ? selected.value.beforeJson : '—'))
const selectedAfter = computed(() => (selected.value?.afterJson ? selected.value.afterJson : '—'))

async function load() {
  const res = await http.get('/api/audit-logs', {
    params: {
      page: page.value,
      pageSize: pageSize.value,
      resourceSetId: resourceSetId.value || undefined,
      versionId: versionId.value || undefined,
    },
  })
  items.value = ((res.data?.data as PageResult<AuditLog>)?.items ?? []) as AuditLog[]
  selected.value = items.value[0] ?? null
}

onMounted(load)
</script>

<template>
  <header class="qpTopbar">
    <strong>操作审计</strong>
    <span class="qpSub">qp_operation_log</span>
  </header>
  <section class="qpPad">
    <div class="panel">
      <div class="toolbar">
        <input v-model="resourceSetId" class="input" placeholder="resourceSetId" style="max-width: 160px" />
        <input v-model="versionId" class="input" placeholder="versionId" style="max-width: 160px" />
        <select v-model.number="pageSize" class="input" style="max-width: 120px">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
        <input v-model.number="page" class="input" type="number" min="1" style="max-width: 120px" />
        <button class="btn" type="button" @click="load">查询</button>
      </div>

      <div class="grid" style="margin-top: 10px">
        <div style="overflow: auto">
          <table class="table">
            <thead>
              <tr>
                <th>id</th>
                <th>time</th>
                <th>user</th>
                <th>action</th>
                <th>resourceSetId</th>
                <th>versionId</th>
                <th>entityType</th>
                <th>entityId</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in items" :key="r.id" :class="{ active: selected?.id === r.id }" @click="selected = r">
                <td>{{ r.id }}</td>
                <td>{{ r.createdAt ?? '-' }}</td>
                <td>{{ r.userName }}</td>
                <td>{{ r.action }}</td>
                <td>{{ r.resourceSetId }}</td>
                <td>{{ r.versionId ?? '-' }}</td>
                <td>{{ r.entityType ?? '-' }}</td>
                <td>{{ r.entityId ?? '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="diff">
          <div class="diffCol">
            <h4>before_json</h4>
            <pre class="pre">{{ selectedBefore }}</pre>
          </div>
          <div class="diffCol">
            <h4>after_json</h4>
            <pre class="pre">{{ selectedAfter }}</pre>
          </div>
        </div>
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
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  min-width: 820px;
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
.table tr.active {
  background: #f8fbff;
}
.diff {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
.diffCol h4 {
  margin: 0 0 6px;
  font-size: 13px;
}
.pre {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px;
  background: #fbfcff;
  font-size: 12px;
  line-height: 1.5;
  max-height: 360px;
  overflow: auto;
}
@media (max-width: 1024px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

