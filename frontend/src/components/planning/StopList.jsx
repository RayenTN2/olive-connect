import { useState } from 'react'
import { formatKg } from '../../planning/labels.js'

const Arrow = ({ down }) => (
  <svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d={down ? 'M3.5 6.5 8 11l4.5-4.5' : 'M3.5 9.5 8 5l4.5 4.5'} />
  </svg>
)

export default function StopList({ stops, dispatch, onShow }) {
  const [announcement, setAnnouncement] = useState('')

  function move(row, index, by) {
    const to = index + by
    if (to < 0 || to >= stops.length) return
    dispatch({ type: 'moveStop', id: row.id, by })
    setAnnouncement(`${row.name}, arrêt ${to + 1} sur ${stops.length}`)
  }

  function remove(event, row) {
    const item = event.currentTarget.closest('li')
    const neighbour = item.nextElementSibling ?? item.previousElementSibling
    neighbour?.querySelector('.stop-name').focus()
    dispatch({ type: 'removeStop', id: row.id })
    setAnnouncement(`${row.name} retiré de la tournée`)
  }

  if (stops.length === 0) return <p className="muted">Aucun arrêt. Ajoutez un verger prêt à planifier depuis la liste ou la carte.</p>
  return (
    <>
      <p className="muted">L’ordre de visite est celui que vous choisissez. Le trajet sera calculé à l’enregistrement.</p>
      <ol className="stops">
        {stops.map((row, index) => (
          <li key={row.id}>
            <span className="stop-number" aria-hidden="true">{index + 1}</span>
            <span className="stop-main">
              <button className="stop-name" type="button" onClick={() => onShow(row.id)}>{row.name}</button>
              <span className="muted">{row.harvest.estimatedQuantityKg == null ? 'Quantité non renseignée' : formatKg(row.harvest.estimatedQuantityKg)}</span>
            </span>
            {/* aria-disabled, not disabled: the button must keep the keyboard when a stop reaches the end */}
            <button className="icon-button" type="button" aria-label={`Monter ${row.name}`} aria-disabled={index === 0} onClick={() => move(row, index, -1)}><Arrow /></button>
            <button className="icon-button" type="button" aria-label={`Descendre ${row.name}`} aria-disabled={index === stops.length - 1} onClick={() => move(row, index, 1)}><Arrow down /></button>
            <button className="icon-button" type="button" aria-label={`Retirer ${row.name}`} onClick={(event) => remove(event, row)}>
              <svg viewBox="0 0 16 16" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" aria-hidden="true"><path d="m4 4 8 8M12 4l-8 8" /></svg>
            </button>
          </li>
        ))}
      </ol>
      <p className="visually-hidden" role="status">{announcement}</p>
    </>
  )
}
