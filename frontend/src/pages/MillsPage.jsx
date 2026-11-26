import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import PageLoading from '../components/PageLoading.jsx'
import FilterCounts from '../components/FilterCounts.jsx'
import PlanningMap from '../components/map/PlanningMap.jsx'
import SortableTable from '../components/SortableTable.jsx'
import { formatKg } from '../planning/labels.js'
import { DESCENDING_FIRST, millStatus, readMillFilters, searchMills, sortMills } from '../planning/mills.js'
import { bookedByMill, millLeftKg } from '../planning/tour.js'
import { countByStatus, tunisDay } from '../planning/tours.js'
import { nextSort, useAddress } from '../planning/useAddress.js'
import { loadMills, loadTours } from '../planning/loaders.js'
import { useLoad, useReady } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: notices, loading
import './ToursPage.css'
import './MillsPage.css'


export default function MillsPage() {
  const mills = useLoad('mills', loadMills)
  const tours = useLoad('tours', loadTours)
  const ready = useReady(mills, tours)
  const { params, text, type, change, clear } = useAddress()
  const navigate = useNavigate()
  const [today] = useState(() => tunisDay(Date.now()))
  const filters = readMillFilters(params, today)
  const booked = useMemo(() => tours.data && bookedByMill(tours.data, filters.jour), [tours.data, filters.jour])

  const found = mills.data && searchMills(mills.data, filters)
  const shown = found && sortMills(found.filter((mill) => !filters.statut || millStatus(mill) === filters.statut), filters, booked)
  const route = useMemo(() => ({
    line: [], points: (mills.data ?? []).filter(({ latitude, longitude }) => latitude != null && longitude != null).map(({ latitude, longitude }) => [latitude, longitude]),
  }), [mills.data])
  const left = (mill) => (!mill.active ? 'Inactif' : booked ? formatKg(millLeftKg(mill, booked)) : tours.error ? 'Non vérifiée' : '…')

  if (!ready) return <main className="page"><PageLoading /></main>
  return (
    <main className="mills-page">
      <div className="page float-left">
        <div className="page-head">
          <h1>Pressoirs</h1>
          <button className="button-quiet" type="button" onClick={() => { mills.retry(); tours.retry() }}>Actualiser</button>
        </div>
        <FilterCounts
          allLabel="Tous" labels={{ active: 'Actifs', inactive: 'Inactifs' }} total={found?.length}
          counts={found && countByStatus(found.map((mill) => ({ status: millStatus(mill) })))} value={filters.statut} onChange={(statut) => change({ statut: filters.statut === statut ? '' : statut })}
        />
        <div className="tour-tools">
          <input type="search" aria-label="Rechercher un pressoir" placeholder="Nom, adresse" value={text} onChange={(event) => type(event.target.value)} />
          <label>Capacité restante le <input type="date" value={filters.jour} onChange={(event) => change({ jour: event.target.value }, true)} /></label>
        </div>
        {mills.error && <div className="notice" role="alert"><p>{mills.error}</p><button className="button-quiet" type="button" onClick={mills.retry}>Réessayer</button></div>}
        {tours.error && !mills.error && <div className="notice" role="alert"><p>La quantité restante n’a pas pu être vérifiée. {tours.error}</p><button className="button-quiet" type="button" onClick={tours.retry}>Réessayer</button></div>}
        {!mills.error && !shown && <div className="loading" role="status"><span className="visually-hidden">Chargement des pressoirs</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>}
        {shown?.length === 0 && (
          <div className="empty">
            <p>{mills.data.length === 0 ? 'Aucun pressoir enregistré.' : 'Aucun pressoir ne correspond à ces filtres.'}</p>
            {mills.data.length > 0 && <button className="button-quiet" type="button" onClick={() => clear()}>Effacer les filtres</button>}
          </div>
        )}
        {shown?.length > 0 && (
          <SortableTable columns={[['nom', 'Pressoir'], ['capacite', 'Par jour'], ['reste', 'Reste'], ['statut', 'Statut']]} numeric={['capacite', 'reste']} filters={filters} onSort={(key) => change(nextSort(filters, key, DESCENDING_FIRST))}>
            {shown.map((mill) => (
              <tr key={mill.id}>
                <td><Link to={`/pressoirs/${encodeURIComponent(mill.id)}`}>{mill.name}</Link><span className="muted mill-address">{mill.address || 'Adresse non renseignée'}</span></td>
                <td className="numeric">{formatKg(mill.dailyCapacityKg)}</td>
                <td className="numeric">{left(mill)}</td>
                <td><span className={`tour-status ${mill.active ? 'CONFIRMED' : ''}`}>{mill.active ? 'Actif' : 'Inactif'}</span></td>
              </tr>
            ))}
          </SortableTable>
        )}
      </div>
      <PlanningMap
        single rows={[]} stopIds={[]} mills={mills.data ?? []} tourMillIds={[]} route={route} selectedId={null} selectedMillId={null}
        onSelect={() => {}} onSelectMill={(id) => navigate(`/pressoirs/${encodeURIComponent(id)}`)} onHover={() => {}}
      />
    </main>
  )
}
