import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import Bars from '../components/Bars.jsx'
import PageLoading from '../components/PageLoading.jsx'
import { coming, countDrafts, countLateDrafts, countPending, countReady, countToExamine, countUrgent, millLoads } from '../planning/dashboard.js'
import { formatKg, formatLongDay, formatTime, TOUR_STATUSES } from '../planning/labels.js'
import { loadMills, loadOrchards, loadProblems, loadRequests, loadTours } from '../planning/loaders.js'
import { buildRows } from '../planning/orchards.js'
import { tunisDay } from '../planning/tours.js'
import { useLoad, useReady } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: notices, loading
import './ToursPage.css'
import './DashboardPage.css'

const LEVELS = {
  ok: { color: 'var(--blue)', text: '' },
  high: { color: '#b86a00', text: 'Presque plein' },
  full: { color: 'var(--danger)', text: 'Complet' },
  over: { color: 'var(--danger)', text: 'Capacité dépassée' },
}

// sources: the loads a block needs. One that failed never shows as 0
function Block({ title, sources, aside, children }) {
  const failed = sources.filter(({ error }) => error)
  const ready = sources.every(({ data }) => data !== null)
  let content
  if (failed.length > 0) {
    content = <div className="notice" role="alert"><p>{failed[0].error}</p><button className="button-quiet" type="button" onClick={() => failed.forEach(({ retry }) => retry())}>Réessayer</button></div>
  } else if (!ready) {
    content = <div className="loading" role="status"><span className="visually-hidden">Chargement</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>
  } else content = children()
  return (
    <section className="block" aria-label={title}>
      <div className="block-head"><h2>{title}</h2>{aside}</div>
      {content}
    </section>
  )
}

const Arrow = () => <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m7.5 4 6 6-6 6" /></svg>

// count answers with { value, detail, tone }
function Todo({ label, to, sources, count }) {
  const failed = sources.filter(({ error }) => error)
  if (failed.length > 0) {
    return (
      <button type="button" className="dash-todo" onClick={() => failed.forEach(({ retry }) => retry())}>
        <span className="count-number" aria-hidden="true">–</span><span className="todo-text">{label}<span className="todo-failed">Non chargé. Réessayer</span></span>
      </button>
    )
  }
  const { value, detail, tone } = sources.every(({ data }) => data !== null) ? count() : { value: '…' }
  return (
    <Link className={value > 0 ? `dash-todo ${tone}` : 'dash-todo'} to={to}>
      <span className="count-number">{value}</span><span className="todo-text">{label}{detail && <span className="muted">{detail}</span>}</span><Arrow />
    </Link>
  )
}

