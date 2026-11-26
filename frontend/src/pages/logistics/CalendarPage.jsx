import { useState } from 'react'
import { Link } from 'react-router'
import FilterCounts from '../../components/FilterCounts.jsx'
import { LoadError, Skeleton } from '../../components/logistics/parts.jsx'
import { byType, dayEntries, daysKey, KINDS, readCalendarFilters, resourcePath, ROW_TESTS, searchCalendar, STATUSES, typeLabel } from '../../logistics/calendar.js'
import { loadCalendar, loadTypes } from '../../logistics/loaders.js'
import { addDays } from '../../planning/dashboard.js'
import { formatDate, TOUR_STATUSES } from '../../planning/labels.js'
import { tunisDay } from '../../planning/tours.js'
import { useAddress } from '../../planning/useAddress.js'
import { useLoad } from '../../planning/useLoad.js'
import '../PlanningPage.css' // shared pieces: notices, loading
import '../ToursPage.css'
import './Logistics.css'

const shortDay = new Intl.DateTimeFormat('fr-FR', { weekday: 'short', day: 'numeric', timeZone: 'UTC' })

function Entry({ entry, hours }) {
  const tour = entry.kind === 'TOUR'
  return (
    <span className={`entry ${entry.kind} ${tour ? entry.tourStatus : ''}`}>
      <b>{hours}</b>
      {tour ? entry.tourTitle : `${KINDS[entry.kind]} : ${entry.reason}`}
      {tour && entry.tourStatus !== 'CONFIRMED' && ` (${TOUR_STATUSES[entry.tourStatus] ?? entry.tourStatus})`}
    </span>
  )
}

export default function CalendarPage() {
  const [today, setToday] = useState(() => tunisDay(Date.now()))
  const { params, text, type, change, clear } = useAddress()
  const filters = readCalendarFilters(params, today)
  const calendar = useLoad(daysKey(filters.semaine, 7), loadCalendar)
  const types = useLoad('types', loadTypes)
  const days = [0, 1, 2, 3, 4, 5, 6].map((n) => addDays(filters.semaine, n))

  const found = calendar.data && searchCalendar(calendar.data, filters)
  const shown = found && found.filter(ROW_TESTS[filters.etat] ?? (() => true))
  const codes = [...new Set([...(types.data ?? []).map(({ code }) => code), ...(calendar.data ?? []).filter((row) => row.equipmentType).map((row) => row.equipmentType)])].sort(byType)
  const go = (week) => change({ semaine: week })
  const rest = params.has('semaine') ? { semaine: filters.semaine } : {}
  return (
    <main className="page">
      <div className="page-head">
        <h1>Calendrier des ressources</h1>
        <button className="button-quiet" type="button" onClick={() => { setToday(tunisDay(Date.now())); calendar.retry() }}>Actualiser</button>
      </div>
      <FilterCounts
        allLabel="Ressources" labels={{ TOUR: 'En tournée', UNAVAILABLE: 'Indisponibles', MAINTENANCE: 'En maintenance' }} total={found?.length}
        counts={found && Object.fromEntries(Object.entries(ROW_TESTS).map(([key, test]) => [key, found.filter(test).length]))} value={filters.etat} onChange={(etat) => change({ etat: filters.etat === etat ? '' : etat })}
      />
      <div className="tour-tools">
        <div className="week-nav">
          <button className="button-quiet" type="button" onClick={() => go(addDays(filters.semaine, -7))}>Précédente</button>
          <strong aria-live="polite">{formatDate(days[0])} – {formatDate(days[6])}</strong>
          <button className="button-quiet" type="button" onClick={() => go(addDays(filters.semaine, 7))}>Suivante</button>
          <button className="button-quiet" type="button" onClick={() => go('')}>Cette semaine</button>
        </div>
        <label><span className="visually-hidden">Aller à la semaine du</span><input type="date" value={filters.semaine} max="9999-12-31" onChange={(event) => event.target.value && go(event.target.value)} /></label>
        <input type="search" aria-label="Rechercher une ressource" placeholder="Nom, référence" value={text} onChange={(event) => type(event.target.value)} />
        <label>
          <span className="visually-hidden">Type de ressource</span>
          <select value={filters.type} onChange={(event) => change({ type: event.target.value })}>
            <option value="">Toutes les ressources</option>
            <option value="WORKER">Ouvriers</option>
            {codes.map((code) => <option key={code} value={code}>{typeLabel(code, types.data)}</option>)}
          </select>
        </label>
      </div>
      {calendar.error && <LoadError load={calendar} />}
      {!calendar.error && !shown && <Skeleton label="Chargement du calendrier" />}
      {shown?.length === 0 && (
        <div className="empty">
          <p>{found.length === 0 && !filters.q && !filters.type ? 'Aucune ressource enregistrée.' : 'Aucune ressource ne correspond à ces filtres cette semaine.'}</p>
          {(filters.q || filters.type || filters.etat) && <button className="button-quiet" type="button" onClick={() => clear(rest)}>Effacer les filtres</button>}
        </div>
      )}
      {shown?.length > 0 && (
        <div className="calendar" tabIndex={0} role="region" aria-label="Calendrier de la semaine">
          <table>
            <thead><tr><th scope="col">Ressource</th>{days.map((day) => <th key={day} scope="col" className={day === today ? 'today' : undefined}>{shortDay.format(new Date(`${day}T00:00:00Z`))}{day === today && <span className="visually-hidden">, aujourd’hui</span>}</th>)}</tr></thead>
            <tbody>
              {shown.map((row) => (
                <tr key={`${row.resourceType} ${row.id}`}>
                  <th scope="row">
                    <Link to={resourcePath(row)}>{row.name || row.reference}</Link>
                    <span className="muted">{row.resourceType === 'WORKER' ? 'Ouvrier' : `${typeLabel(row.equipmentType, types.data)}, ${row.reference}`}{!row.active && `, ${STATUSES.INACTIVE.toLowerCase()}`}</span>
                  </th>
                  {days.map((day) => <td key={day} className={day === today ? 'today' : undefined}>{dayEntries(row.entries, day).map(({ entry, hours }) => <Entry key={`${entry.kind} ${entry.tourId ?? entry.reason} ${entry.start}`} entry={entry} hours={hours} />)}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
