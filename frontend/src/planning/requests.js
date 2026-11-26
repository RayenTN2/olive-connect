import { normalize, UNKNOWN } from './orchards.js'
import { readSort, sortWith, time } from './sort.js'

// Filters live in the address: ?statut=APPROVED&type=URGENT&maturite=OPTIMAL&q=...&tri=verger&sens=asc
const SORTS = ['priorite', 'verger', 'agriculteur', 'type', 'quantite', 'date', 'maturite', 'recue', 'statut']
export const DESCENDING_FIRST = ['priorite', 'quantite', 'maturite', 'recue']
export { UNKNOWN }

// Requests waiting for a decision come first; "toutes" shows every status
export function readRequestFilters(params) {
  const statut = params.get('statut') ?? 'PENDING'
  return { q: params.get('q') ?? '', statut: statut === 'toutes' ? '' : statut, type: params.get('type') ?? '', maturite: params.get('maturite') ?? '', ...readSort(params, SORTS, 'priorite', DESCENDING_FIRST) }
}

// Everything but the status, so every status keeps its count
export function searchRequests(requests, { q, type, maturite }) {
  const needle = normalize(q.trim())
  return requests.filter((item) =>
    (!type || item.requestType === type) &&
    (!maturite || (maturite === UNKNOWN ? !item.orchardMaturity : item.orchardMaturity === maturite)) &&
    (!needle || [item.orchardName, item.farmerName].some((text) => text && normalize(text).includes(needle))))
}

const STATUS_ORDER = ['PENDING', 'APPROVED', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED']
const MATURITY_ORDER = ['GREEN', 'VERAISON', 'OPTIMAL', 'OVERRIPE']
const VALUE = {
  priorite: (item) => item.priorityScore,
  verger: (item) => item.orchardName,
  agriculteur: (item) => item.farmerName,
  type: (item) => (item.requestType === 'URGENT' ? 0 : 1),
  quantite: (item) => item.estimatedQuantityKg,
  date: (item) => item.preferredDate,
  maturite: (item) => (item.orchardMaturity ? MATURITY_ORDER.indexOf(item.orchardMaturity) : null),
  recue: (item) => time(item.submittedAt),
  statut: (item) => STATUS_ORDER.indexOf(item.status),
}

// Missing values come last in both directions; ties go to the oldest request
export const sortRequests = (requests, { tri, sens }) => sortWith(requests, VALUE[tri], sens, (item) => time(item.submittedAt) ?? 0)

// The next one waiting, in the order of the list the user came from
export function nextPending(requests, filters, id) {
  return sortRequests(searchRequests(requests, filters).filter((item) => item.status === 'PENDING' && item.id !== id), filters)[0] ?? null
}
