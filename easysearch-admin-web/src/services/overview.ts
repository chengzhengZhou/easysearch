/**
 * 总览页面 API 服务
 */
import { http } from './http'
import type { PageResult, ResourceSet, Snapshot, PublishRecord, AuditLogItem, DiffSummary } from '../types'

export interface OverviewData {
  resourceSets: ResourceSet[]
  recentPublishRecords: PublishRecord[]
  recentAuditLogs: AuditLogItem[]
}

export interface ResourceSetStatsMap {
  [resourceSetId: number]: {
    ruleCount: number
    snapshot: Snapshot | null
    hasPendingChanges: boolean
  }
}

/**
 * 获取资源集列表
 */
export async function fetchResourceSets(page = 1, pageSize = 100): Promise<PageResult<ResourceSet>> {
  const res = await http.get('/api/resource-sets', { params: { page, pageSize } })
  return res.data?.data ?? { page, pageSize, total: 0, items: [] }
}

/**
 * 获取单个资源集详情
 */
export async function fetchResourceSet(id: number): Promise<ResourceSet | null> {
  try {
    const res = await http.get(`/api/resource-sets/${id}`)
    return res.data?.data ?? null
  } catch {
    return null
  }
}

/**
 * 获取资源集的快照列表
 */
export async function fetchSnapshots(resourceSetId: number, page = 1, pageSize = 50): Promise<PageResult<Snapshot>> {
  const res = await http.get(`/api/resource-sets/${resourceSetId}/snapshots`, { params: { page, pageSize } })
  return res.data?.data ?? { page, pageSize, total: 0, items: [] }
}

/**
 * 获取资源集的变更摘要
 */
export async function fetchDiffSummary(resourceSetId: number, module = 'intervention'): Promise<DiffSummary | null> {
  try {
    const res = await http.get(`/api/resource-sets/${resourceSetId}/diff-summary`, { params: { module } })
    return res.data?.data ?? null
  } catch {
    return null
  }
}

/**
 * 获取最近发布记录
 */
export async function fetchRecentPublishRecords(page = 1, pageSize = 10): Promise<PageResult<PublishRecord>> {
  const res = await http.get('/api/publish-records', { params: { page, pageSize } })
  return res.data?.data ?? { page, pageSize, total: 0, items: [] }
}

/**
 * 获取资源集的发布记录
 */
export async function fetchResourceSetPublishRecords(resourceSetId: number, page = 1, pageSize = 20): Promise<PageResult<PublishRecord>> {
  const res = await http.get('/api/publish-records', { params: { resourceSetId, page, pageSize } })
  return res.data?.data ?? { page, pageSize, total: 0, items: [] }
}

/**
 * 获取最近审计日志
 */
export async function fetchRecentAuditLogs(page = 1, pageSize = 10): Promise<PageResult<AuditLogItem>> {
  const res = await http.get('/api/audit-logs', { params: { page, pageSize } })
  return res.data?.data ?? { page, pageSize, total: 0, items: [] }
}

/**
 * 获取资源集的审计日志
 */
export async function fetchResourceSetAuditLogs(resourceSetId: number, page = 1, pageSize = 20): Promise<PageResult<AuditLogItem>> {
  const res = await http.get('/api/audit-logs', { params: { resourceSetId, page, pageSize } })
  return res.data?.data ?? { page, pageSize, total: 0, items: [] }
}

/**
 * 获取资源集的统计信息（规则数、快照、变更状态）
 */
export async function fetchResourceSetStats(resourceSetId: number): Promise<{
  ruleCount: number
  snapshot: Snapshot | null
  hasPendingChanges: boolean
}> {
  let ruleCount = 0
  let snapshot: Snapshot | null = null
  let hasPendingChanges = false

  try {
    // 获取最新快照
    const snapshots = await fetchSnapshots(resourceSetId, 1, 1)
    if (snapshots.items.length > 0) {
      snapshot = snapshots.items[0]
      ruleCount = snapshot.ruleCount ?? 0
    }
  } catch { /* ignore */ }

  try {
    // 获取变更摘要
    const diffSummary = await fetchDiffSummary(resourceSetId)
    if (diffSummary?.hasChanges) {
      hasPendingChanges = true
    }
    // 如果没有快照，使用当前规则数
    if (!snapshot && diffSummary?.currentRuleCount) {
      ruleCount = diffSummary.currentRuleCount
    }
  } catch { /* ignore */ }

  return { ruleCount, snapshot, hasPendingChanges }
}

/**
 * 批量获取多个资源集的统计信息
 */
export async function fetchAllResourceSetStats(resourceSetIds: number[]): Promise<ResourceSetStatsMap> {
  const statsMap: ResourceSetStatsMap = {}
  
  const promises = resourceSetIds.map(async (id) => {
    const stats = await fetchResourceSetStats(id)
    statsMap[id] = stats
  })
  
  await Promise.all(promises)
  return statsMap
}

/**
 * 加载总览页面所需的全部数据
 */
export async function loadOverviewData(): Promise<OverviewData> {
  const [resourceSetsResult, publishRecordsResult, auditLogsResult] = await Promise.all([
    fetchResourceSets(),
    fetchRecentPublishRecords(),
    fetchRecentAuditLogs(),
  ])

  return {
    resourceSets: resourceSetsResult.items,
    recentPublishRecords: publishRecordsResult.items,
    recentAuditLogs: auditLogsResult.items,
  }
}
