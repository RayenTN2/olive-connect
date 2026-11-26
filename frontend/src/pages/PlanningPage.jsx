import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { createTour, deleteTour, getDraftTours } from '../api/planning.js'
import PageLoading from '../components/PageLoading.jsx'
import PlanningMap from '../components/map/PlanningMap.jsx'
import MillPanel from '../components/planning/MillPanel.jsx'
import OrchardList from '../components/planning/OrchardList.jsx'
import OrchardPanel from '../components/planning/OrchardPanel.jsx'
import TourBuilder from '../components/planning/TourBuilder.jsx'
import { MATURITIES, STATES } from '../planning/labels.js'
import { buildRows, filterRows, listVarieties, NO_FILTERS, UNKNOWN } from '../planning/orchards.js'
import { checkTour } from '../planning/rules.js'
import { isStateChange, isUncertain, saveError, UNVERIFIED_SAVE } from '../planning/saveErrors.js'
import { matchesPayload, resourceWindow, bookedByMill, remainingText, startInstant, tourPayload, utcDay } from '../planning/tour.js'
import { usePlanning } from '../planning/PlanningContext.jsx'
import { forget, invalidate } from '../planning/cache.js'
import { loadMills, loadOrchards, loadRequests, loadTours, together } from '../planning/loaders.js'
import { useFree } from '../planning/useFree.js'
import { useLoad, useReady } from '../planning/useLoad.js'
import './PlanningPage.css'

// Deleting what is already gone is a success
const removeTour = (id) => deleteTour(id).catch((failure) => {
  if (failure.status !== 404) throw failure
})

