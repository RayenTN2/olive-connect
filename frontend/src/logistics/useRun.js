import { useRef, useState } from 'react'
import { forget, revalidate } from '../planning/cache.js'
import { logisticsError } from './save.js'

// One action at a time. work answers when it is done, or throws
export function useRun() {
  const [state, setState] = useState({}) // { busy, error }
  const busyRef = useRef(false) // blocks a second click before React has redrawn
  async function run(work) {
    if (busyRef.current) return
    busyRef.current = true
    setState({ busy: true })
    let error = ''
    try {
      await work()
    } catch (failure) {
      error = logisticsError(failure)
    } finally {
      busyRef.current = false
      setState({ error })
    }
  }
  return { busy: Boolean(state.busy), error: state.error ?? '', run, clearError: () => setState({ busy: busyRef.current }) }
}

// After a change: what was known about every period is out of date. keep: the calendar on screen, refreshed in place
export function resourcesChanged(kind, id, keep) {
  for (const entry of [kind, `equipment-item|${id}`, `periods|${kind}|${id}`]) revalidate(entry)
  forget('calendar|', keep && `calendar|${keep}`)
  if (keep) revalidate(`calendar|${keep}`)
  forget('free|')
}
