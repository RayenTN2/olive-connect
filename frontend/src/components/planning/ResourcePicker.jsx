import { equipmentTypeLabel, equipmentTypes, formatKg } from '../../planning/labels.js'
import { unavailableIds } from '../../planning/rules.js'
import { binCapacityKg, totalKg } from '../../planning/tour.js'

function Choices({ legend, summary, items, selected, lost, checking, onToggle, name, detail, none }) {
  // Chosen resources that are no longer free stay listed, so they can be seen and removed
  const listed = [...items, ...selected.filter(({ id }) => !items.some((item) => item.id === id))]
  return (
    <fieldset className="group" disabled={checking}>
      <legend>{legend}</legend>
      {summary && <p className="muted">{summary}</p>}
      {listed.length === 0 && <p className="muted">{none}</p>}
      <ul className="choices">
        {listed.map((item) => {
          const gone = lost.has(item.id)
          return (
            <li key={item.id}>
              <label className={gone ? 'choice gone' : 'choice'}>
                <input type="checkbox" checked={selected.some(({ id }) => id === item.id)} onChange={() => onToggle(item)} />
                <span>{name(item)}</span>
                <span className={gone ? 'urgent' : 'muted'}>{gone ? 'Indisponible' : detail(item)}</span>
              </label>
            </li>
          )
        })}
      </ul>
    </fieldset>
  )
}

export default function ResourcePicker({ tour, stops, resources, dispatch, onSchedule }) {
  if (!resources.wanted) {
    return (
      <div className="notice">
        <p>Choisissez d’abord la date et l’heure de départ : les disponibilités en dépendent.</p>
        <button className="button-quiet" type="button" onClick={onSchedule}>Choisir le départ</button>
      </div>
    )
  }
  if (resources.error) return <div className="notice" role="alert"><p>{resources.error}</p><button className="button-quiet" type="button" onClick={resources.retry}>Réessayer</button></div>
  if (!resources.shown) return <div className="loading" role="status"><span className="visually-hidden">Vérification des disponibilités</span>{[1, 2, 3].map((n) => <div className="skeleton" key={n} />)}</div>

  // Not checked yet for this period: shown greyed, and nothing can be chosen
  const checking = resources.data === null
  const { workers, equipment } = resources.data ?? resources.shown
  const lost = checking ? new Set() : unavailableIds(tour, resources.data)
  const types = equipmentTypes([...equipment, ...tour.equipment])
  return (
    <div className={checking ? 'checking' : undefined} aria-busy={checking}>
      <p className="muted" role="status">{checking ? 'Vérification des disponibilités…' : 'Ressources libres pendant les 8 heures qui suivent le départ.'}</p>
      <Choices
        legend="Ouvriers" summary={`${tour.workers.length} choisi${tour.workers.length > 1 ? 's' : ''}, 2 au minimum.`}
        items={workers} selected={tour.workers} lost={lost} checking={checking} none="Aucun ouvrier libre à cet horaire."
        onToggle={(item) => dispatch({ type: 'toggleWorker', item })} name={(item) => item.fullName} detail={() => ''}
      />
      {types.length === 0 && <p className="muted">Aucun équipement libre à cet horaire.</p>}
      {types.map((type) => (
        <Choices
          key={type} legend={equipmentTypeLabel(type)}
          summary={type === 'BIN' ? `Capacité ${formatKg(binCapacityKg(tour.equipment))} pour ${formatKg(totalKg(stops))} à récolter.` : ''}
          items={equipment.filter((item) => item.type === type)} selected={tour.equipment.filter((item) => item.type === type)} lost={lost} checking={checking}
          onToggle={(item) => dispatch({ type: 'toggleEquipment', item })}
          name={(item) => item.name || item.reference} detail={(item) => (item.capacityKg != null ? formatKg(item.capacityKg) : item.name ? item.reference : '')}
        />
      ))}
    </div>
  )
}
