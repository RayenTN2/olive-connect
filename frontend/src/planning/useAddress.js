import { useState } from 'react'
import { useSearchParams } from 'react-router'

// Filters kept in the address. q is the search text.
export function useAddress() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''

  // The address updates a little after a keystroke: the field keeps its own text, and follows Back and Forward
  const [typed, setTyped] = useState({ text: q, address: q })
  if (typed.address !== q) setTyped({ text: typed.sent === q ? typed.text : q, address: q, sent: typed.sent })

  // Typing replaces the history entry; choosing a filter adds one, so Back undoes it
  function change(changes, replace = false) {
    const next = new URLSearchParams(window.location.search) // not params: it lags behind quick changes
    for (const [key, value] of Object.entries(changes)) {
      if (value) next.set(key, value)
      else next.delete(key)
    }
    setParams(next, { replace })
  }

  function type(text) {
    setTyped({ text, address: q, sent: text })
    change({ q: text }, true)
  }

  function clear(remaining = {}) {
    setTyped({ text: '', address: q, sent: '' })
    setParams(remaining)
  }

  return { params, text: typed.text, type, change, clear }
}

// A click on a column: its natural direction first, then the other one
export function nextSort({ tri, sens }, key, descendingFirst) {
  const first = descendingFirst.includes(key) ? 'desc' : 'asc'
  const other = first === 'asc' ? 'desc' : 'asc'
  return { tri: key, sens: tri === key && sens === first ? other : first }
}
