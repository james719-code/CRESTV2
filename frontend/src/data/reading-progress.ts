export interface ProgressStorage {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
}

interface ReadingProgress {
  fileId: string
  page: number
  updatedAt: number
}
const limit = 200
const key = (scope: string) => `crest-reading-progress-v1:${encodeURIComponent(scope)}`

function entries(storage: ProgressStorage, scope: string): ReadingProgress[] {
  const value = storage.getItem(key(scope))
  if (!value) return []
  let parsed: unknown
  try {
    parsed = JSON.parse(value)
  } catch {
    return []
  }
  if (!Array.isArray(parsed)) return []
  return parsed.filter(
    (entry): entry is ReadingProgress =>
      entry &&
      typeof entry.fileId === 'string' &&
      Number.isSafeInteger(entry.page) &&
      entry.page >= 1 &&
      Number.isFinite(entry.updatedAt) &&
      entry.updatedAt >= 0,
  )
}

export function loadReadingProgress(
  storage: ProgressStorage | undefined,
  scope: string,
  fileId: string,
  pageCount: number,
) {
  if (!storage) return { page: 1, available: false }
  try {
    const saved = entries(storage, scope).find((entry) => entry.fileId === fileId)
    const maximum = Number.isSafeInteger(pageCount) && pageCount > 0 ? pageCount : 1
    return { page: Math.min(saved?.page || 1, maximum), available: true }
  } catch {
    return { page: 1, available: false }
  }
}

export function saveReadingProgress(
  storage: ProgressStorage | undefined,
  scope: string,
  fileId: string,
  page: number,
  pageCount: number,
  now = Date.now(),
): boolean {
  if (
    !storage ||
    !scope ||
    !fileId ||
    !Number.isSafeInteger(page) ||
    page < 1 ||
    !Number.isSafeInteger(pageCount) ||
    pageCount < 1 ||
    page > pageCount
  )
    return false
  try {
    const previous = entries(storage, scope)
      .filter((entry) => entry.fileId !== fileId)
      .sort((a, b) => b.updatedAt - a.updatedAt)
      .slice(0, limit - 1)
    storage.setItem(key(scope), JSON.stringify([{ fileId, page, updatedAt: now }, ...previous]))
    return true
  } catch {
    return false
  }
}
