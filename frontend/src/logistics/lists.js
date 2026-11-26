import { normalize } from '../planning/orchards.js'
import { readSort, sortWith } from '../planning/sort.js'
import { typeLabel, typeRank } from './calendar.js'

const readList = (params, columns) => ({ q: params.get('q') ?? '', statut: params.get('statut') ?? '', ...readSort(params, columns, 'nom', ['capacite']) })
export const readWorkerFilters = (params) => readList(params, ['nom', 'statut'])
export const readEquipmentFilters = (params) => ({ type: params.get('type') ?? '', ...readList(params, ['nom', 'reference', 'type', 'capacite', 'statut']) })

const matches = (q, texts) => {
  const needle = normalize(q.trim())
  return !needle || texts.some((text) => text && normalize(text).includes(needle))
}
export const searchWorkers = (workers, { q }) => workers.filter(({ fullName, phone }) => matches(q, [fullName, phone]))
export const searchEquipment = (items, { q, type }) => items.filter((item) => (!type || item.type === type)
  && matches(q, [item.name, item.reference, item.brand, item.model, item.serialNumber, item.registrationNumber]))

const ORDER = ['AVAILABLE', 'IN_USE', 'UNAVAILABLE', 'UNDER_MAINTENANCE', 'INACTIVE', 'UNKNOWN']
export function sortResources(items, { tri, sens }, status, types) {
  const value = {
    nom: ({ fullName, name }) => fullName ?? name,
    reference: ({ reference }) => reference,
    type: ({ type }) => typeLabel(type, types),
    capacite: ({ capacityKg }) => capacityKg,
    statut: ({ id }) => ORDER.indexOf(status[id]),
  }[tri]
  return sortWith(sortWith(items, ({ fullName, name }) => fullName ?? name, 'asc', () => 0), value, sens, () => 0)
}

// Free now, over all active ones, per type; workers first
export function freeByType(calendar, statusOf) {
  const groups = new Map()
  for (const row of calendar.filter(({ active }) => active)) {
    const key = row.resourceType === 'WORKER' ? 'WORKER' : row.equipmentType
    const group = groups.get(key) ?? { key, rank: typeRank(row), total: 0, free: 0 }
    group.total += 1
    if (statusOf(row) === 'AVAILABLE') group.free += 1
    groups.set(key, group)
  }
  return sortWith([...groups.values()], ({ rank }) => rank, 'asc', () => 0)
}
