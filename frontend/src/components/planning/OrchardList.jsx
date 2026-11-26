import { useEffect, useRef } from 'react'
import { formatKg, maturityOf, MATURITIES, REQUEST_TYPES, STATES } from '../../planning/labels.js'
import { NO_FILTERS, UNKNOWN } from '../../planning/orchards.js'
import { canAddStop } from '../../planning/tour.js'
import OrchardMark from './OrchardMark.jsx'

function Filter({ label, value, onChange, options }) {
  return (
    <label className={value ? 'filter set' : 'filter'}>
      <span className="visually-hidden">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">{label}</option>
        {options.map(([key, text]) => <option key={key} value={key}>{text}</option>)}
      </select>
    </label>
  )
}

export default function OrchardList({ rows, total, varieties, filters, onFilters, stopIds, locked, onAdd, hoveredId, onHover, onSelect, returnToId }) {
  const listRef = useRef(null)
  const set = (key) => (value) => onFilters({ ...filters, [key]: value })
  const filtered = Object.values(filters).some((value) => value.trim())

  // The button disappears once used: leave the keyboard on its row
  function add(event, id) {
    event.currentTarget.previousElementSibling.focus()
    onAdd(id)
  }

  // Back from the details: put the keyboard where it was
  useEffect(() => {
    if (returnToId) listRef.current?.querySelector(`[data-id="${CSS.escape(returnToId)}"]`)?.focus()
  }, [returnToId])

  return (
    <>
      <div className="list-head">
        <h1>Vergers</h1>
        <p role="status">{filtered ? `${rows.length} sur ${total}` : total}</p>
      </div>
      <div className="filters">
        <input
          type="search" aria-label="Rechercher un verger, un agriculteur ou une variété" placeholder="Verger, agriculteur, variété"
          value={filters.search} onChange={(event) => set('search')(event.target.value)}
        />
        <Filter label="État" value={filters.state} onChange={set('state')} options={Object.entries(STATES)} />
        <Filter label="Maturité" value={filters.maturity} onChange={set('maturity')} options={[...Object.entries(MATURITIES).map(([key, { label }]) => [key, label]), [UNKNOWN, 'Non renseignée']]} />
        <Filter label="Type" value={filters.type} onChange={set('type')} options={Object.entries(REQUEST_TYPES)} />
        <Filter label="Variété" value={filters.variety} onChange={set('variety')} options={varieties.map((name) => [name, name])} />
      </div>
      {rows.length > 0 ? (
        <ul className="rows" ref={listRef}>
          {rows.map((row) => {
            const order = stopIds.indexOf(row.id) + 1
            const urgent = row.harvest?.requestType === 'URGENT'
            return (
              <li key={row.id} className={hoveredId === row.id ? 'hovered' : undefined}>
                <button
                  type="button" data-id={row.id} className="row" onClick={() => onSelect(row.id)}
                  onMouseEnter={() => onHover(row.id)} onMouseLeave={() => onHover(null)} onFocus={() => onHover(row.id)} onBlur={() => onHover(null)}
                >
                  <OrchardMark state={row.state} color={maturityOf(row.maturity).color} urgent={urgent} size={24} />
                  <span className="row-main">
                    <span className="row-name">{row.name}</span>
                    <span className="row-meta">
                      {row.maturity in MATURITIES ? MATURITIES[row.maturity].label : 'Maturité inconnue'} ·{' '}
                      <span className={`state-${row.state}`}>{order > 0 ? `Arrêt ${order} de la tournée` : STATES[row.state]}</span>
                      {urgent && <>, <strong>urgente</strong></>}
                      {!row.position && ', sans position'}
                    </span>
                  </span>
                  {row.harvest?.estimatedQuantityKg != null && <span className="row-kg">{formatKg(row.harvest.estimatedQuantityKg)}</span>}
                </button>
                {!locked && canAddStop(row) && order === 0 && (
                  <button className="icon-button add" type="button" aria-label={`Ajouter ${row.name} à la tournée`} title="Ajouter à la tournée" onClick={(event) => add(event, row.id)}>
                    <svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" aria-hidden="true"><path d="M8 3v10M3 8h10" /></svg>
                  </button>
                )}
              </li>
            )
          })}
        </ul>
      ) : (
        <div className="empty">
          <p>{total === 0 ? 'Aucun verger enregistré pour le moment.' : 'Aucun verger ne correspond à ces filtres.'}</p>
          {total > 0 && <button className="button-quiet" type="button" onClick={() => onFilters(NO_FILTERS)}>Effacer les filtres</button>}
        </div>
      )}
    </>
  )
}
