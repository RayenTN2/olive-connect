import { describe, expect, it } from 'vitest'
import { buildRows, filterRows, listVarieties, NO_FILTERS, toLatLng } from './orchards.js'
import { formatDate, formatInstant, formatKg } from './labels.js'

const feature = (orchardId, extra = {}) => ({
  geometry: { coordinates: [10.76, 34.74] },
  properties: { orchardId, name: `Verger ${orchardId}`, oliveVarieties: ['Chemlali'], currentMaturity: 'GREEN', ...extra },
})
const request = (orchardId, status, extra = {}) => ({ id: `r-${orchardId}`, orchardId, status, requestType: 'PLANNED', ...extra })
const build = (features, requests) => buildRows({ features }, requests)

describe('toLatLng', () => {
  it('swaps GeoJSON order and rejects unusable coordinates', () => {
    expect(toLatLng([10.76, 34.74])).toEqual([34.74, 10.76])
    expect(toLatLng(undefined)).toBe(null)
    expect(toLatLng([10.76, null])).toBe(null)
  })
})

describe('buildRows', () => {
  it.each([
    ['APPROVED', 'eligible'], ['PENDING', 'pending'], ['SCHEDULED', 'scheduled'], ['IN_PROGRESS', 'scheduled'],
    ['COMPLETED', 'none'], ['REJECTED', 'none'], ['CANCELLED', 'none'],
  ])('%s request → %s', (status, state) => {
    expect(build([feature('a')], [request('a', status)])[0].state).toBe(state)
  })

  it('uses the active request, not an older finished one', () => {
    const [row] = build([feature('a')], [request('a', 'COMPLETED', { id: 'old' }), request('a', 'APPROVED', { id: 'new' })])
    expect(row.harvest.id).toBe('new')
  })

  it('keeps a request whose orchard is not on the map', () => {
    const [row] = build([], [request('x', 'APPROVED', { orchardName: 'Sans GPS' })])
    expect(row).toMatchObject({ name: 'Sans GPS', position: null, state: 'eligible' })
  })

  it('sorts by priority, orchards without a request last', () => {
    const rows = build([feature('a'), feature('b'), feature('c')],
      [request('b', 'APPROVED', { priorityScore: 20 }), request('c', 'APPROVED', { priorityScore: 90 })])
    expect(rows.map((row) => row.id)).toEqual(['c', 'b', 'a'])
  })
})

describe('filterRows', () => {
  const rows = build(
    [feature('a', { name: 'El Aïn Nord', oliveVarieties: ['Koroneiki'], currentMaturity: 'OPTIMAL' }), feature('b')],
    [request('a', 'APPROVED', { requestType: 'URGENT', farmerName: 'Démo Agricole' })])
  const ids = (filters) => filterRows(rows, { ...NO_FILTERS, ...filters }).map((row) => row.id)

  it('filters by each criterion', () => {
    expect(ids({})).toEqual(['a', 'b'])
    expect(ids({ state: 'none' })).toEqual(['b'])
    expect(ids({ maturity: 'OPTIMAL' })).toEqual(['a'])
    expect(ids({ type: 'URGENT' })).toEqual(['a'])
    expect(ids({ variety: 'Chemlali' })).toEqual(['b'])
    expect(ids({ state: 'eligible', maturity: 'GREEN' })).toEqual([])
  })

  it('searches name, farmer and variety without accents or case', () => {
    expect(ids({ search: ' ain ' })).toEqual(['a'])
    expect(ids({ search: 'demo' })).toEqual(['a'])
    expect(ids({ search: 'chemlali' })).toEqual(['b'])
  })

  it('lists varieties once', () => expect(listVarieties(rows)).toEqual(['Chemlali', 'Koroneiki']))
})

describe('formats', () => {
  it('keeps plain dates, converts instants to Tunis, and never invents a quantity', () => {
    expect(formatDate('2026-11-25')).toBe('25 nov. 2026')
    expect(formatInstant('2026-11-24T23:30:00Z')).toBe('25 nov. 2026')
    expect(formatKg(null)).toBe('Non renseignée')
    expect(formatKg(0)).toBe('0 kg')
    expect(formatKg(1250.5)).toBe('1 250,5 kg')
  })
})
