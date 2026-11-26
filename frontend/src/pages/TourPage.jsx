import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { cancelTour, changeTeamLeader, confirmTour, deleteTour, getActiveOilMills, getTour } from '../api/planning.js'
import { loadErrorMessage } from '../auth/messages.js'
import PlanningMap from '../components/map/PlanningMap.jsx'
import TourReview from '../components/planning/TourReview.jsx'
import { usePlanning } from '../planning/PlanningContext.jsx'
import { ACTION_NOT_DONE, ACTION_UNVERIFIED, actionError, isStateChange, isUncertain, TOUR_GONE } from '../planning/saveErrors.js'
import { toBuilder } from '../planning/tour.js'
import { forget, invalidate, revalidate } from '../planning/cache.js'
import { rememberItem, useLoad } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: facts, notices, loading
import './ToursPage.css'
import './TourPage.css'

const orNull = (failure) => {
  if (failure.status === 404) return null
  throw failure
}
// { tour: null } = it doesn't exist
const loadTour = rememberItem('tour', async (id) => ({ tour: await getTour(id).catch(orNull) }))
// Deleting what is already gone is a success
const removeTour = (id) => deleteTour(id).catch(orNull)

function Tour({ id }) {
  const loaded = useLoad(id, loadTour)
  const location = useLocation()
  const navigate = useNavigate()
  const planning = usePlanning()
  const [arrival] = useState(() => location.state ?? {}) // { notice, leftoverId } from the builder
  const [current, setCurrent] = useState(null) // { tour, notice }: newer than what was loaded
  const [leftoverId, setLeftoverId] = useState(arrival.leftoverId ?? null)
  const [request, setRequest] = useState({}) // { busy, error } of the running action
  const [selectedId, setSelectedId] = useState(null) // the step chosen on the map
  const busyRef = useRef(false) // blocks a second click before React has redrawn
  const mountedRef = useRef(false)
  useEffect(() => {
    mountedRef.current = true
    return () => { mountedRef.current = false }
  }, [])

  // A reload must not repeat "Enregistré"
  useEffect(() => {
    if (location.state) navigate(location.pathname, { replace: true, state: null })
  }, [location, navigate])

  const tour = current ? current.tour : loaded.data?.tour
  const notice = current ? current.notice : arrival.notice ?? ''

  const places = useMemo(() => {
    if (!tour) return null
    const located = ({ latitude, longitude }) => latitude != null && longitude != null
    const line = tour.routeGeometry ?? []
    return {
      rows: tour.stops.map((stop) => ({ id: stop.orchardId, name: stop.orchardName, position: located(stop) ? [stop.latitude, stop.longitude] : null, harvest: null })),
      mills: tour.dropOffs.map((item) => ({ id: item.oilMillId, name: item.oilMillName, latitude: item.latitude, longitude: item.longitude })),
      route: { line, real: tour.routeEstimated === false, points: [...line, ...[...tour.stops, ...tour.dropOffs].filter(located).map(({ latitude, longitude }) => [latitude, longitude])] },
    }
  }, [tour])

  async function run(work) {
    if (busyRef.current) return
    busyRef.current = true
    setRequest({ busy: true })
    try {
      setRequest({ error: await work() })
    } finally {
      busyRef.current = false
    }
  }

  // Any action may change the tour, its requests and its orchards
  function changed() {
    invalidate('tours', 'orchards', 'requests')
    forget('free|')
    revalidate(`tour|${id}`)
  }

  // Runs an action on the tour. call answers with the updated tour, or null once deleted.
  // isDone tells from the tour read again (null = gone) whether an unanswered call went through.
  const act = (call, isDone, done) => run(async () => {
    const finish = (saved) => {
      changed()
      if (saved) setCurrent({ tour: saved, notice: done })
      else if (mountedRef.current) navigate('/tournees', { replace: true, state: { notice: 'Le brouillon a été supprimé.' } })
      return ''
    }
    try {
      return finish(await call())
    } catch (failure) {
      const uncertain = isUncertain(failure.status)
      const text = actionError(failure.status, failure.message, tour)
      if (!uncertain && !isStateChange(failure.status, failure.message)) return text
      changed()
      // null = gone, undefined = could not be read
      const read = await getTour(id).catch((error) => (error.status === 404 ? null : undefined))
      if (uncertain && read !== undefined && isDone(read)) return finish(read)
      if (read === null) return TOUR_GONE
      if (read) setCurrent({ tour: read, notice: '' })
      if (uncertain) return read === undefined ? ACTION_UNVERIFIED : ACTION_NOT_DONE
      return text
    }
  })

  const refresh = () => run(async () => {
    try {
      const read = await getTour(id)
      setCurrent({ tour: read, notice: '' })
      return ''
    } catch (failure) {
      return failure.status === 404 ? TOUR_GONE : loadErrorMessage(failure.status)
    }
  })

  const beingEdited = planning.editing?.id === id
  const editDraft = () => run(async () => {
    // The builder already holds this draft's changes: take them up again
    if (beingEdited) {
      navigate('/planification')
      return ''
    }
    if (planning.busyRef.current) return 'Un enregistrement est en cours. Réessayez dans un instant.'
    try {
      const builder = toBuilder(tour, await getActiveOilMills())
      if (!mountedRef.current) return ''
      planning.openDraft(tour, builder)
      navigate('/planification')
      return ''
    } catch (failure) {
      return loadErrorMessage(failure.status)
    }
  })

  const removeLeftover = () => run(async () => {
    try {
      await removeTour(leftoverId)
      setLeftoverId(null)
      invalidate('tours')
      return ''
    } catch (failure) {
      return actionError(failure.status, failure.message, tour)
    }
  })

  const actions = {
    replaces: planning.tour !== null && !beingEdited,
    onConfirm: () => act(() => confirmTour(id), (read) => read?.status === 'CONFIRMED', 'Les ressources sont réservées.'),
    onCancelTour: (reason) => act(() => cancelTour(id, reason), (read) => read?.status === 'CANCELLED', 'Les ressources sont libérées.'),
    onLeader: (leaderId) => act(() => changeTeamLeader(id, leaderId), (read) => read?.teamLeaderId === leaderId, 'Le chef d’équipe a été changé.'),
    onDelete: () => act(() => removeTour(id), (read) => read === null, ''),
    onEdit: editDraft,
  }

  const back = <Link className="button-quiet back" to="/tournees">Toutes les tournées</Link>
  if (!tour) {
    return (
      <main className="page">
        {back}
        {loaded.error && <div className="notice" role="alert"><p>{loaded.error}</p><button className="button-quiet" type="button" onClick={loaded.retry}>Réessayer</button></div>}
        {!loaded.error && !loaded.data && <div className="loading" role="status"><span className="visually-hidden">Chargement de la tournée</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>}
        {loaded.data && <h1 className="tour-gone">{TOUR_GONE}</h1>}
      </main>
    )
  }
  const millIds = places.mills.map((mill) => mill.id)
  return (
    <main className="tour-page">
      <TourReview
        key={`${tour.status} ${tour.teamLeaderId} ${leftoverId}`}
        saved={tour} notice={notice} busy={Boolean(request.busy)} error={request.error} back={back} onRefresh={refresh}
        leftover={leftoverId !== null} onRemoveLeftover={removeLeftover} actions={actions} selectedId={selectedId}
      />
      <PlanningMap
        single rows={places.rows} stopIds={places.rows.map((row) => row.id)} mills={places.mills} tourMillIds={millIds} route={places.route}
        selectedId={selectedId} selectedMillId={selectedId} onSelect={setSelectedId} onSelectMill={setSelectedId} onHover={() => {}}
      />
    </main>
  )
}

export default function TourPage() {
  const { id } = useParams()
  return <Tour key={id} id={id} />
}