export default function PlanningPage() {
  const orchardList = useLoad('orchards', loadOrchards)
  const requestList = useLoad('requests', loadRequests)
  const orchards = together(orchardList, requestList)
  // ?verger=... opens on that orchard (from a harvest request); ?etat= and ?maturite= on a filtered list (from the dashboard)
  const [params] = useSearchParams()
  const [filters, setFilters] = useState(() => {
    const [state, maturity] = [params.get('etat'), params.get('maturite')]
    return { ...NO_FILTERS, state: state in STATES ? state : '', maturity: maturity in MATURITIES || maturity === UNKNOWN ? maturity : '' }
  })
  const [askedId] = useState(() => params.get('verger'))
  const [selectedId, setSelectedId] = useState(askedId)
  const [selectedMillId, setSelectedMillId] = useState(null)
  const [returnToMillId, setReturnToMillId] = useState(null)
  const [hoveredId, setHoveredId] = useState(null)
  const [focusTarget, setFocusTarget] = useState(null)
  const [returnToId, setReturnToId] = useState(null)
  const { tour, dispatch, editing, request, setRequest, busyRef, unansweredRef, discard: discardTour } = usePlanning()
  const navigate = useNavigate()
  // A save that ends after leaving the page must not move the user
  const mountedRef = useRef(false)
  useEffect(() => {
    mountedRef.current = true
    return () => { mountedRef.current = false }
  }, [])

  const rows = useMemo(
    () => (orchardList.data && requestList.data ? buildRows(orchardList.data, requestList.data) : null),
    [orchardList.data, requestList.data],
  )
  const visible = useMemo(() => filterRows(rows ?? [], filters), [rows, filters])
  const varieties = useMemo(() => listVarieties(rows ?? []), [rows])
  const stops = useMemo(() => (tour?.stopIds ?? []).map((id) => rows?.find((row) => row.id === id)).filter(Boolean), [tour, rows])
  const mapStopIds = useMemo(() => stops.map(({ id }) => id), [stops])
  // Stops stay on the map even when the filters hide them from the list
  const onMap = useMemo(() => [...visible, ...(rows ?? []).filter((row) => mapStopIds.includes(row.id) && !visible.includes(row))], [visible, rows, mapStopIds])
  const tourMills = tour?.dropOffs.map(({ mill }) => mill) ?? []
  const selected = rows?.find((row) => row.id === selectedId)

  const start = tour ? startInstant(tour) : null
  const resources = useFree(start ? resourceWindow(start) : null)
  const millList = useLoad('mills', loadMills)
  const mills = { ...millList, data: useMemo(() => millList.data?.filter(({ active }) => active) ?? null, [millList.data]) }
  // Mills of the tour that aren't active any more stay on the map
  const mapMills = [...(mills.data ?? []), ...tourMills.filter(({ id }) => !mills.data?.some((mill) => mill.id === id))]
  const tourMillIds = tourMills.map(({ id }) => id)
  const selectedMill = mapMills.find(({ id }) => id === selectedMillId)
  const day = start ? utcDay(start) : null
  const tours = useLoad(day ? 'tours' : null, loadTours)
  const booked = { ...tours, data: useMemo(() => (day && tours.data ? bookedByMill(tours.data, day) : null), [day, tours.data]) }
  const ready = useReady(orchardList, requestList, millList)

  function show(id) {
    setHoveredId(null)
    clearSelection()
    setSelectedId(id)
    setFocusTarget(rows.find((row) => row.id === id))
  }

  function clearSelection() {
    setSelectedId(null)
    setSelectedMillId(null)
    setReturnToMillId(null)
  }

  function selectOnMap(id) {
    clearSelection()
    setSelectedId(id)
  }

  function selectMill(id) {
    clearSelection()
    setSelectedMillId(id)
  }

  function closeMill() {
    setReturnToMillId(selectedMillId)
    setSelectedMillId(null)
  }

  function close() {
    setReturnToId(selectedId)
    clearSelection()
    setFocusTarget(null)
  }

  // Any change makes the last refusal out of date
  function edit(action) {
    setRequest({})
    dispatch(action)
  }

  const addStop = (id) => edit({ type: 'addStop', id })
  const removeStop = (id) => edit({ type: 'removeStop', id })
  const addMill = (mill) => edit({ type: 'addMill', mill, kg: remainingText(tour, stops) })
  const removeMill = (mill) => edit({ type: 'removeMill', id: mill.id })
  const locked = Boolean(request.busy)

  function discard() {
    const draft = editing
    const first = tour.stopIds[0]
    discardTour()
    if (draft) navigate(`/tournees/${draft.id}`)
    else {
      setReturnToId(null)
      requestAnimationFrame(() => setReturnToId(first))
    }
  }

  // The new version exists: remove the one it replaces, then show it
  async function showSaved(draft, notice) {
    let leftoverId = null
    let shown = notice
    if (editing) {
      try {
        await removeTour(editing.id)
      } catch (failure) {
        if (isStateChange(failure.status, failure.message)) shown = 'L’ancienne version n’est plus un brouillon : elle a été conservée.'
        else leftoverId = editing.id
      }
    }
    discardTour()
    invalidate('tours')
    if (mountedRef.current) navigate(`/tournees/${draft.id}`, { state: { notice: shown, leftoverId } })
  }

  // Answers with the builder section to open after a refusal
  async function save() {
    if (busyRef.current) return null
    busyRef.current = true
    setRequest({ busy: true })
    const payload = tourPayload(tour, stops)
    try {
      if (unansweredRef.current) {
        const drafts = await getDraftTours().catch(() => null)
        if (!drafts) {
          setRequest({ error: { text: UNVERIFIED_SAVE } })
          return null
        }
        const existing = drafts.find((draft) => draft.id !== editing?.id && matchesPayload(draft, unansweredRef.current))
        unansweredRef.current = null
        if (existing) {
          await showSaved(existing, 'Il avait bien été enregistré. Vérifiez-le.')
          return null
        }
      }
      await showSaved(await createTour(payload), 'Enregistré. Il ne réserve aucune ressource.')
      return null
    } catch (failure) {
      if (isUncertain(failure.status)) unansweredRef.current = payload
      const error = saveError(failure.status, failure.message, tour)
      setRequest({ error })
      // A refusal may mean someone was just booked
      forget('free|')
      resources.retry()
      if (error.stale) invalidate('orchards', 'requests')
      if (isUncertain(failure.status)) invalidate('tours')
      return error.section
    } finally {
      busyRef.current = false
    }
  }

  let side
  if (orchards.error) {
    side = <div className="empty" role="alert"><p>{orchards.error}</p><button className="button-quiet" type="button" onClick={orchards.retry}>Réessayer</button></div>
  } else if (!rows) {
    side = <div className="loading" role="status"><span className="visually-hidden">Chargement des vergers</span>{[1, 2, 3, 4, 5].map((n) => <div className="skeleton" key={n} />)}</div>
  } else if (selectedMill) {
    side = (
      <MillPanel
        key={selectedMill.id} mill={selectedMill} day={day} load={booked} inTour={tourMillIds.includes(selectedMill.id)} locked={locked}
        onAdd={addMill} onRemove={removeMill} onClose={closeMill}
      />
    )
  } else if (selected) {
    side = <OrchardPanel key={selected.id} row={selected} inTour={stops.includes(selected)} locked={locked} onAdd={addStop} onRemove={removeStop} onClose={close} />
  } else {
    side = (
      <OrchardList
        rows={visible} total={rows.length} varieties={varieties} filters={filters} onFilters={setFilters}
        stopIds={stops.map(({ id }) => id)} locked={locked} onAdd={addStop}
        hoveredId={hoveredId} onHover={setHoveredId} onSelect={show} returnToId={returnToId}
      />
    )
  }

  if (!ready) return <main className="page"><PageLoading /></main>
  return (
    <main className={tour ? 'planning with-builder' : 'planning'}>
      <div className="side float-left">{side}</div>
      <PlanningMap
        rows={onMap} stopIds={mapStopIds} mills={mapMills} tourMillIds={tourMillIds} millsError={mills.error} onRetryMills={mills.retry}
        selectedId={selectedId} hoveredId={hoveredId} focusTarget={focusTarget ?? (askedId && askedId === selectedId ? selected : null)}
        selectedMillId={selectedMillId} returnToMillId={returnToMillId} onSelectMill={selectMill} onSelect={selectOnMap} onHover={setHoveredId}
      />
      {tour && (
        <TourBuilder
          key={editing?.id ?? 'new'}
          tour={tour} stops={stops} dispatch={edit} rules={checkTour({ tour, stops, availability: resources.data, booked: booked.data })}
          mills={mills} booked={booked.data} resources={resources} onShow={show} onHoverMill={setHoveredId}
          onSave={save} saving={Boolean(request.busy)} error={request.error} editing={editing !== null} onDiscard={discard}
        />
      )}
    </main>
  )
}
