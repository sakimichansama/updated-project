export const DEFAULT_PAGE_SIZE = 10
export const PAGE_SIZES = [10, 20, 50, 100]

export const filterRows = (rows, keyword, extraText = () => '') => {
  const normalizedKeyword = String(keyword || '').trim().toLowerCase()
  if (!normalizedKeyword) return rows || []
  return (rows || []).filter(row => {
    const text = `${collectText(row)} ${extraText(row)}`.toLowerCase()
    return text.includes(normalizedKeyword)
  })
}

export const pageRows = (rows, currentPage, pageSize) => {
  const page = Math.max(1, Number(currentPage || 1))
  const size = Math.max(1, Number(pageSize || DEFAULT_PAGE_SIZE))
  const start = (page - 1) * size
  return (rows || []).slice(start, start + size)
}

const collectText = (value) => {
  if (value == null) return ''
  if (Array.isArray(value)) return value.map(collectText).join(' ')
  if (typeof value === 'object') return Object.values(value).map(collectText).join(' ')
  return String(value)
}
