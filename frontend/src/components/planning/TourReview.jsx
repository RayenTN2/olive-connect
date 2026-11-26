import { useEffect, useRef } from 'react'
import { equipmentTypeLabel, equipmentTypes, formatInstant, formatKg, formatMinutes, formatNumber, formatTime, TOUR_STATUSES } from '../../planning/labels.js'
import TourActions from './TourActions.jsx'
import './TourBuilder.css'
import './TourReview.css'

function Fact({ term, children }) {
  return <div><dt>{term}</dt><dd>{children}</dd></div>
}

const SITUATION = {
  PLANNED: 'Aucune ressource n’est réservée.', CONFIRMED: 'Les ressources sont réservées.', IN_PROGRESS: 'La tournée est en cours.',
  COMPLETED: 'La tournée est terminée.', CANCELLED: 'Cette tournée a été annulée.',
}

// saved: the tour as the backend returned it. Mounted with a key, so each new state starts fresh and takes the keyboard
export default function TourReview({ saved, notice, busy, error, back, onRefresh, leftover, onRemoveLeftover, actions, selectedId }) {
  const titleRef = useRef(null)
  const stepsRef = useRef(null)
  useEffect(() => titleRef.current.focus(), [])

  // A place chosen on the map: show its step
  useEffect(() => {
    if (selectedId) stepsRef.current.querySelector('.chosen')?.scrollIntoView({ block: 'nearest' })
  }, [selectedId])

  return (
    <aside className="builder review float-left" aria-label="Tournée enregistrée">
      <div className="builder-head">
        <div>
          {back}
          <h1 ref={titleRef} tabIndex={-1}>{saved.title}</h1>
          <p className="muted" role="status"><strong className={`tour-status ${saved.status}`}>{TOUR_STATUSES[saved.status]}</strong> {notice || SITUATION[saved.status]}</p>
        </div>
        <button className="button-quiet" type="button" aria-disabled={busy} onClick={onRefresh}>Actualiser</button>
      </div>
      <div className="builder-foot">
        {error && <p className="urgent" role="alert">{error}</p>}
        {leftover && (
          <div className="leaving" role="alert">
            <p>L’ancienne version du brouillon n’a pas pu être supprimée.</p>
            <button className="button-quiet" type="button" aria-disabled={busy} onClick={() => !busy && onRemoveLeftover()}>{busy ? 'Suppression…' : 'Réessayer la suppression'}</button>
          </div>
        )}
        <TourActions tour={saved} busy={busy} {...actions} />
      </div>
      <div className="builder-body">
        <dl className="facts">
          <Fact term="Départ">{saved.plannedStartTime ? `${formatInstant(saved.plannedStartTime)}, ${formatTime(saved.plannedStartTime)}` : 'Non renseigné'}</Fact>
          <Fact term="Fin estimée">{saved.estimatedEndTime ? formatTime(saved.estimatedEndTime) : 'Non calculée'}</Fact>
          <Fact term="Durée">{formatMinutes(saved.totalTourDurationMinutes)}</Fact>
          <Fact term="Récolte">{formatMinutes(saved.totalHarvestingDurationMinutes)}</Fact>
          <Fact term={saved.routeEstimated === false ? 'Itinéraire routier' : 'Trajet estimé'}>{formatMinutes(saved.totalTravelDurationMinutes)}, {formatNumber(saved.totalDistanceKm)} km</Fact>
          <Fact term="Quantité">{formatKg(saved.totalQuantityKg)}</Fact>
          <Fact term="Chef d’équipe">{saved.teamLeaderName ?? 'Non renseigné'}</Fact>
          {saved.cancellationReason && <Fact term="Motif d’annulation">{saved.cancellationReason}</Fact>}
        </dl>
        {saved.routeEstimated === true && <p className="notice">Le service de routes ne répondait pas : le trajet est une estimation en ligne droite. Distances et horaires peuvent être faux.</p>}
        {saved.routeEstimated == null && <p className="notice">Tournée enregistrée avant le calcul routier : le trajet est probablement une estimation en ligne droite.</p>}
        <p className="muted review-note">Trajet dans l’ordre que vous avez choisi. Le déchargement au pressoir n’est pas compté dans la fin estimée.</p>

        <h2>Étapes</h2>
        <ol className="steps" ref={stepsRef}>
          {saved.stops.map((stop) => (
            <li key={stop.orchardId} className={stop.orchardId === selectedId ? 'chosen' : undefined}>
              <span className="stop-number" aria-hidden="true">{stop.visitOrder}</span>
              <span className="stop-main">
                <span className="step-name">{stop.orchardName}</span>
                <span className="muted">{stop.estimatedArrival && stop.estimatedDeparture ? `${formatTime(stop.estimatedArrival)} à ${formatTime(stop.estimatedDeparture)}, ` : ''}{formatKg(stop.estimatedQuantityKg)}</span>
              </span>
            </li>
          ))}
          {saved.dropOffs.map((dropOff) => (
            <li key={dropOff.oilMillId} className={dropOff.oilMillId === selectedId ? 'chosen' : undefined}>
              <svg className="step-mill" viewBox="0 0 24 24" width="26" height="26" aria-hidden="true"><rect x="1" y="1" width="22" height="22" rx="7.5" fill="var(--blue)" /><path d="M6.5 17v-6l3.2 2.2V11l3.2 2.2V8h4.6v9Z" fill="none" stroke="#fff" strokeWidth="1.6" strokeLinejoin="round" /></svg>
              <span className="stop-main">
                <span className="step-name">{dropOff.oilMillName}</span>
                <span className="muted">{dropOff.estimatedArrival ? `Arrivée à ${formatTime(dropOff.estimatedArrival)}, ` : ''}{formatKg(dropOff.allocatedQuantityKg)}</span>
              </span>
            </li>
          ))}
        </ol>

        <h2>Ressources</h2>
        <dl className="facts">
          <Fact term="Ouvriers">{saved.assignedWorkers.map(({ fullName }) => fullName).join(', ') || 'Aucun'}</Fact>
          {equipmentTypes(saved.assignedEquipment).map((type) => (
            <Fact key={type} term={equipmentTypeLabel(type)}>
              {saved.assignedEquipment.filter((item) => item.type === type).map((item) => item.name || item.reference).join(', ')}
            </Fact>
          ))}
        </dl>
        {saved.notes && <p className="note">{saved.notes}</p>}
      </div>
    </aside>
  )
}
