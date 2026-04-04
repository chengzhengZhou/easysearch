/**
 * 公共类型定义
 * 供各管理页面复用
 */

// ==================== 分页相关 ====================
export type PageResult<T> = {
  page: number
  pageSize: number
  total: number
  items: T[]
}

export type PaginationState = {
  page: number
  pageSize: number
  total: number
  totalPages: number
}

// ==================== 资源集相关 ====================
export type ResourceSet = {
  id: number
  name: string
  moduleType: string
  env: string
  scene: string
  currentSnapshotId?: number | null
}

export type Snapshot = {
  id: number
  snapshotNo: number
  changeLog?: string | null
  ruleCount: number
  publishedBy: string
  publishedAt: string
}

// ==================== 差异对比相关 ====================
export type DiffSummary = {
  hasChanges: boolean
  addedCount: number
  deletedCount: number
  modifiedCount: number
  currentRuleCount: number
  noSnapshot: boolean
}

export type DiffResult<T = any> = {
  added: T[]
  deleted: T[]
  modified: Array<{
    key: string | number
    before: T
    after: T
  }>
}

// ==================== 干预规则相关 ====================
export type InterventionMode = 'sentence' | 'term'

export type SentenceRule = {
  id: number
  sourceText: string
  targetText: string
  matchType: 'EXACT' | 'PREFIX' | 'CONTAINS'
  priority: number
  enabled: number
  remark?: string | null
}

export type TermRule = {
  id: number
  sourceText: string
  targetText: string
  priority: number
  enabled: number
  remark?: string | null
}

export type InterventionRule = SentenceRule | TermRule

// ==================== 同义词规则相关 ====================
export type SynonymDirection = '=>' | '<=' | 'SYM'

export type SynonymRule = {
  id: number
  sourceText: string
  direction: SynonymDirection
  targetsJson: string
  enabled: number
  remark?: string | null
}

// ==================== 实体规则相关 ====================
export type EntityType =
  | 'CATEGORY'
  | 'BRAND'
  | 'MODEL'
  | 'CPU'
  | 'RAM'
  | 'STORAGE'
  | 'PRICE'
  | 'CONDITION'
  | 'COLOR'
  | 'SIZE'
  | 'WEIGHT'
  | 'BATTERY'
  | 'CAMERA'
  | 'SCREEN'
  | 'OS'
  | 'NETWORK'
  | 'INTERFACE'
  | 'FEATURE'
  | 'ACCESSORY'
  | 'WARRANTY'
  | 'TAG'
  | 'UNKNOWN'

export type EntityRule = {
  id: number
  entityText: string
  entityType: EntityType | string
  normalizedValue: string
  aliasesJson: string | null
  attributesJson: string | null
  relationsJson: string | null
  idsJson: string | null
  enabled: number
  remark?: string | null
}

// ==================== 分词词典规则相关 ====================
export type TokenRule = {
  id: number
  word: string
  nature: string
  frequency: number | null
  bizId: string | null
  enabled: number
}

// ==================== 弹窗相关 ====================
export type ModalType = 'info' | 'warn' | 'primary' | 'danger'

export type ModalProps = {
  open: boolean
  title: string
  icon?: string
  type?: ModalType
  size?: 'sm' | 'md' | 'lg'
}

// ==================== 通用基础规则类型 ====================
export interface BaseRule {
  id: number
  enabled: number
  priority?: number
  remark?: string | null
}

// ==================== 日志相关 ====================
export type AuditLogItem = {
  id: number
  createdAt: string
  action: string
  userName: string
  entityType?: string
  entityId?: number
}

export type PublishRecord = {
  id: number
  resourceSetId: number
  snapshotId: number
  env?: string
  publishStatus: string
  publishMsg?: string
  startedAt: string
  finishedAt?: string
  operator: string
}

// ==================== 总览页面相关 ====================
export type OverviewStats = {
  totalResourceSets: number
  totalRules: number
  publishedSnapshots: number
  pendingChanges: number
}

export type ModuleRuleCount = {
  module: string
  count: number
}

export type ResourceSetWithStats = ResourceSet & {
  ruleCount?: number
  lastPublishedAt?: string
  lastPublishedBy?: string
  snapshotNo?: number
  hasPendingChanges?: boolean
}
