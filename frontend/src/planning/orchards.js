// Joins the map's orchards with their harvest requests. One row per orchard.

const ACTIVE = ['PENDING', 'APPROVED', 'SCHEDULED', 'IN_PROGRESS']
export const STATE_BY_STATUS = { PENDING: 'pending', APPROVED: 'eligible', SCHEDULED: 'scheduled', IN_PROGRESS: 'scheduled' }

// Filter value for orchards without a maturity
export const UNKNOWN = 'inconnue'

export const NO_FILTERS = { search: '', state: '', maturity: '', type: '', variety: '' }

// GeoJSON is [longitude, latitude]; Leaflet wants [latitude, longitude]
export function toLatLng(coordinates) {
  if (!Array.isArray(coordinates) || coordinates.length < 2) return null
  const [longitude, latitude] = coordinates
  return Number.isFinite(latitude) && Number.isFinite(longitude) ? [latitude, longitude] : null
}

export function buildRows(collection, requests) {
  // The backend allows one active request per orchard; finished ones don't matter for planning
  const active = new Map()
  for (const item of requests) {
    if (ACTIVE.includes(item.status) && !active.has(item.orchardId)) active.set(item.orchardId, item)
  }

  const rows = collection.features.map(({ geometry, properties }) => {
    const harvest = active.get(properties.orchardId) ?? null
    active.delete(properties.orchardId)
    return {
      id: properties.orchardId,
      name: properties.name,
      varieties: properties.oliveVarieties ?? [],
      areaHectares: properties.surfaceAreaHectares,
      treeCount: properties.treeCount,
      maturity: properties.currentMaturity ?? null,
      harvestStatus: properties.harvestStatus ?? null,
      position: toLatLng(geometry?.coordinates),
      harvest,
      state: harvest ? STATE_BY_STATUS[harvest.status] : 'none',
    }
  })

  // The map endpoint leaves out orchards without coordinates: their requests must stay visible in the list
  for (const harvest of active.values()) {
    rows.push({
      id: harvest.orchardId,
      name: harvest.orchardName ?? 'Verger inconnu',
      varieties: [],
      areaHectares: null,
      treeCount: null,
      maturity: harvest.orchardMaturity ?? null,
      harvestStatus: null,
      position: null,
      harvest,
      state: STATE_BY_STATUS[harvest.status],
    })
  }

  const score = (row) => row.harvest?.priorityScore ?? -1
  return rows.sort((a, b) => score(b) - score(a) || a.name.localeCompare(b.name, 'fr'))
}

export const normalize = (text) => text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()

export function filterRows(rows, { search, state, maturity, type, variety }) {
  const needle = normalize(search.trim())
  return rows.filter((row) =>
    (!state || row.state === state) &&
    (!maturity || (row.maturity ?? UNKNOWN) === maturity) &&
    (!type || row.harvest?.requestType === type) &&
    (!variety || row.varieties.includes(variety)) &&
    (!needle || [row.name, row.harvest?.farmerName ?? '', ...row.varieties].some((text) => normalize(text).includes(needle))))
}

export function listVarieties(rows) {
  return [...new Set(rows.flatMap((row) => row.varieties))].sort((a, b) => a.localeCompare(b, 'fr'))
}
