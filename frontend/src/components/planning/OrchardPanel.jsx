import { useEffect, useRef } from 'react'
import { formatDate, formatKg, formatNumber, maturityOf, PRIORITIES, REQUEST_TYPES } from '../../planning/labels.js'
import { canAddStop } from '../../planning/tour.js'
import OrchardReports from './OrchardReports.jsx'
import StateBadge from './StateBadge.jsx'

function Fact({ term, children }) {
  return <div><dt>{term}</dt><dd>{children}</dd></div>
}

// Mounted with key={row.id}, so its state always belongs to one orchard
export default function OrchardPanel({ row, inTour, locked, onAdd, onRemove, onClose }) {
  const titleRef = useRef(null)

  useEffect(() => titleRef.current.focus(), [])

  const { harvest } = row
  const maturity = maturityOf(row.maturity)
  return (
    <div className="panel" onKeyDown={(event) => event.key === 'Escape' && onClose()}>
      <button className="button-quiet back" type="button" onClick={onClose}>
        <svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M10 3 5 8l5 5" /></svg>
        Tous les vergers
      </button>
      <h1 ref={titleRef} tabIndex={-1}>{row.name}</h1>
      <StateBadge state={row.state} />
      {!row.position && <p className="notice">Ce verger n’a pas de coordonnées : il ne peut ni apparaître sur la carte, ni être ajouté à une tournée.</p>}

      {!locked && (inTour || canAddStop(row)) && (
        <button className={inTour ? 'button secondary' : 'button'} type="button" onClick={() => (inTour ? onRemove : onAdd)(row.id)}>
          {inTour ? 'Retirer de la tournée' : 'Ajouter à la tournée'}
        </button>
      )}

      <h2>Demande de récolte</h2>
      {harvest ? (
        <>
          <dl className="facts">
            <Fact term="Type">{harvest.requestType === 'URGENT' ? <strong className="urgent">{REQUEST_TYPES.URGENT}</strong> : REQUEST_TYPES[harvest.requestType]}</Fact>
            <Fact term="Quantité estimée">{formatKg(harvest.estimatedQuantityKg)}</Fact>
            <Fact term="Date souhaitée">{harvest.preferredDate ? formatDate(harvest.preferredDate) : 'Non renseignée'}</Fact>
            {harvest.priorityTier && <Fact term="Priorité">{PRIORITIES[harvest.priorityTier] ?? harvest.priorityTier}</Fact>}
            <Fact term="Agriculteur">{harvest.farmerName ?? 'Non renseigné'}</Fact>
            {harvest.farmerPhone && <Fact term="Téléphone"><a href={`tel:${harvest.farmerPhone}`}>{harvest.farmerPhone}</a></Fact>}
          </dl>
          {harvest.notes && <p className="note">{harvest.notes}</p>}
        </>
      ) : <p className="muted">Aucune demande en cours : ce verger ne peut pas être ajouté à une tournée.</p>}

      <h2>Verger</h2>
      <dl className="facts">
        <Fact term="Maturité"><span className="maturity"><i style={{ background: maturity.color }} />{maturity.label}</span></Fact>
        <Fact term="Variétés">{row.varieties.join(', ') || 'Non renseignées'}</Fact>
        {row.areaHectares != null && <Fact term="Surface">{formatNumber(row.areaHectares)} ha</Fact>}
        {row.treeCount != null && <Fact term="Oliviers">{formatNumber(row.treeCount)}</Fact>}
      </dl>

      <OrchardReports orchardId={row.id} />
    </div>
  )
}