function TourRows({ tours }) {
  return (
    <ul className="dash-tours">
      {tours.map((tour) => (
        <li key={tour.id}>
          <Link to={`/tournees/${encodeURIComponent(tour.id)}`}>
            <span className="dash-time">{formatTime(tour.plannedStartTime)}</span>
            <span className="dash-name">{tour.title}<span className="muted">{tour.teamLeaderName ?? 'Chef non renseigné'}, {tour.stops.length} {tour.stops.length > 1 ? 'vergers' : 'verger'}, {formatKg(tour.totalQuantityKg)}</span></span>
            <span className={`tour-status ${tour.status}`}>{TOUR_STATUSES[tour.status] ?? tour.status}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function Days({ tours }) {
  const days = new Map()
  for (const tour of tours) {
    const day = tunisDay(tour.plannedStartTime)
    days.set(day, [...(days.get(day) ?? []), tour])
  }
  return [...days].map(([day, list]) => <div key={day}><h3 className="dash-date">{formatLongDay(day)}</h3><TourRows tours={list} /></div>)
}

export default function DashboardPage() {
  const tours = useLoad('tours', loadTours)
  const requests = useLoad('requests', loadRequests)
  const orchards = useLoad('orchards', loadOrchards)
  const mills = useLoad('mills', loadMills)
  const problems = useLoad('problems', loadProblems)
  const ready = useReady(tours, requests, orchards, mills, problems)
  const navigate = useNavigate()
  const [now, setNow] = useState(() => Date.now())
  const today = tunisDay(now)
  const [chosenDay, setChosenDay] = useState(null)
  const millDay = chosenDay || today

  function refresh() {
    setNow(Date.now())
    for (const source of [tours, requests, orchards, mills, problems]) source.retry()
  }

  return (
    <main className="page">
      <div className="page-head">
        <div className="page-title">
          <p className="muted dash-date">{formatLongDay(today)}</p>
          <h1>À faire aujourd’hui</h1>
        </div>
        <button className="button-quiet" type="button" onClick={refresh}>Actualiser<span className="visually-hidden">, dernière fois à {formatTime(now)}</span></button>
        <button className="button" type="button" onClick={() => navigate('/planification')}>Préparer une tournée</button>
      </div>

      {!ready ? <PageLoading /> : (
        <div className="dash">
          <div className="dash-column">
            <section className="todos" aria-label="À traiter">
              <Todo
                label="Demandes à valider" to="/demandes" sources={[requests]}
                count={() => {
                  const urgent = countUrgent(requests.data)
                  return { value: countPending(requests.data), detail: urgent > 0 && `dont ${urgent} ${urgent > 1 ? 'urgentes' : 'urgente'}`, tone: urgent > 0 ? 'red' : 'amber' }
                }}
              />
              <Todo label="Vergers prêts à planifier" to="/planification?etat=eligible" sources={[orchards, requests]} count={() => ({ value: countReady(buildRows(orchards.data, requests.data)), tone: 'green' })} />
              <Todo
                label="Brouillons à confirmer" to="/tournees?statut=PLANNED" sources={[tours]}
                count={() => {
                  const late = countLateDrafts(tours.data, today)
                  return { value: countDrafts(tours.data), detail: late > 0 && `${late} dont la date est passée`, tone: late > 0 ? 'red' : 'amber' }
                }}
              />
              <Todo label="Problèmes à examiner" to="/signalements" sources={[problems]} count={() => ({ value: countToExamine(problems.data), tone: 'amber' })} />
            </section>

            <Block
              title="Charge des pressoirs" sources={[tours, mills]}
              aside={<label className="block-day"><span className="visually-hidden">Jour</span><input type="date" value={millDay} onChange={(event) => setChosenDay(event.target.value)} /></label>}
            >
              {() => {
                const active = mills.data.filter((mill) => mill.active)
                if (active.length === 0) return <p className="muted">Aucun pressoir actif.</p>
                return (
                  <Bars rows={millLoads(active, tours.data, millDay).map(({ mill, kg, share, level }) => ({
                    key: mill.id, label: mill.name, value: kg, max: mill.dailyCapacityKg, color: LEVELS[level].color, text: `${formatKg(kg)} sur ${formatKg(mill.dailyCapacityKg)}`, to: `/pressoirs/${encodeURIComponent(mill.id)}`,
                    note: LEVELS[level].text && `${LEVELS[level].text}${Number.isFinite(share) ? `, ${Math.round(share * 100)} %` : ''}`,
                  }))} />
                )
              }}
            </Block>
          </div>

          <Block title="Tournées" sources={[tours]}>
            {() => {
              const list = coming(tours.data, today)
              return (
                <>
                  <h3>Aujourd’hui</h3>
                  {list.today.length > 0 ? <TourRows tours={list.today} /> : <p className="muted">Aucune tournée aujourd’hui.</p>}
                  {list.next.length > 0 ? <Days tours={list.next} /> : <><h3>Les 7 prochains jours</h3><p className="muted">Aucune tournée prévue.</p></>}
                </>
              )
            }}
          </Block>
        </div>
      )}
    </main>
  )
}
