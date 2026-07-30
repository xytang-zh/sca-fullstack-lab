import { reactive, ref } from 'vue'
import type { PageVO, PageQuery } from '@sca/types'

export function useTable<T, Q extends PageQuery>(fetcher: (q: Q) => Promise<PageVO<T>>) {
  const loading = ref(false)
  const list = ref<T[]>([]) as ReturnType<typeof ref<T[]>>
  const total = ref(0)
  const query = reactive<Q>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    ...({} as Partial<Q>)
  } as Q)

  async function fetch() {
    loading.value = true
    try {
      const data = await fetcher({ ...query } as Q)
      list.value = data.list
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  function handlePageChange(page: number) {
    query.pageNum = page
    fetch()
  }
  function handlePageSizeChange(size: number) {
    query.pageSize = size
    query.pageNum = 1
    fetch()
  }
  function handleSearch() {
    query.pageNum = 1
    fetch()
  }
  function resetQuery() {
    query.keyword = ''
    query.pageNum = 1
    fetch()
  }

  return {
    loading,
    list,
    total,
    query,
    fetch,
    handlePageChange,
    handlePageSizeChange,
    handleSearch,
    resetQuery
  }
}
