import { Link } from 'react-router'
import FilterCounts from '../components/FilterCounts.jsx'
import SortableTable from '../components/SortableTable.jsx'
import { formatDate, formatInstant, formatKg, MATURITIES, maturityOf, PRIORITIES, REQUEST_STATUSES, REQUEST_TYPES } from '../planning/labels.js'
import { DESCENDING_FIRST, readRequestFilters, searchRequests, sortRequests, UNKNOWN } from '../planning/requests.js'
import { countByStatus } from '../planning/tours.js'
import { nextSort, useAddress } from '../planning/useAddress.js'
import { loadRequests } from '../planning/loaders.js'
import { useLoad } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: notices, loading
import './ToursPage.css'

const COLUMNS = [['verger', 'Verger'], ['agriculteur', 'Agriculteur'], ['type', 'Type'], ['quantite', 'Quantité'], ['date', 'Date souhaitée'], ['maturite', 'Maturité'], ['priorite', 'Priorité'], ['recue', 'Reçue le'], ['statut', 'Statut']]
const COUNT_LABELS = { PENDING: 'À valider', APPROVED: 'Acceptées', SCHEDULED: 'Planifiées', IN_PROGRESS: 'En cours', COMPLETED: 'Terminées', REJECTED: 'Refusées', CANCELLED: 'Annulées' }

export default function RequestsPage() {
  const requests = useLoad('requests', loadRequests)
  const { params, text, type, change, clear } = useAddress()
  const filters = readRequestFilters(params)
  const query = params.toString()

  const found = requests.data && searchRequests(requests.data, filters)
  const shown = found && sortRequests(found.filter(({ status }) => !filters.statut || status === filters.statut), filters)
  const narrowed = filters.q || filters.type || filters.maturite
  return (
    <main className="page">
      <div className="page-head">
        <h1>Demandes de récolte</h1>
        <button className="button-quiet" type="button" onClick={requests.retry}>Actualiser</button>
      </div>

      <FilterCounts
        allLabel="Toutes" labels={COUNT_LABELS} total={found?.length} counts={found && countByStatus(found)} value={filters.statut}
        onChange={(status) => change({ statut: status || 'toutes' })}
      />

      <div className="tour-tools">
        <input type="search" aria-label="Rechercher un verger ou un agriculteur" placeholder="Verger, agriculteur" value={text} onChange={(event) => type(event.target.value)} />
        <label>
          <span className="visually-hidden">Type</span>
          <select value={filters.type} onChange={(event) => change({ type: event.target.value })}>
            <option value="">Tous les types</option>
            {Object.entries(REQUEST_TYPES).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </select>
        </label>
        <label>
          <span className="visually-hidden">Maturité</span>
          <select value={filters.maturite} onChange={(event) => change({ maturite: event.target.value })}>
            <option value="">Toutes les maturités</option>
            {Object.entries(MATURITIES).map(([key, { label }]) => <option key={key} value={key}>{label}</option>)}
            <option value={UNKNOWN}>Non renseignée</option>
          </select>
        </label>
        {shown && <p className="muted" role="status">{shown.length} {shown.length > 1 ? 'demandes' : 'demande'}</p>}
      </div>

      {requests.error && <div className="notice" role="alert"><p>{requests.error}</p><button className="button-quiet" type="button" onClick={requests.retry}>Réessayer</button></div>}
      {!requests.error && !shown && <div className="loading" role="status"><span className="visually-hidden">Chargement des demandes</span>{[1, 2, 3, 4].map((n) => <div className="skeleton" key={n} />)}</div>}
      {shown?.length === 0 && (
        <div className="empty">
          <p>{narrowed ? 'Aucune demande ne correspond à ces filtres.' : filters.statut === 'PENDING' ? 'Aucune demande n’attend de décision.' : 'Aucune demande avec ce statut.'}</p>
          {narrowed
            ? <button className="button-quiet" type="button" onClick={() => clear(filters.statut === 'PENDING' ? {} : { statut: filters.statut || 'toutes' })}>Effacer les filtres</button>
            : filters.statut && <button className="button-quiet" type="button" onClick={() => change({ statut: 'toutes' })}>Voir toutes les demandes</button>}
        </div>
      )}
      {shown?.length > 0 && (
        <SortableTable columns={COLUMNS} numeric={['quantite']} filters={filters} onSort={(key) => change(nextSort(filters, key, DESCENDING_FIRST))}>
          {shown.map((item) => (
            <tr key={item.id}>
              <td><Link to={`/demandes/${encodeURIComponent(item.id)}${query ? `?${query}` : ''}`}>{item.orchardName ?? 'Verger inconnu'}</Link></td>
              <td>{item.farmerName ?? 'Non renseigné'}</td>
              <td>{item.requestType === 'URGENT' ? <strong className="urgent">{REQUEST_TYPES.URGENT}</strong> : REQUEST_TYPES[item.requestType] ?? item.requestType}</td>
              <td className="numeric">{formatKg(item.estimatedQuantityKg)}</td>
              <td>{item.preferredDate ? formatDate(item.preferredDate) : 'Non renseignée'}</td>
              <td><span className="maturity"><i style={{ background: maturityOf(item.orchardMaturity).color }} />{maturityOf(item.orchardMaturity).label}</span></td>
              <td>{PRIORITIES[item.priorityTier] ?? 'Non calculée'}</td>
              <td>{item.submittedAt ? formatInstant(item.submittedAt) : 'Non renseignée'}</td>
              <td><span className={`tour-status ${item.status}`}>{REQUEST_STATUSES[item.status] ?? item.status}</span></td>
            </tr>
          ))}
        </SortableTable>
      )}
    </main>
  )
}
