import { normalize } from './orchards.js'

// Filters live in the address: ?statut=PLANNED&periode=a-venir&q=...&du=...&au=...&tri=nom&sens=asc
export const PERIODS = { '': 'Toutes les dates', 'aujourd-hui': 'Aujourd’hui', 'a-venir': 'À venir', passees: 'Passées', dates: 'Entre deux dates' }
const SORTS = ['depart', 'nom', 'chef', 'arrets', 'quantite', 'statut']

// Tunisia is UTC+1 all year
export const tunisDay = (instant) => new Date(new Date(instant).getTime() + 3600_000).toISOString().slice(0, 10)

export function readFilters(params) {
  const tri = SORTS.includes(params.get('tri')) ? params.get('tri') : 'depart'
  const sens = ['asc', 'desc'].includes(params.get('sens')) ? params.get('sens') : tri === 'depart' ? 'desc' : 'asc'
  const periode = params.get('periode') in PERIODS ? params.get('periode') : ''
  return { q: params.get('q') ?? '', statut: params.get('statut') ?? '', periode, du: params.get('du') ?? '', au: params.get('au') ?? '', tri, sens }
}

function inPeriod(tour, { periode, du, au }, today) {
  if (!periode) return true
  if (!tour.plannedStartTime) return false
  const day = tunisDay(tour.plannedStartTime)
  if (periode === 'aujourd-hui') return day === today
  if (periode === 'a-venir') return day >= today
  if (periode === 'passees') return day < today
  return (!du || day >= du) && (!au || day <= au)
}

// Search and period only: the status is applied afterwards, so every status keeps its count
export function searchTours(tours, filters, today) {
  const needle = normalize(filters.q.trim())
  return tours.filter((tour) => inPeriod(tour, filters, today) && (!needle ||
    [tour.title, tour.teamLeaderName, ...tour.stops.map(({ orchardName }) => orchardName), ...tour.dropOffs.map(({ oilMillName }) => oilMillName)]
      .some((text) => text && normalize(text).includes(needle))))
}

export function countByStatus(tours) {
  const counts = {}
  for (const { status } of tours) counts[status] = (counts[status] ?? 0) + 1
  return counts
}

const STATUS_ORDER = ['PLANNED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']
const text = (a, b) => (a ?? '').localeCompare(b ?? '', 'fr')
const COMPARE = {
  depart: (a, b) => (Date.parse(a.plannedStartTime) || 0) - (Date.parse(b.plannedStartTime) || 0),
  nom: (a, b) => text(a.title, b.title),
  chef: (a, b) => text(a.teamLeaderName, b.teamLeaderName),
  arrets: (a, b) => a.stops.length - b.stops.length,
  quantite: (a, b) => a.totalQuantityKg - b.totalQuantityKg,
  statut: (a, b) => STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status),
}

export function sortTours(tours, { tri, sens }) {
  const direction = sens === 'asc' ? 1 : -1
  return [...tours].sort((a, b) => direction * COMPARE[tri](a, b) || text(a.title, b.title))
}
