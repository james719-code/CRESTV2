import { test } from 'node:test'
import assert from 'node:assert/strict'
import { loadReadingProgress, saveReadingProgress } from '../src/data/reading-progress.ts'

function memoryStorage() {
  const values = new Map()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    values,
  }
}

test('last viewed page survives closing/reopening and stays separate by account, backend, and PDF', () => {
  const storage = memoryStorage()
  assert.deepEqual(loadReadingProgress(storage, 'school/teacher', 'paper-1', 73), {
    page: 1,
    available: true,
  })
  assert.equal(saveReadingProgress(storage, 'school/teacher', 'paper-1', 12, 73), true)
  assert.equal(loadReadingProgress(storage, 'school/teacher', 'paper-1', 73).page, 12)
  assert.equal(loadReadingProgress(storage, 'school/other-teacher', 'paper-1', 73).page, 1)
  assert.equal(loadReadingProgress(storage, 'another-backend/teacher', 'paper-1', 73).page, 1)
  assert.equal(loadReadingProgress(storage, 'school/teacher', 'new-file-version', 73).page, 1)
  saveReadingProgress(storage, 'school/teacher', 'paper-1', 7, 73)
  assert.equal(loadReadingProgress(storage, 'school/teacher', 'paper-1', 73).page, 7)
})

test('restoration clamps stale pages and invalid page changes cannot overwrite a valid bookmark', () => {
  const storage = memoryStorage()
  saveReadingProgress(storage, 'scope', 'file', 12, 73)
  assert.equal(loadReadingProgress(storage, 'scope', 'file', 5).page, 5)
  for (const page of [0, -1, NaN, Infinity, 1.5, 74])
    assert.equal(saveReadingProgress(storage, 'scope', 'file', page, 73), false)
  assert.equal(loadReadingProgress(storage, 'scope', 'file', 73).page, 12)
})

test('corrupt data and blocked/quota-exhausted storage never prevent reading', () => {
  const storage = memoryStorage()
  saveReadingProgress(storage, 'scope', 'file', 2, 4)
  const key = [...storage.values.keys()][0]
  for (const corrupt of [
    'broken JSON',
    '{}',
    '[null,{}, {"fileId":"file","page":-8,"updatedAt":1}]',
  ]) {
    storage.setItem(key, corrupt)
    assert.equal(loadReadingProgress(storage, 'scope', 'file', 4).page, 1)
    assert.equal(saveReadingProgress(storage, 'scope', 'file', 3, 4), true)
  }
  const denied = {
    getItem() {
      throw new Error('blocked')
    },
    setItem() {
      throw new Error('quota')
    },
  }
  assert.deepEqual(loadReadingProgress(denied, 'scope', 'file', 4), { page: 1, available: false })
  assert.equal(saveReadingProgress(denied, 'scope', 'file', 2, 4), false)
  assert.deepEqual(loadReadingProgress(undefined, 'scope', 'file', 4), {
    page: 1,
    available: false,
  })
  assert.equal(
    saveReadingProgress(
      {
        ...storage,
        setItem() {
          throw new Error('quota')
        },
      },
      'scope',
      'file',
      2,
      4,
    ),
    false,
  )
})

test('only the 200 most recently read files are kept without storing paper names, URLs, or PDF bytes', () => {
  const storage = memoryStorage()
  for (let index = 0; index < 205; index++)
    saveReadingProgress(storage, 'scope', `file-${index}`, 2, 4, index)
  const saved = JSON.parse([...storage.values.values()][0])
  assert.equal(saved.length, 200)
  assert.deepEqual(Object.keys(saved[0]).sort(), ['fileId', 'page', 'updatedAt'])
  assert.equal(loadReadingProgress(storage, 'scope', 'file-0', 4).page, 1)
  assert.equal(loadReadingProgress(storage, 'scope', 'file-204', 4).page, 2)
})
