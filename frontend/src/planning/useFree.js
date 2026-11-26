import { useEffect, useState } from 'react'
import { loadFree } from './loaders.js'
import { useLoad } from './useLoad.js'

const PAUSE_MS = 400

// Who is free during a period ({ start, end } or null).
//   data    checked just now for this period: the only one to decide with
//   known   what was last known for this period, possibly being checked again
//   shown   known, else the previous period's answer, to show greyed while waiting
export function useFree(span) {
  const key = span ? `${span.start}|${span.end}` : null
  const [settled, setSettled] = useState(key)
  // The first period is asked at once; later ones wait for a pause, so choosing a time doesn't ask at each step
  if (settled === null && key !== null) setSettled(key)
  useEffect(() => {
    if (key === settled) return undefined
    const timer = setTimeout(() => setSettled(key), PAUSE_MS)
    return () => clearTimeout(timer)
  }, [key, settled])

  const current = key !== null && key === settled
  const load = useLoad(current ? key : null, loadFree)
  const [last, setLast] = useState(null)
  if (load.data !== null && load.data !== last) setLast(load.data)

  const known = current ? load.data : null
  const error = current ? load.error || (load.checked ? load.failure : '') : ''
  return {
    wanted: key !== null, data: current && load.checked && !error ? load.data : null, known, shown: known ?? last,
    checking: key !== null && !error && !(current && load.checked), error, retry: load.retry,
  }
}
