import { useState } from 'react'
import { setResourceActive } from '../../api/logistics.js'
import { loadCalendar } from '../../logistics/loaders.js'
import { saveChange } from '../../logistics/save.js'
import { useRun } from '../../logistics/useRun.js'
import { formatInstant, formatTime, TOUR_STATUSES } from '../../planning/labels.js'
import { useLoad } from '../../planning/useLoad.js'
import { Ask, LoadError, Skeleton } from './parts.jsx'

// Deactivating never deletes: it only keeps the resource out of new tours
export function ActiveSwitch({ kind, item, read, name, onDone }) {
  const [asking, setAsking] = useState(false)
  const run = useRun()

  const change = (active) => run.run(async () => {
    try {
      onDone(await saveChange({ base: item, read, send: () => setResourceActive(kind, item.id, active), matches: (after) => after.active === active }), active ? 'Réactivé.' : 'Désactivé.')
    } catch (failure) {
      if (failure.current) onDone(failure.current, '')
      throw failure
    }
    setAsking(false)
  })

  return (
    <>
      {run.error && <p className="urgent" role="alert">{run.error}</p>}
      {asking && item.active ? (
        <Ask label="Désactiver" danger busy={run.busy} onConfirm={() => change(false)} onCancel={() => setAsking(false)}>
          <p>« {name} » ne pourra plus être choisi pour une tournée. L’historique est conservé et vous pourrez réactiver.</p>
        </Ask>
      ) : item.active
        ? <button className="button-quiet danger" type="button" onClick={() => setAsking(true)}>Désactiver</button>
        : <button className="button-quiet" type="button" aria-disabled={run.busy} onClick={() => change(true)}>{run.busy ? 'Enregistrement…' : 'Réactiver'}</button>}
    </>
  )
}

// span: "start|end" of the calendar, from the start of today
export function ResourceTours({ id, span, days }) {
  const calendar = useLoad(span, loadCalendar)
  const entries = calendar.data?.find((row) => row.id === id)?.entries.filter(({ kind }) => kind === 'TOUR') ?? []
  return (
    <section className="block" aria-label="Tournées">
      <div className="block-head"><h2>Tournées, {days} prochains jours</h2></div>
      {calendar.error && <LoadError load={calendar} />}
      {!calendar.error && !calendar.data && <Skeleton label="Chargement des tournées" />}
      {calendar.data && entries.length === 0 && <p className="muted">Aucune tournée confirmée.</p>}
      <ul className="periods">
        {entries.map((entry) => (
          <li key={entry.tourId}>
            <div className="period">
              <span className="period-main"><span className="period-dates">{entry.tourTitle}</span><span className="muted">{formatInstant(entry.start)}, {formatTime(entry.start)} à {formatTime(entry.end)}</span></span>
              <span className={`tour-status ${entry.tourStatus}`}>{TOUR_STATUSES[entry.tourStatus] ?? entry.tourStatus}</span>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
