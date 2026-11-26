import { Link } from 'react-router'
import FilterCounts from '../components/FilterCounts.jsx'
import SortableTable from '../components/SortableTable.jsx'
import { formatInstant, formatTime, MATURITIES, maturityOf, PROBLEM_STATUSES, PROBLEMS } from '../planning/labels.js'
import { DESCENDING_FIRST, readMaturityFilters, readProblemFilters, searchMaturity, searchProblems, sortMaturity, sortProblems } from '../planning/reports.js'
import { countByStatus } from '../planning/tours.js'
import { nextSort, useAddress } from '../planning/useAddress.js'
import { loadMaturity, loadProblems } from '../planning/loaders.js'
import { useLoad } from '../planning/useLoad.js'
import './PlanningPage.css' // shared pieces: notices, loading
import './ToursPage.css'

const COUNT_LABELS = { PENDING_REVIEW: 'À examiner', UNDER_REVIEW: 'En cours d’examen', RESOLVED: 'Résolus', REJECTED: 'Rejetés' }
const received = (instant) => (instant ? `${formatInstant(instant)}, ${formatTime(instant)}` : 'Non renseigné')

function Loading({ load }) {
  if (load.error) return <div className="notice" role="alert"><p>{load.error}</p><button className="button-quiet" type="button" onClick={load.retry}>Réessayer</button></div>
  return <div className="loading" role="status"><span className="visually-hidden">Chargement des signalements</span>{[1, 2, 3, 4].map((n) => <div className="skeleton" key={n} />)}</div>
}

function Empty({ narrowed, text, onClear, onAll }) {
  return (
    <div className="empty">
      <p>{narrowed ? 'Aucun signalement ne correspond à ces filtres.' : text}</p>
      {narrowed && <button className="button-quiet" type="button" onClick={onClear}>Effacer les filtres</button>}
      {!narrowed && onAll && <button className="button-quiet" type="button" onClick={onAll}>Voir tous les problèmes</button>}
    </div>
  )
}

function Problems({ load, address }) {
  const { params, text, type, change, clear } = address
  const filters = readProblemFilters(params)
  const query = params.toString()
  const found = load.data && searchProblems(load.data, filters)
  const shown = found && sortProblems(found.filter(({ status }) => !filters.statut || status === filters.statut), filters)
  return (
    <>
      <FilterCounts allLabel="Tous" labels={COUNT_LABELS} total={found?.length} counts={found && countByStatus(found)} value={filters.statut} onChange={(status) => change({ statut: status || 'toutes' })} />
      <div className="tour-tools">
        <input type="search" aria-label="Rechercher un verger ou une description" placeholder="Verger, description" value={text} onChange={(event) => type(event.target.value)} />
        <label>
          <span className="visually-hidden">Type de problème</span>
          <select value={filters.type} onChange={(event) => change({ type: event.target.value })}>
            <option value="">Tous les types</option>
            {Object.entries(PROBLEMS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </select>
        </label>
        {shown && <p className="muted" role="status">{shown.length} {shown.length > 1 ? 'problèmes' : 'problème'}</p>}
      </div>
      {!shown && <Loading load={load} />}
      {shown?.length === 0 && (
        <Empty
          narrowed={Boolean(filters.q || filters.type)} text={filters.statut === 'PENDING_REVIEW' ? 'Aucun problème à examiner.' : 'Aucun problème avec ce statut.'}
          onClear={() => clear(params.get('statut') ? { statut: params.get('statut') } : {})} onAll={filters.statut ? () => change({ statut: 'toutes' }) : null}
        />
      )}
      {shown?.length > 0 && (
        <SortableTable columns={[['verger', 'Verger'], ['type', 'Problème'], [null, 'Description'], ['recu', 'Reçu le'], ['statut', 'Statut']]} numeric={[]} filters={filters} onSort={(key) => change(nextSort(filters, key, DESCENDING_FIRST))}>
          {shown.map((report) => (
            <tr key={report.id}>
              <td><Link to={`/signalements/${encodeURIComponent(report.id)}${query ? `?${query}` : ''}`}>{report.orchardName ?? 'Verger inconnu'}</Link></td>
              <td className="wrap">{PROBLEMS[report.problemType] ?? PROBLEMS.OTHER}</td>
              <td className="clip">{report.description || 'Non renseignée'}</td>
              <td>{received(report.reportedAt)}</td>
              <td><span className={`tour-status ${report.status}`}>{PROBLEM_STATUSES[report.status] ?? report.status}</span></td>
            </tr>
          ))}
        </SortableTable>
      )}
    </>
  )
}

function Maturity({ load, address }) {
  const { params, text, type, change, clear } = address
  const filters = readMaturityFilters(params)
  const shown = load.data && sortMaturity(searchMaturity(load.data, filters), filters)
  return (
    <>
      <div className="tour-tools">
        <input type="search" aria-label="Rechercher un verger ou une note" placeholder="Verger, note" value={text} onChange={(event) => type(event.target.value)} />
        <label>
          <span className="visually-hidden">Maturité</span>
          <select value={filters.maturite} onChange={(event) => change({ maturite: event.target.value })}>
            <option value="">Toutes les maturités</option>
            {Object.entries(MATURITIES).map(([key, { label }]) => <option key={key} value={key}>{label}</option>)}
          </select>
        </label>
        {shown && <p className="muted" role="status">{shown.length} {shown.length > 1 ? 'signalements' : 'signalement'}</p>}
      </div>
      {!shown && <Loading load={load} />}
      {shown?.length === 0 && <Empty narrowed={Boolean(filters.q || filters.maturite)} text="Aucun signalement de maturité." onClear={() => clear({ onglet: 'maturite' })} />}
      {shown?.length > 0 && (
        <SortableTable columns={[['verger', 'Verger'], ['maturite', 'Maturité'], [null, 'Note'], ['recu', 'Reçu le']]} numeric={[]} filters={filters} onSort={(key) => change(nextSort(filters, key, DESCENDING_FIRST))}>
          {shown.map((report) => (
            <tr key={report.id}>
              <td><Link to={`/planification?verger=${encodeURIComponent(report.orchardId)}`}>{report.orchardName ?? 'Verger inconnu'}<span className="visually-hidden">, voir sur la carte</span></Link></td>
              <td className="wrap"><span className="maturity"><i style={{ background: maturityOf(report.maturityLevel).color }} />{maturityOf(report.maturityLevel).label}</span></td>
              <td className="clip">{report.notes || 'Aucune'}</td>
              <td>{received(report.reportedAt)}</td>
            </tr>
          ))}
        </SortableTable>
      )}
    </>
  )
}

export default function ReportsPage() {
  const address = useAddress()
  const maturityTab = address.params.get('onglet') === 'maturite'
  const problems = useLoad(maturityTab ? null : 'problems', loadProblems)
  const maturity = useLoad(maturityTab ? 'maturity' : null, loadMaturity)
  return (
    <main className="page">
      <div className="page-head">
        <h1>Signalements</h1>
        <button className="button-quiet" type="button" onClick={(maturityTab ? maturity : problems).retry}>Actualiser</button>
      </div>
      <nav className="page-tabs" aria-label="Type de signalement">
        <button type="button" aria-current={maturityTab ? undefined : 'true'} onClick={() => address.clear()}>Problèmes</button>
        <button type="button" aria-current={maturityTab ? 'true' : undefined} onClick={() => address.clear({ onglet: 'maturite' })}>Maturité</button>
      </nav>
      {maturityTab ? <Maturity load={maturity} address={address} /> : <Problems load={problems} address={address} />}
    </main>
  )
}
