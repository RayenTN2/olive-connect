import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { getOilMill } from '../api/planning.js'
import PlanningMap from '../components/map/PlanningMap.jsx'
import { formatInstant, formatKg, formatTime, TOUR_STATUSES } from '../planning/labels.js'
import { toursOfMill } from '../planning/mills.js'
import { bookedByMill, millLeftKg } from '../planning/tour.js'
import { tunisDay } from '../planning/tours.js'
import { loadTours } from '../planning/loaders.js'
import { rememberItem, useLoad } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: facts, notices, loading
import '../components/planning/TourBuilder.css'
import '../components/planning/TourReview.css'
import './ToursPage.css'
import './TourPage.css'
import './MillsPage.css'

const MILL_GONE = 'Ce pressoir n’existe plus.'
const loadMill = rememberItem('mill', async (id) => ({
  mill: await getOilMill(id).catch((failure) => {
    if (failure.status === 404) return null
    throw failure
  }),
}))

function Mill({ id }) {
  const loaded = useLoad(id, loadMill)
  const tours = useLoad('tours', loadTours)
  const [chosenDay, setChosenDay] = useState(null)
  const [today] = useState(() => tunisDay(Date.now()))
  const titleRef = useRef(null)
  const mill = loaded.data?.mill
  const day = chosenDay || today
  const route = useMemo(() => ({ line: [], points: mill?.latitude != null && mill?.longitude != null ? [[mill.latitude, mill.longitude]] : [] }), [mill])
  useEffect(() => titleRef.current?.focus(), [mill?.id])

  const back = <Link className="button-quiet back" to="/pressoirs">Tous les pressoirs</Link>
  if (!mill) {
    return (
      <main className="page">
        {back}
        {loaded.error && <div className="notice" role="alert"><p>{loaded.error}</p><button className="button-quiet" type="button" onClick={loaded.retry}>Réessayer</button></div>}
        {!loaded.error && !loaded.data && <div className="loading" role="status"><span className="visually-hidden">Chargement du pressoir</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>}
        {loaded.data && <h1 className="tour-gone">{MILL_GONE}</h1>}
      </main>
    )
  }
  const list = tours.data && toursOfMill(tours.data, id)
  return (
    <main className="tour-page detail-page">
      <aside className="builder review" aria-label="Pressoir">
        <div className="builder-head">
          <div>
            {back}
            <h1 ref={titleRef} tabIndex={-1}>{mill.name}</h1>
            <p><strong className={`tour-status ${mill.active ? 'CONFIRMED' : ''}`}>{mill.active ? 'Actif' : 'Inactif'}</strong> {!mill.active && <span className="muted">Il ne peut pas être ajouté à une tournée.</span>}</p>
          </div>
        </div>
        <div className="builder-body panel-body">
          <h2>Réception</h2>
          <dl className="facts">
            <div><dt>Adresse</dt><dd>{mill.address || 'Non renseignée'}</dd></div>
            <div><dt>Capacité par jour</dt><dd>{formatKg(mill.dailyCapacityKg)}</dd></div>
            <div><dt>Position</dt><dd>{route.points.length > 0 ? 'Sur la carte' : 'Non renseignée'}</dd></div>
          </dl>
          <label className="field mill-day">Capacité restante le<input type="date" value={day} onChange={(event) => setChosenDay(event.target.value)} /></label>
          {tours.error && <div className="notice" role="alert"><p>{tours.error}</p><button className="button-quiet" type="button" onClick={tours.retry}>Réessayer</button></div>}
          {tours.data && mill.active && <p className="mill-left">{formatKg(millLeftKg(mill, bookedByMill(tours.data, day)))}</p>}
          <h2>Tournées</h2>
          {!tours.error && !list && <div className="skeleton" role="status"><span className="visually-hidden">Chargement des tournées</span></div>}
          {list?.length === 0 && <p className="muted">Aucune tournée ne passe par ce pressoir.</p>}
          {list?.length > 0 && (
            <ul className="mill-tours">
              {list.map(({ tour, kg }) => (
                <li key={tour.id}>
                  <Link to={`/tournees/${encodeURIComponent(tour.id)}`}>
                    <span className="mill-tour">{tour.title}<span className="muted">{tour.plannedStartTime ? `${formatInstant(tour.plannedStartTime)}, ${formatTime(tour.plannedStartTime)}` : 'Départ non renseigné'}, {formatKg(kg)}</span></span>
                    <span className={`tour-status ${tour.status}`}>{TOUR_STATUSES[tour.status] ?? tour.status}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </aside>
      <PlanningMap
        single rows={[]} stopIds={[]} mills={[mill]} tourMillIds={[]} route={route} selectedId={null} selectedMillId={id}
        onSelect={() => {}} onSelectMill={() => {}} onHover={() => {}}
      />
    </main>
  )
}

export default function MillPage() {
  const { id } = useParams()
  return <Mill key={id} id={id} />
}
