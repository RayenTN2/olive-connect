import { createContext, useContext, useEffect, useMemo, useReducer, useRef, useState } from 'react'
import { tourReducer } from './tour.js'

const PlanningContext = createContext(null)

// The tour being prepared lives above the pages, so changing page doesn't lose it
export function PlanningProvider({ children }) {
  const [tour, dispatch] = useReducer(tourReducer, null)
  const [editing, setEditing] = useState(null) // the saved draft the builder will replace
  const [request, setRequest] = useState({}) // { busy, error } of the save, which may outlive its page
  const busyRef = useRef(false) // blocks a second click before React has redrawn
  const unansweredRef = useRef(null) // the payload whose answer was lost

  // The tour exists only in the browser: ask before a reload or a closed tab loses it
  const unsaved = tour !== null
  useEffect(() => {
    if (!unsaved) return undefined
    const warn = (event) => event.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [unsaved])

  const value = useMemo(() => {
    // Opens a saved draft in the builder; replaces any tour being prepared
    function openDraft(saved, builder) {
      unansweredRef.current = null
      setRequest({})
      setEditing(saved)
      dispatch({ type: 'load', tour: builder })
    }
    function discard() {
      unansweredRef.current = null
      setRequest({})
      setEditing(null)
      dispatch({ type: 'discard' })
    }
    return { tour, dispatch, editing, request, setRequest, busyRef, unansweredRef, openDraft, discard }
  }, [tour, editing, request])
  return <PlanningContext.Provider value={value}>{children}</PlanningContext.Provider>
}

// eslint-disable-next-line react/only-export-components
export function usePlanning() {
  return useContext(PlanningContext)
}
