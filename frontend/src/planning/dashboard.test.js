import { expect, it } from 'vitest'
import { addDays, coming, countDrafts, countLateDrafts, countPending, countReady, countToExamine, countUrgent, millLoads } from './dashboard.js'
import { filterRows, NO_FILTERS } from './orchards.js'

const today = '2026-11-25'
const tour = (id, status, plannedStartTime, kg = 100) => ({ id, status, plannedStartTime, totalQuantityKg: kg, dropOffs: [{ oilMillId: 'm1', allocatedQuantityKg: kg }] })
const ids = (list) => list.map(({ id }) => id).join('')

it('counts what needs the manager', () => {
  const requests = [{ status: 'PENDING', requestType: 'URGENT' }, { status: 'PENDING', requestType: 'PLANNED' }, { status: 'APPROVED', requestType: 'URGENT' }]
  expect([countPending(requests), countUrgent(requests)]).toEqual([2, 1])
  expect(countReady([{ state: 'eligible' }, { state: 'pending' }])).toBe(1)
  expect(countToExamine([{ status: 'PENDING_REVIEW' }, { status: 'UNDER_REVIEW' }, { status: 'RESOLVED' }])).toBe(1)
  const tours = [tour('a', 'PLANNED', '2026-11-24T07:00:00Z'), tour('b', 'PLANNED', '2026-11-25T06:00:00Z'), tour('c', 'CONFIRMED', '2026-11-20T07:00:00Z'), tour('d', 'PLANNED', null)]
  expect([countDrafts(tours), countLateDrafts(tours, today)]).toEqual([3, 1])
})

it('lists today and the 7 following days in Tunis time, without cancelled tours', () => {
  const tours = [
    tour('z', 'CONFIRMED', '2026-11-25T14:00:00Z'), tour('a', 'COMPLETED', '2026-11-24T23:30:00Z'), tour('b', 'CANCELLED', '2026-11-25T08:00:00Z'),
    tour('l', 'PLANNED', '2026-12-02T08:00:00Z'), tour('n', 'PLANNED', '2026-11-26T08:00:00Z'), tour('x', 'PLANNED', '2026-12-03T08:00:00Z'), tour('y', 'PLANNED', null), tour('o', 'PLANNED', '2026-11-24T08:00:00Z'),
  ]
  expect(ids(coming(tours, today).today)).toBe('az')
  expect(ids(coming(tours, today).next)).toBe('nl')
  expect(addDays('2026-11-30', 1)).toBe('2026-12-01')
})

it.each([[0, 'ok'], [799, 'ok'], [800, 'high'], [999, 'high'], [1000, 'full'], [1001, 'over']])('a mill of 1000 kg with %i kg is %s', (kg, level) => {
  expect(millLoads([{ id: 'm1', name: 'A', dailyCapacityKg: 1000 }], [tour('t', 'CONFIRMED', '2026-11-25T07:00:00Z', kg)], today)[0]).toMatchObject({ kg, level })
})

it('shows every mill, busiest first, without counting drafts', () => {
  const mills = [{ id: 'm2', name: 'B', dailyCapacityKg: 100 }, { id: 'm1', name: 'A', dailyCapacityKg: 100 }]
  const loads = millLoads(mills, [tour('t', 'CONFIRMED', '2026-11-25T07:00:00Z', 50), tour('d', 'PLANNED', '2026-11-25T07:00:00Z', 500)], today)
  expect(loads.map(({ mill, kg }) => [mill.name, kg])).toEqual([['A', 50], ['B', 0]])
})

it('filters orchards without a maturity apart from every level', () => {
  const rows = [{ maturity: 'GREEN' }, { maturity: null }, { maturity: null }].map((row) => ({ ...row, state: 'none', varieties: [], name: 'v', harvest: null }))
  expect(filterRows(rows, { ...NO_FILTERS, maturity: 'inconnue' })).toHaveLength(2)
  expect(filterRows(rows, { ...NO_FILTERS, maturity: 'GREEN' })).toHaveLength(1)
})
