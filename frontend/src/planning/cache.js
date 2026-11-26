// What the signed-in user has already loaded, kept in memory only. Each entry: { data, error }.
const entries = new Map()
const running = new Map() // id → promise, so the same list asked twice at once is sent once
const loads = new Map() // id → how to load it, known even before the first answer
const listeners = new Set()
let generation = 0 // answers asked for a previous account, or before an invalidation, are dropped

const notify = () => listeners.forEach((listener) => listener())
const versions = new Map()
const versionOf = (id) => `${generation} ${versions.get(id) ?? 0}`

export function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export const peek = (id) => entries.get(id)

// Loads again and keeps the old data meanwhile. An identical answer keeps the same object, so nothing is redrawn
export function refresh(id, load) {
  loads.set(id, load)
  if (running.has(id)) return running.get(id)
  const version = versionOf(id)
  const promise = Promise.resolve()
    .then(load)
    .then((data) => ({ data }), (error) => ({ error }))
    .then((answer) => {
      if (running.get(id) === promise) running.delete(id)
      if (versionOf(id) !== version) return
      const before = entries.get(id)
      if (answer.error) entries.set(id, { data: before?.data, error: answer.error })
      else {
        const same = before?.data !== undefined && JSON.stringify(before.data) === JSON.stringify(answer.data)
        if (same && !before.error) return
        entries.set(id, { data: same ? before.data : answer.data })
      }
      notify()
    })
  running.set(id, promise)
  return promise
}

// After a change: forget these lists, so an outdated one is never shown, and load them again in the background
export function invalidate(...ids) {
  for (const id of ids) {
    versions.set(id, (versions.get(id) ?? 0) + 1)
    running.delete(id)
    const load = loads.get(id)
    entries.delete(id)
    if (load) refresh(id, load)
  }
  notify()
}

// After a tour is confirmed, cancelled or saved: what was known about every period is out of date
// keep: an entry on screen, which stays
export function forget(prefix, keep) {
  for (const id of new Set([...entries.keys(), ...running.keys()])) {
    if (!id.startsWith(prefix) || id === keep) continue
    versions.set(id, (versions.get(id) ?? 0) + 1)
    running.delete(id)
    entries.delete(id)
  }
  notify()
}

// After a change to one item: keep showing it, and read it again
export function revalidate(id) {
  versions.set(id, (versions.get(id) ?? 0) + 1)
  running.delete(id)
  const load = loads.get(id)
  if (load) refresh(id, load)
}

// Sign-out, expired session or another account: nothing may remain
export function clearCache() {
  generation += 1
  entries.clear()
  running.clear()
  loads.clear()
  notify()
}

// Entries still shown although their last refresh failed (free resources show their own error instead)
export const staleIds = () => [...entries].filter(([id, { data, error }]) => data !== undefined && error && !id.startsWith('free|')).map(([id]) => id)
export const retryStale = () => staleIds().forEach((id) => refresh(id, loads.get(id)))
