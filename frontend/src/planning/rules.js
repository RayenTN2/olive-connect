import { formatKg, formatMinutes } from './labels.js'
import { allocatedKg, binCapacityKg, canAddStop, harvestMinutes, millLeftKg, parseKg, startInstant, totalKg } from './tour.js'

// What the backend will require, checked early. status:
//   ok      satisfied
//   todo    the backend would refuse the tour
//   warn    allowed, but worth a look
//   server  only the backend can decide, when the tour is saved
const OPENING = 7 * 60
const CLOSING = 17 * 60
const MIN_WORKERS = 2
const EPSILON = 1e-6

const minutesOf = (time) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3, 5))

export function checkTour({ tour, stops, availability, booked = null }) {
  const rules = []
  const add = (section, status, label, detail = '') => rules.push({ section, status, label, detail })
  const quantity = totalKg(stops)
  const start = startInstant(tour)

  // Stops
  if (stops.length === 0) add('stops', 'todo', 'Arrêts', 'Ajoutez au moins un verger.')
  else if (!stops.every(canAddStop)) {
    const gone = stops.filter((row) => !canAddStop(row)).map(({ name }) => name).join(', ')
    add('stops', 'todo', 'Arrêts', `N’est plus à planifier : ${gone}. Retirez-le de la tournée.`)
  } else {
    const unknown = stops.filter((row) => row.harvest.estimatedQuantityKg == null).length
    if (unknown > 0) add('stops', 'warn', 'Arrêts', `Quantité non renseignée pour ${unknown} ${unknown > 1 ? 'vergers, comptés' : 'verger, compté'} pour 0 kg.`)
    else add('stops', 'ok', 'Arrêts')
  }

  // Organisation
  add('organisation', tour.title.trim() ? 'ok' : 'todo', 'Nom de la tournée', tour.title.trim() ? '' : 'Donnez un nom à la tournée.')
  if (!tour.leader) add('organisation', 'todo', 'Chef d’équipe', 'Choisissez un chef d’équipe.')
  else if (!availability) add('organisation', 'server', 'Chef d’équipe', 'Disponibilité non vérifiée pour cet horaire.')
  else if (!availability.leaders.some(({ id }) => id === tour.leader.id)) add('organisation', 'todo', 'Chef d’équipe', `${tour.leader.fullName} n’est pas libre à cet horaire.`)
  else add('organisation', 'ok', 'Chef d’équipe')

  const startMinutes = start ? minutesOf(tour.time) : null
  if (!start) add('organisation', 'todo', 'Départ', 'Choisissez la date et l’heure de départ.')
  else if (startMinutes < OPENING) add('organisation', 'todo', 'Départ', 'Une tournée ne peut pas partir avant 07:00.')
  else add('organisation', 'ok', 'Départ')

  if (start && startMinutes >= OPENING && stops.length > 0) {
    const harvest = harvestMinutes(stops, tour.workers.length)
    if (startMinutes + harvest > CLOSING) {
      add('resources', 'todo', 'Fin avant 17:00', `La récolte seule dure ${formatMinutes(harvest)}. Ajoutez des ouvriers, retirez un arrêt ou partez plus tôt.`)
    } else {
      add('organisation', 'server', 'Fin avant 17:00', `Récolte seule : ${formatMinutes(harvest)}. Les trajets seront calculés à l’enregistrement.`)
    }
  }

  // Mills
  const badAmount = tour.dropOffs.find(({ kg }) => !(parseKg(kg) > 0))
  const overCapacity = tour.dropOffs.find(({ mill, kg }) => parseKg(kg) > mill.dailyCapacityKg)
  const overLeft = booked && tour.dropOffs.find(({ mill, kg }) => parseKg(kg) > millLeftKg(mill, booked) + EPSILON)
  const gap = quantity - allocatedKg(tour.dropOffs)
  if (tour.dropOffs.length === 0) add('organisation', 'todo', 'Pressoirs', 'Choisissez au moins un pressoir.')
  else if (badAmount) add('organisation', 'todo', 'Pressoirs', `Saisissez une quantité supérieure à 0 pour ${badAmount.mill.name}.`)
  else if (Math.abs(gap) > EPSILON) add('organisation', 'todo', 'Pressoirs', gap > 0 ? `Il reste ${formatKg(gap)} à affecter.` : `${formatKg(-gap)} affectés en trop.`)
  else if (overCapacity) add('organisation', 'todo', 'Pressoirs', `${overCapacity.mill.name} accepte au plus ${formatKg(overCapacity.mill.dailyCapacityKg)} par jour.`)
  else if (overLeft) add('organisation', 'warn', 'Pressoirs', `Il ne reste que ${formatKg(millLeftKg(overLeft.mill, booked))} à ${overLeft.mill.name} ce jour-là. L’enregistrement risque d’être refusé.`)
  else add('organisation', 'server', 'Pressoirs', 'Capacité restante du jour vérifiée à l’enregistrement.')

  // Resources
  const workers = tour.workers.length
  if (workers < MIN_WORKERS) add('resources', 'todo', 'Ouvriers', `Choisissez au moins ${MIN_WORKERS} ouvriers (${workers} choisi${workers > 1 ? 's' : ''}).`)
  else add('resources', 'ok', 'Ouvriers')

  const capacity = binCapacityKg(tour.equipment)
  if (capacity + EPSILON < quantity) add('resources', 'todo', 'Bennes', `Capacité ${formatKg(capacity)} pour ${formatKg(quantity)} à récolter.`)
  else add('resources', 'ok', 'Bennes')

  if (workers + tour.equipment.length > 0 && start) {
    if (!availability) add('resources', 'server', 'Disponibilité', 'Non vérifiée pour cet horaire.')
    else {
      const lost = unavailableIds(tour, availability).size
      if (lost > 0) add('resources', 'todo', 'Disponibilité', `${lost} ressource${lost > 1 ? 's' : ''} indisponible${lost > 1 ? 's' : ''} à cet horaire.`)
      else add('resources', 'ok', 'Disponibilité')
    }
  }
  return rules
}

export function unavailableIds(tour, availability) {
  const free = new Set([...availability.workers, ...availability.equipment].map(({ id }) => id))
  return new Set([...tour.workers, ...tour.equipment].map(({ id }) => id).filter((id) => !free.has(id)))
}
