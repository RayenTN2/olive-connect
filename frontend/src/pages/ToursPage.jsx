import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import FilterCounts from '../components/FilterCounts.jsx'
import SortableTable from '../components/SortableTable.jsx'
import { formatInstant, formatKg, formatTime, TOUR_STATUSES } from '../planning/labels.js'
import { countByStatus, PERIODS, readFilters, searchTours, sortTours, tunisDay } from '../planning/tours.js'
import { nextSort, useAddress } from '../planning/useAddress.js'
import { loadTours } from '../planning/loaders.js'
import { useLoad } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: notices, loading
import './ToursPage.css'

const COLUMNS = [['nom', 'Tournée'], ['depart', 'Départ'], ['chef', 'Chef d’équipe'], ['arrets', 'Arrêts'], ['quantite', 'Quantité'], [null, 'Pressoirs'], ['statut', 'Statut']]
const NUMERIC = ['arrets', 'quantite']
const COUNT_LABELS = { PLANNED: 'Brouillons', CONFIRMED: 'Confirmées', IN_PROGRESS: 'En cours', COMPLETED: 'Terminées', CANCELLED: 'Annulées' }

export default function ToursPage() {
  const tours = useLoad('tours', loadTours)
  const { params, text, type, change, clear } = useAddress()
  const { state } = useLocation()
  const navigate = useNavigate()
  const [today] = useState(() => tunisDay(Date.now()))
  const filters = readFilters(params)

  const found = tours.data && searchTours(tours.data, filters, today)
  const counts = found && countByStatus(found)
  const shown = found && sortTours(found.filter(({ status }) => !filters.statut || status === filters.statut), filters)

  return (
    <main className="page">
      <div className="page-head">
        <h1>Tournées</h1>
        <button className="button-quiet" type="button" onClick={tours.retry}>Actualiser</button>
        <button className="button" type="button" onClick={() => navigate('/planification')}>Nouvelle tournée</button>
      </div>
      {state?.notice && <p className="page-notice" role="status">{state.notice}</p>}

      <FilterCounts
        allLabel="Toutes" labels={COUNT_LABELS} total={found?.length} counts={counts} value={filters.statut}
        onChange={(status) => change({ statut: filters.statut === status ? '' : status })}
      />

      <div className="tour-tools">
        <input type="search" aria-label="Rechercher une tournée, un chef, un verger ou un pressoir" placeholder="Tournée, chef, verger, pressoir" value={text} onChange={(event) => type(event.target.value)} />
        <label>
          <span className="visually-hidden">Période</span>
          <select value={filters.periode} onChange={(event) => change({ periode: event.target.value, du: '', au: '' })}>
            {Object.entries(PERIODS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </select>
        </label>
        {filters.periode === 'dates' && (
          <>
            <label>Du <input type="date" value={filters.du} max={filters.au || undefined} onChange={(event) => change({ du: event.target.value }, true)} /></label>
            <label>au <input type="date" value={filters.au} min={filters.du || undefined} onChange={(event) => change({ au: event.target.value }, true)} /></label>
          </>
        )}
        {shown && <p className="muted" role="status">{shown.length} {shown.length > 1 ? 'tournées' : 'tournée'}</p>}
      </div>

      {tours.error && <div className="notice" role="alert"><p>{tours.error}</p><button className="button-quiet" type="button" onClick={tours.retry}>Réessayer</button></div>}
      {!tours.error && !shown && <div className="loading" role="status"><span className="visually-hidden">Chargement des tournées</span>{[1, 2, 3, 4].map((n) => <div className="skeleton" key={n} />)}</div>}
      {shown?.length === 0 && (
        <div className="empty">
          <p>{tours.data.length === 0 ? 'Aucune tournée enregistrée. Préparez la première depuis la carte.' : 'Aucune tournée ne correspond à ces filtres.'}</p>
          {tours.data.length > 0 && <button className="button-quiet" type="button" onClick={() => clear()}>Effacer les filtres</button>}
        </div>
      )}
      {shown?.length > 0 && (
        <SortableTable columns={COLUMNS} numeric={NUMERIC} filters={filters} onSort={(key) => change(nextSort(filters, key, ['depart']))}>
            {shown.map((tour) => (
              <tr key={tour.id}>
                <td><Link to={`/tournees/${encodeURIComponent(tour.id)}`}>{tour.title}</Link></td>
                <td>{tour.plannedStartTime ? `${formatInstant(tour.plannedStartTime)}, ${formatTime(tour.plannedStartTime)}` : 'Non renseigné'}</td>
                <td>{tour.teamLeaderName ?? 'Non renseigné'}</td>
                <td className="numeric">{tour.stops.length}</td>
                <td className="numeric">{formatKg(tour.totalQuantityKg)}</td>
                <td>{tour.dropOffs.map(({ oilMillName }) => oilMillName).filter(Boolean).join(', ') || 'Non renseigné'}</td>
                <td><span className={`tour-status ${tour.status}`}>{TOUR_STATUSES[tour.status] ?? tour.status}</span></td>
              </tr>
            ))}
        </SortableTable>
      )}
    </main>
  )
}
