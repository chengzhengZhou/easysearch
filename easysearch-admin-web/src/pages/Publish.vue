<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../services/http'

type PublishRecord = {
  id: number
  resourceSetId: number
  versionId: number
  env: string
  publishStatus: string
  publishMsg?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  operator: string
}
type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }

const page = ref(1)
const pageSize = ref(20)
const resourceSetId = ref<string>('')
const versionId = ref<string>('')
const hint = ref('')
const items = ref<PublishRecord[]>([])

async function load() {
  const res = await http.get('/api/publish-records', {
    params: {
      page: page.value,
      pageSize: pageSize.value,
      resourceSetId: resourceSetId.value || undefined,
      versionId: versionId.value || undefined,
    },
  })
  items.value = ((res.data?.data as PageResult<PublishRecord>)?.items ?? []) as PublishRecord[]
}

onMounted(load)
</script>

<template>
  <header class="qpTopbar">
    <strong>发布记录</strong>
    <span class="qpSub">qp_publish_record</span>
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
        <span class="hint">{{ hint }}</span>
      </div>

      <div style="overflow: auto; margin-top: 10px">
        <table class="table">
          <thead>
            <tr>
              <th>id</th>
              <th>resourceSetId</th>
              <th>versionId</th>
              <th>env</th>
              <th>status</th>
              <th>operator</th>
              <th>startedAt</th>
              <th>finishedAt</th>
              <th>msg</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in items" :key="r.id">
              <td>{{ r.id }}</td>
              <td>{{ r.resourceSetId }}</td>
              <td>{{ r.versionId }}</td>
              <td>{{ r.env }}</td>
              <td>{{ r.publishStatus }}</td>
              <td>{{ r.operator }}</td>
              <td>{{ r.startedAt ?? '-' }}</td>
              <td>{{ r.finishedAt ?? '-' }}</td>
              <td style="white-space: normal; min-width: 320px">{{ r.publishMsg ?? '-' }}</td>
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
  min-width: 1100px;
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

