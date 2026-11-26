import { useState } from 'react'
import { Link } from 'react-router'
import FilterCounts from '../../components/FilterCounts.jsx'
import { LoadError, Skeleton, Status } from '../../components/logistics/parts.jsx'
import WorkerWindow from '../../components/logistics/WorkerWindow.jsx'
import PageLoading from '../../components/PageLoading.jsx'
import SortableTable from '../../components/SortableTable.jsx'
import { daysKey, statuses } from '../../logistics/calendar.js'
import { readWorkerFilters, searchWorkers, sortResources } from '../../logistics/lists.js'
import { loadCalendar, loadWorkers } from '../../logistics/loaders.js'
import { resourcesChanged } from '../../logistics/useRun.js'
import { countByStatus, tunisDay } from '../../planning/tours.js'
import { nextSort, useAddress } from '../../planning/useAddress.js'
import { useLoad, useReady } from '../../planning/useLoad.js'
import '../PlanningPage.css' // shared pieces: notices, loading
import '../ToursPage.css'
import './Logistics.css'

export default function WorkersPage() {
  const [now, setNow] = useState(() => Date.now())
  const workers = useLoad('workers', loadWorkers)
  const span = daysKey(tunisDay(now), 1)
  const calendar = useLoad(span, loadCalendar)
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(null)
  const ready = useReady(workers, calendar)
  const { params, text, type, change, clear } = useAddress()
  const filters = readWorkerFilters(params)
  const query = params.toString()

  if (!ready) return <main className="page"><PageLoading /></main>
  const status = workers.data && statuses(workers.data, calendar.data, now)
  const found = workers.data && searchWorkers(workers.data, filters)
  const shown = found && sortResources(found.filter(({ id }) => !filters.statut || status[id] === filters.statut), filters, status)
  return (
    <main className="page">
      <div className="page-head">
        <h1>Ouvriers</h1>
        <button className="button-quiet" type="button" onClick={() => { setNow(Date.now()); workers.retry(); calendar.retry() }}>Actualiser</button>
        <button className="button" type="button" onClick={() => { setAdded(null); setAdding(true) }}>Nouvel ouvrier</button>
      </div>
      <div role="status">{added && <p className="page-notice">« {added.fullName} » est ajouté. <Link to={`/logistique/ouvriers/${encodeURIComponent(added.id)}`}>Voir la fiche</Link></p>}</div>
      {adding && (
        <WorkerWindow
          worker={null} onClose={() => setAdding(false)}
          onSaved={(saved) => {
            setAdding(false)
            setAdded(saved)
            resourcesChanged('workers', saved.id, span)
          }}
        />
      )}
      <FilterCounts
        allLabel="Tous" labels={{ AVAILABLE: 'Disponibles', IN_USE: 'En tournée', UNAVAILABLE: 'Indisponibles', INACTIVE: 'Désactivés' }} total={found?.length}
        counts={found && calendar.data && countByStatus(found.map(({ id }) => ({ status: status[id] })))} value={filters.statut} onChange={(statut) => change({ statut: filters.statut === statut ? '' : statut })}
      />
      <div className="tour-tools">
        <input type="search" aria-label="Rechercher un ouvrier" placeholder="Nom, téléphone" value={text} onChange={(event) => type(event.target.value)} />
        {shown && <p className="muted" role="status">{shown.length} {shown.length > 1 ? 'ouvriers' : 'ouvrier'}, situation actuelle</p>}
      </div>
      {workers.error && <LoadError load={workers} />}
      {calendar.error && !workers.error && <LoadError load={calendar} before="Les disponibilités n’ont pas pu être vérifiées." />}
      {!workers.error && !shown && <Skeleton label="Chargement des ouvriers" />}
      {shown?.length === 0 && (
        <div className="empty">
          <p>{workers.data.length === 0 ? 'Aucun ouvrier enregistré.' : 'Aucun ouvrier ne correspond à ces filtres.'}</p>
          {workers.data.length > 0 && <button className="button-quiet" type="button" onClick={() => clear()}>Effacer les filtres</button>}
        </div>
      )}
      {shown?.length > 0 && (
        <SortableTable columns={[['nom', 'Ouvrier'], [null, 'Téléphone'], ['statut', 'Situation']]} numeric={[]} filters={filters} onSort={(key) => change(nextSort(filters, key, []))}>
          {shown.map((worker) => (
            <tr key={worker.id}>
              <td><Link to={`/logistique/ouvriers/${encodeURIComponent(worker.id)}${query ? `?${query}` : ''}`}>{worker.fullName}</Link></td>
              <td>{worker.phone ?? 'Non renseigné'}</td>
              <td><Status status={status[worker.id]} /></td>
            </tr>
          ))}
        </SortableTable>
      )}
    </main>
  )
}
