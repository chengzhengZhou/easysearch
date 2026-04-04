import { ref, computed, watch, type Ref, type ComputedRef } from 'vue'

export interface PaginationOptions<T> {
  /** 原始数据源 */
  data: Ref<T[]> | ComputedRef<T[]>
  /** 每页大小，默认 20 */
  defaultPageSize?: number
  /** 页大小选项 */
  pageSizeOptions?: number[]
}

export interface PaginationReturn<T> {
  /** 当前页码 */
  page: Ref<number>
  /** 每页大小 */
  pageSize: Ref<number>
  /** 总条数 */
  total: ComputedRef<number>
  /** 总页数 */
  totalPages: ComputedRef<number>
  /** 当前页数据 */
  pagedData: ComputedRef<T[]>
  /** 页大小选项 */
  pageSizeOptions: number[]
  /** 上一页 */
  prevPage: () => void
  /** 下一页 */
  nextPage: () => void
  /** 跳转到指定页 */
  goToPage: (p: number) => void
  /** 重置页码 */
  resetPage: () => void
  /** 是否有上一页 */
  hasPrev: ComputedRef<boolean>
  /** 是否有下一页 */
  hasNext: ComputedRef<boolean>
}

/**
 * 分页组合式函数
 * 提供通用的前端分页逻辑
 */
export function usePagination<T>(options: PaginationOptions<T>): PaginationReturn<T> {
  const { data, defaultPageSize = 20, pageSizeOptions = [10, 20, 50] } = options

  const page = ref(1)
  const pageSize = ref(defaultPageSize)

  const total = computed(() => data.value.length)

  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

  const pagedData = computed(() => {
    const size = Math.max(1, pageSize.value)
    const currentPage = Math.min(Math.max(1, page.value), totalPages.value)
    const start = (currentPage - 1) * size
    return data.value.slice(start, start + size)
  })

  const hasPrev = computed(() => page.value > 1)
  const hasNext = computed(() => page.value < totalPages.value)

  // 当 pageSize 变化时重置页码
  watch(pageSize, () => {
    page.value = 1
  })

  // 当数据源变化时，确保页码在有效范围内
  watch(
    () => data.value.length,
    () => {
      if (page.value > totalPages.value) {
        page.value = Math.max(1, totalPages.value)
      }
    }
  )

  function prevPage() {
    if (hasPrev.value) {
      page.value--
    }
  }

  function nextPage() {
    if (hasNext.value) {
      page.value++
    }
  }

  function goToPage(p: number) {
    page.value = Math.min(Math.max(1, p), totalPages.value)
  }

  function resetPage() {
    page.value = 1
  }

  return {
    page,
    pageSize,
    total,
    totalPages,
    pagedData,
    pageSizeOptions,
    prevPage,
    nextPage,
    goToPage,
    resetPage,
    hasPrev,
    hasNext,
  }
}
