import { useState } from 'react'
import { Link } from 'react-router'
import Bars from '../../components/Bars.jsx'
import { LoadError, Status } from '../../components/logistics/parts.jsx'
import PageLoading from '../../components/PageLoading.jsx'
import { absences, daysKey, KINDS, resourcePath, statusAt, toursOf, typeLabel } from '../../logistics/calendar.js'
import { freeByType } from '../../logistics/lists.js'
import { loadCalendar, loadTypes } from '../../logistics/loaders.js'
import { when } from '../../components/logistics/names.js'
import { formatLongDay, formatTime, TOUR_STATUSES } from '../../planning/labels.js'
import { tunisDay } from '../../planning/tours.js'
import { useLoad, useReady } from '../../planning/useLoad.js'
import '../PlanningPage.css' // shared pieces: notices, loading
import '../ToursPage.css'
import '../DashboardPage.css'
import './Logistics.css'

const DAYS = 7
const plural = (count, word) => `${count} ${word}${count > 1 ? 's' : ''}`

function Todo({ label, detail, value, tone, to }) {
  return (
    <Link className={value > 0 ? `dash-todo ${tone}` : 'dash-todo'} to={to}>
      <span className="count-number">{value}</span><span className="todo-text">{label}<span className="muted">{detail}</span></span>
      <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m7.5 4 6 6-6 6" /></svg>
    </Link>
  )
}

function Content({ calendar, types, now }) {
  const rows = calendar.map((row) => ({ ...row, status: statusAt(row, now) }))
  const workers = rows.filter(({ resourceType }) => resourceType === 'WORKER')
  const equipment = rows.filter(({ resourceType }) => resourceType !== 'WORKER')
  const count = (list, status) => list.filter((row) => row.status === status).length
  const tours = toursOf(calendar)
  const away = absences(calendar, now)
  const bars = freeByType(rows, ({ status }) => status)
  return (
    <div className="dash">
      <div className="dash-column">
        <section className="todos" aria-label="À traiter">
          <Todo label="Ouvriers indisponibles" detail="En ce moment" value={count(workers, 'UNAVAILABLE')} tone="amber" to="/logistique/ouvriers?statut=UNAVAILABLE" />
          <Todo label="Équipements en maintenance" detail="En ce moment" value={count(equipment, 'UNDER_MAINTENANCE')} tone="amber" to="/logistique/equipements?statut=UNDER_MAINTENANCE" />
          <Todo label="Équipements indisponibles" detail="En ce moment" value={count(equipment, 'UNAVAILABLE')} tone="amber" to="/logistique/equipements?statut=UNAVAILABLE" />
        </section>
        <section className="block" aria-label="Disponibles en ce moment">
          <div className="block-head"><h2>Disponibles en ce moment</h2></div>
          {bars.length === 0 ? <p className="muted">Aucune ressource active.</p> : (
            <Bars rows={bars.map(({ key, free, total }) => ({
              key, label: key === 'WORKER' ? 'Ouvriers' : typeLabel(key, types), value: free, max: total, color: 'var(--olive)', text: `${free} sur ${total}`,
              to: key === 'WORKER' ? '/logistique/ouvriers?statut=AVAILABLE' : `/logistique/equipements?${new URLSearchParams({ type: key, statut: 'AVAILABLE' })}`,
            }))} />
          )}
        </section>
      </div>
      <div className="dash-column">
        <section className="block" aria-label="Tournées">
          <div className="block-head"><h2>Tournées, {DAYS} prochains jours</h2></div>
          {tours.length === 0 && <p className="muted">Aucune tournée confirmée.</p>}
          <ul className="periods">
            {tours.map((tour) => (
              <li key={tour.id}>
                <div className="period">
                  <span className="period-main"><span className="period-dates">{tour.title}</span><span className="muted">{when(tour.start)} à {formatTime(tour.end)}, {plural(tour.workers, 'ouvrier')}, {plural(tour.equipment, 'équipement')}</span></span>
                  <span className={`tour-status ${tour.status}`}>{TOUR_STATUSES[tour.status] ?? tour.status}</span>
                </div>
              </li>
            ))}
          </ul>
        </section>
        <section className="block" aria-label="Absences et maintenances">
          <div className="block-head"><h2>Absences et maintenances, {DAYS} prochains jours</h2></div>
          {away.length === 0 && <p className="muted">Aucune période enregistrée.</p>}
          <ul className="dash-tours">
            {away.map(({ row, entry }) => (
              <li key={`${row.id} ${entry.start} ${entry.end} ${entry.reason}`}>
                <Link to={resourcePath(row)}>
                  <span className="dash-name">{row.name || row.reference}<span className="muted">{entry.reason}. {Date.parse(entry.start) > now ? `Du ${when(entry.start)} au ${when(entry.end)}` : `Jusqu’au ${when(entry.end)}`}</span></span>
                  {entry.kind === 'MAINTENANCE' ? <Status status="UNDER_MAINTENANCE" /> : <span className="tour-status UNAVAILABLE">{KINDS.UNAVAILABLE}</span>}
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  )
}

export default function LogisticsDashboardPage() {
  const [now, setNow] = useState(() => Date.now())
  const today = tunisDay(now)
  const calendar = useLoad(daysKey(today, DAYS), loadCalendar)
  const types = useLoad('types', loadTypes)
  const ready = useReady(calendar)

  return (
    <main className="page">
      <div className="page-head">
        <div className="page-title">
          <p className="muted dash-date">{formatLongDay(today)}</p>
          <h1>Ressources aujourd’hui</h1>
        </div>
        <button className="button-quiet" type="button" onClick={() => { setNow(Date.now()); calendar.retry(); types.retry() }}>Actualiser</button>
        <Link className="button" to="/logistique/calendrier">Voir le calendrier</Link>
      </div>
      {calendar.error && <LoadError load={calendar} />}
      {!calendar.error && (calendar.data ? <Content calendar={calendar.data} types={types.data} now={now} /> : !ready && <PageLoading />)}
      {!calendar.error && !calendar.data && ready && <PageLoading />}
    </main>
  )
}
