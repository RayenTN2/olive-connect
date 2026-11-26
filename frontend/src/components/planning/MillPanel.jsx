import { useEffect, useRef } from 'react'
import { formatDate, formatKg } from '../../planning/labels.js'
import { millLeftKg } from '../../planning/tour.js'

function Fact({ term, children }) {
  return <div><dt>{term}</dt><dd>{children}</dd></div>
}

// day: the tour's day (2026-11-25) or null; load: its useLoad result. Mounted with key={mill.id}
export default function MillPanel({ mill, day, load, inTour, locked, onAdd, onRemove, onClose }) {
  const titleRef = useRef(null)
  useEffect(() => titleRef.current.focus(), [])

  let left = 'Choisissez la date de la tournée'
  if (mill.dailyCapacityKg == null) left = 'Non renseignée'
  else if (day && load.data) left = formatKg(millLeftKg(mill, load.data))
  else if (day) left = load.error ? 'Non vérifiée' : 'Calcul…'
  return (
    <div className="panel" onKeyDown={(event) => event.key === 'Escape' && onClose()}>
      <button className="button-quiet back" type="button" onClick={onClose}>
        <svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M10 3 5 8l5 5" /></svg>
        Retour
      </button>
      <h1 ref={titleRef} tabIndex={-1}>{mill.name}</h1>
      <p className="state-badge state-scheduled">{inTour ? 'Pressoir de la tournée' : 'Pressoir'}</p>
      {!locked && (
        <button className={inTour ? 'button secondary' : 'button'} type="button" onClick={() => (inTour ? onRemove : onAdd)(mill)}>
          {inTour ? 'Retirer de la tournée' : 'Ajouter à la tournée'}
        </button>
      )}
      <h2>Réception</h2>
      <dl className="facts">
        <Fact term="Adresse">{mill.address || 'Non renseignée'}</Fact>
        <Fact term="Capacité par jour">{formatKg(mill.dailyCapacityKg)}</Fact>
        <Fact term={day ? `Reste le ${formatDate(day)}` : 'Reste ce jour-là'}>{left}</Fact>
      </dl>
      {day && load.error && <div className="notice" role="alert"><p>{load.error}</p><button className="button-quiet" type="button" onClick={load.retry}>Réessayer</button></div>}
      <p className="muted review-note">Les brouillons ne réservent aucune capacité. Les pressoirs sont visités après tous les vergers.</p>
    </div>
  )
}
