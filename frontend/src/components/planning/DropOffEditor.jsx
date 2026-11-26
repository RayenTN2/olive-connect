import { formatKg } from '../../planning/labels.js'
import { allocatedKg, kgText, millLeftKg, parseKg } from '../../planning/tour.js'

// onHover(millId or null): the map names the mill the pointer or keyboard is on
export default function DropOffEditor({ dropOffs, quantity, mills, booked, dispatch, onHover }) {
  const point = (id) => ({ onMouseEnter: () => onHover(id), onMouseLeave: () => onHover(null), onFocus: () => onHover(id), onBlur: () => onHover(null) })
  const left = (mill) => (booked ? `Reste ${formatKg(millLeftKg(mill, booked))} ce jour-là` : `${formatKg(mill.dailyCapacityKg)} par jour au total`)
  const remaining = quantity - allocatedKg(dropOffs)
  const unused = (mills.data ?? []).filter((mill) => !dropOffs.some((item) => item.mill.id === mill.id))

  return (
    <fieldset className="group">
      <legend>Pressoirs</legend>
      <p className="muted">{formatKg(allocatedKg(dropOffs))} affectés sur {formatKg(quantity)}. Les pressoirs sont visités après tous les vergers.</p>
      {mills.error && <div className="notice" role="alert"><p>{mills.error}</p><button className="button-quiet" type="button" onClick={mills.retry}>Réessayer</button></div>}
      {!mills.error && !mills.data && <div className="skeleton" role="status"><span className="visually-hidden">Chargement des pressoirs</span></div>}
      {dropOffs.map(({ mill, kg }) => {
        const others = remaining + (parseKg(kg) || 0)
        return (
          <div className="dropoff" key={mill.id} {...point(mill.id)}>
            <span className="dropoff-name">{mill.name}<span className="muted">{left(mill)}</span></span>
            <label>
              <span className="visually-hidden">Quantité pour {mill.name}, en kg</span>
              <input inputMode="decimal" value={kg} onChange={(event) => dispatch({ type: 'setMillKg', id: mill.id, kg: event.target.value })} /> kg
            </label>
            <span className="dropoff-actions">
              {others > 0 && Math.abs(remaining) > 1e-6 && (
                <button className="button-quiet" type="button" onClick={() => dispatch({ type: 'setMillKg', id: mill.id, kg: kgText(others) })}>Ajuster à {formatKg(others)}</button>
              )}
              <button className="button-quiet" type="button" aria-label={`Retirer ${mill.name}`} onClick={() => dispatch({ type: 'removeMill', id: mill.id })}>Retirer</button>
            </span>
          </div>
        )
      })}
      {mills.data?.length === 0 && <p className="muted">Aucun pressoir actif.</p>}
      {unused.length > 0 && (
        // A list, not a select: a select's options can't show which mill it is on the map
        <div className="mill-choices" role="group" aria-label="Ajouter un pressoir">
          <p className="mill-choices-title">Ajouter un pressoir</p>
          <ul>
            {unused.map((mill) => (
              <li key={mill.id}>
                <button
                  type="button" className="mill-choice" {...point(mill.id)}
                  onClick={() => { onHover(null); dispatch({ type: 'addMill', mill, kg: remaining > 0 ? kgText(remaining) : '' }) }}
                >
                  <span className="dropoff-name">{mill.name}<span className="muted">{left(mill)}</span></span>
                  <span className="mill-choice-add" aria-hidden="true">+ Ajouter</span>
                  <span className="visually-hidden">, ajouter à la tournée</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </fieldset>
  )
}
