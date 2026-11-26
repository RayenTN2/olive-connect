import { describe, expect, it } from 'vitest'
import { nextPending, readRequestFilters, searchRequests, sortRequests } from './requests.js'
import { requestError } from './saveErrors.js'
import { nextSort } from './useAddress.js'

const item = (id, status, requestType, orchardMaturity, priorityScore, x = {}) => ({ id, status, requestType, orchardMaturity, priorityScore, orchardName: `Verger ${id}`, farmerName: 'Amel', estimatedQuantityKg: 100, preferredDate: '2026-11-30', submittedAt: '2026-11-20T08:00:00Z', ...x })
const requests = [
  item('a', 'PENDING', 'PLANNED', null, 10),
  item('b', 'PENDING', 'URGENT', 'OPTIMAL', 85, { farmerName: 'Béchir' }),
  item('c', 'APPROVED', 'PLANNED', 'OVERRIPE', 50, { estimatedQuantityKg: null }),
  item('d', 'PENDING', 'URGENT', null, null, { submittedAt: '2026-11-19T08:00:00Z', orchardName: null }),
]
const read = (query) => readRequestFilters(new URLSearchParams(query))
const ids = (list) => list.map(({ id }) => id).join('')

describe('readRequestFilters', () => {
  it('opens on what waits for a decision', () => {
    expect(read('')).toMatchObject({ statut: 'PENDING', tri: 'priorite', sens: 'desc' })
    expect(read('statut=toutes').statut).toBe('')
    expect(read('tri=verger').sens).toBe('asc')
    expect(read('tri=x&sens=y')).toMatchObject({ tri: 'priorite', sens: 'desc' })
  })
})

it('searches and filters, keeping unknown maturity apart from every level', () => {
  expect(ids(searchRequests(requests, read('q=bechir')))).toBe('b')
  expect(ids(searchRequests(requests, read('type=URGENT')))).toBe('bd')
  expect(ids(searchRequests(requests, read('maturite=inconnue')))).toBe('ad')
  expect(ids(searchRequests(requests, read('maturite=OPTIMAL')))).toBe('b')
})

it('sorts with missing values last both ways, oldest first on a tie', () => {
  expect(ids(sortRequests(requests, read('')))).toBe('bcad')
  expect(ids(sortRequests(requests, read('tri=priorite&sens=asc')))).toBe('acbd')
  expect(ids(sortRequests(requests, read('tri=maturite')))).toBe('cbda')
  expect(ids(sortRequests(requests, read('tri=quantite&sens=asc')))).toBe('dabc')
  expect(ids(sortRequests(requests, read('tri=type')))).toBe('dbac')
})

it('finds the next request waiting, with the same filters', () => {
  expect(nextPending(requests, read(''), 'b').id).toBe('a')
  expect(nextPending(requests, read('type=URGENT'), 'b').id).toBe('d')
  expect(nextPending(requests, read('maturite=OPTIMAL'), 'b')).toBe(null)
})

it('toggles a column from its natural direction', () => {
  expect(nextSort({ tri: 'x', sens: 'asc' }, 'recue', ['recue'])).toEqual({ tri: 'recue', sens: 'desc' })
  expect(nextSort({ tri: 'recue', sens: 'desc' }, 'recue', ['recue']).sens).toBe('asc')
  expect(nextSort({ tri: 'recue', sens: 'asc' }, 'recue', ['recue']).sens).toBe('desc')
  expect(nextSort({ tri: 'recue', sens: 'asc' }, 'verger', ['recue']).sens).toBe('asc')
})

it('explains a refused decision in French', () => {
  expect(requestError(400, 'Only PENDING harvest requests can be accepted or rejected. Current status: APPROVED')).toContain('déjà')
  expect(requestError(400, 'Harvest request not found with id: x')).toContain('n’existe plus')
  expect(requestError(403, '')).toContain('compte')
  expect(requestError(400, 'raw')).toContain('a refusé')
})
