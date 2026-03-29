<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { http } from '../services/http'

type PageResult<T> = { page: number; pageSize: number; total: number; items: T[] }
type ResourceSet = {
  id: number
  moduleType: string
  scene: string
  env: string
  name: string
  status: number
  currentVersionId?: number | null
}

const health = ref<string>('未检测')
const loading = ref(false)
const items = ref<ResourceSet[]>([])

const form = ref({ moduleType: 'intervention', scene: 'default', env: 'dev', name: '干预-默认-dev' })
const hint = ref<string>('')

const hasItems = computed(() => items.value.length > 0)

async function ping() {
  try {
    await http.get('/api/health')
    health.value = 'OK（/api/health 可达）'
  } catch (e) {
    health.value = '失败（请确认 easysearch-admin 端口与 /api 路由）'
  }
}

async function load() {
  loading.value = true
  hint.value = ''
  try {
    const res = await http.get('/api/resource-sets', { params: { page: 1, pageSize: 50 } })
    items.value = (res.data?.data?.items ?? []) as ResourceSet[]
  } catch (e: any) {
    hint.value = e?.message ?? '加载失败'
  } finally {
    loading.value = false
  }
}

async function createResourceSet() {
  hint.value = ''
  try {
    await http.post('/api/resource-sets', form.value)
    await load()
    hint.value = '已创建'
  } catch (e: any) {
    hint.value = e?.response?.data?.message ?? e?.message ?? '创建失败'
  }
}

onMounted(async () => {
  await ping()
  await load()
})
</script>

<template>
  <div class="card">
    <h2>概览</h2>
    <p class="hint">
      这是 <code>easysearch-admin-web</code> 的 Vue 骨架。后续可以把 prototypes 里的页面交互迁移为 Vue
      组件与路由页面。
    </p>
    <div class="row">
      <button class="btn" type="button" @click="ping">测试 /api 连通性</button>
      <span class="hint">结果：{{ health }}</span>
    </div>

    <div style="margin-top: 16px">
      <h3>资源集</h3>
      <div class="hint">本页用于快速创建/查看 resource set（Phase1 先跑通干预模块）。</div>

      <div class="form" style="margin-top: 10px">
        <label class="field">
          <span class="label">moduleType</span>
          <input v-model="form.moduleType" class="input" />
        </label>
        <label class="field">
          <span class="label">scene</span>
          <input v-model="form.scene" class="input" />
        </label>
        <label class="field">
          <span class="label">env</span>
          <input v-model="form.env" class="input" />
        </label>
        <label class="field" style="grid-column: 1 / -1">
          <span class="label">name</span>
          <input v-model="form.name" class="input" />
        </label>
      </div>

      <div class="row">
        <button class="btn primary" type="button" @click="createResourceSet">创建资源集</button>
        <button class="btn" type="button" @click="load">刷新</button>
        <span class="hint">{{ loading ? '加载中…' : hint }}</span>
      </div>

      <div v-if="hasItems" style="margin-top: 10px; overflow: auto">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>module</th>
              <th>scene</th>
              <th>env</th>
              <th>name</th>
              <th>currentVersionId</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in items" :key="r.id">
              <td>{{ r.id }}</td>
              <td>{{ r.moduleType }}</td>
              <td>{{ r.scene }}</td>
              <td>{{ r.env }}</td>
              <td>{{ r.name }}</td>
              <td>{{ r.currentVersionId ?? '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.form {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.label {
  color: var(--muted);
  font-size: 12px;
}
.table {
  width: 100%;
  border-collapse: collapse;
}
.table th,
.table td {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
</style>

