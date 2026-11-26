import { normalize } from './orchards.js'
import { readSort, sortWith } from './sort.js'
import { millLeftKg } from './tour.js'

export const DESCENDING_FIRST = ['capacite', 'reste']

// ?statut=active|inactive&q=...&jour=2026-11-25&tri=nom
export function readMillFilters(params, today) {
  return { q: params.get('q') ?? '', statut: params.get('statut') ?? '', jour: params.get('jour') || today, ...readSort(params, ['nom', 'capacite', 'reste', 'statut'], 'nom', DESCENDING_FIRST) }
}

export const millStatus = ({ active }) => (active ? 'active' : 'inactive')

export function searchMills(mills, { q }) {
  const needle = normalize(q.trim())
  return mills.filter((mill) => !needle || [mill.name, mill.address].some((text) => text && normalize(text).includes(needle)))
}

// booked: { millId: kg }, or null when it isn't known
export function sortMills(mills, { tri, sens }, booked) {
  const value = {
    nom: ({ name }) => name,
    capacite: ({ dailyCapacityKg }) => dailyCapacityKg,
    reste: (mill) => (booked && mill.active ? millLeftKg(mill, booked) : null),
    statut: ({ active }) => (active ? 0 : 1),
  }[tri]
  return sortWith(mills, value, sens, () => 0)
}

// Its tours, latest first; cancelled ones deliver nothing
export function toursOfMill(tours, id) {
  return tours
    .filter((tour) => tour.status !== 'CANCELLED' && tour.dropOffs.some(({ oilMillId }) => oilMillId === id))
    .map((tour) => ({ tour, kg: tour.dropOffs.filter(({ oilMillId }) => oilMillId === id).reduce((sum, { allocatedQuantityKg }) => sum + allocatedQuantityKg, 0) }))
    .sort((a, b) => (Date.parse(b.tour.plannedStartTime) || 0) - (Date.parse(a.tour.plannedStartTime) || 0))
}
