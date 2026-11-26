import { useEffect, useState, useSyncExternalStore } from 'react'
import { loadErrorMessage } from '../auth/messages.js'
import { peek, refresh, subscribe } from './cache.js'

// A list that is remembered while signed in
export const rememberList = (name, load) => Object.assign(load, { cacheId: () => name })
// One item per key
export const rememberItem = (name, load) => Object.assign(load, { cacheId: (key) => `${name}|${key}` })

// Loads data for a key; null key = nothing to load. An answer for an older key is never shown.
// `load` must be a module-level function taking the key. A remembered one shows what is known at once, then refreshes it.
export function useLoad(key, load) {
  const [result, setResult] = useState({})
  const [attempt, setAttempt] = useState(0)
  const [checkedFor, setCheckedFor] = useState('')
  const id = key !== null && load.cacheId ? load.cacheId(key) : null
  const entry = useSyncExternalStore(subscribe, () => (id ? peek(id) : undefined))

  useEffect(() => {
    if (key === null) return undefined
    if (id) {
      let left = false
      refresh(id, () => load(key)).then(() => !left && setCheckedFor(`${id} ${attempt}`))
      return () => { left = true }
    }
    let stale = false
    load(key)
      .then((data) => !stale && setResult({ key, attempt, data }))
      .catch((failure) => !stale && setResult({ key, attempt, error: loadErrorMessage(failure.status) }))
    return () => { stale = true }
  }, [id, key, attempt, load])

  const retry = () => setAttempt(attempt + 1)
  if (id) {
    const data = entry?.data ?? null
    const failure = entry?.error ? loadErrorMessage(entry.error.status) : ''
    // checked: the server has answered since this was asked; failure: that answer was an error, even if old data remains
    return { wanted: true, data, error: data === null ? failure : '', failure, checked: checkedFor === `${id} ${attempt}`, retry }
  }
  const current = key !== null && result.key === key && result.attempt === attempt ? result : {}
  return { wanted: key !== null, data: current.data ?? null, error: current.error ?? '', retry }
}

// True once every load has an answer, or after 3 s, so a page appears complete but a slow source never holds it long.
// It then stays true.
const LONGEST_WAIT_MS = 3000
export function useReady(...loads) {
  const [late, setLate] = useState(false)
  const [shown, setShown] = useState(false)
  const complete = loads.every(({ data, error }) => data !== null || error)
  if (complete && !shown) setShown(true)
  useEffect(() => {
    const timer = setTimeout(() => setLate(true), LONGEST_WAIT_MS)
    return () => clearTimeout(timer)
  }, [])
  return complete || shown || late
}
