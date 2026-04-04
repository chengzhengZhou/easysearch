import { ref, computed, type Ref, type ComputedRef } from 'vue'

export interface SelectionReturn<T extends { id: number }> {
  /** 已选中的 ID 集合 */
  selectedIds: Ref<Set<number>>
  /** 已选中的数量 */
  selectedCount: ComputedRef<number>
  /** 是否全选 */
  isAllSelected: (rows: T[]) => boolean
  /** 切换全选 */
  toggleAll: (rows: T[], checked: boolean) => void
  /** 切换单个选中 */
  toggleOne: (id: number, checked: boolean) => void
  /** 是否选中指定 ID */
  isSelected: (id: number) => boolean
  /** 清空选中 */
  clearSelection: () => void
  /** 获取已选中的 ID 数组 */
  getSelectedIds: () => number[]
}

/**
 * 选择功能组合式函数
 * 提供通用的多选逻辑
 */
export function useSelection<T extends { id: number }>(): SelectionReturn<T> {
  const selectedIds = ref<Set<number>>(new Set())

  const selectedCount = computed(() => selectedIds.value.size)

  function isAllSelected(rows: T[]): boolean {
    if (rows.length === 0) return false
    return rows.every((r) => selectedIds.value.has(r.id))
  }

  function toggleAll(rows: T[], checked: boolean) {
    const next = new Set<number>()
    if (checked) {
      for (const r of rows) {
        next.add(r.id)
      }
    }
    selectedIds.value = next
  }

  function toggleOne(id: number, checked: boolean) {
    const next = new Set(selectedIds.value)
    if (checked) {
      next.add(id)
    } else {
      next.delete(id)
    }
    selectedIds.value = next
  }

  function isSelected(id: number): boolean {
    return selectedIds.value.has(id)
  }

  function clearSelection() {
    selectedIds.value = new Set()
  }

  function getSelectedIds(): number[] {
    return Array.from(selectedIds.value)
  }

  return {
    selectedIds,
    selectedCount,
    isAllSelected,
    toggleAll,
    toggleOne,
    isSelected,
    clearSelection,
    getSelectedIds,
  }
}
