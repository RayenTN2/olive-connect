import { expect, it } from 'vitest'
import { countByStatus, readFilters, searchTours, sortTours, tunisDay } from './tours.js'

const tour = (title, status, plannedStartTime, x = {}) => ({ title, status, plannedStartTime, teamLeaderName: 'Sami', stops: [{ orchardName: 'Verger Amel' }], dropOffs: [{ oilMillName: 'Pressoir Un' }], totalQuantityKg: 100, ...x })
const tours = [
  tour('Été', 'PLANNED', '2026-11-25T07:00:00Z', { totalQuantityKg: 300 }),
  tour('Bord', 'CONFIRMED', '2026-11-26T07:00:00Z', { teamLeaderName: null, stops: [{ orchardName: 'Nord' }, { orchardName: null }] }),
  tour('Ancienne', 'CONFIRMED', '2026-11-20T07:00:00Z', { dropOffs: [{ oilMillName: 'Gremda' }] }),
  tour('Sans date', 'CANCELLED', null),
]
const read = (query) => readFilters(new URLSearchParams(query))
const names = (query) => searchTours(tours, read(query), '2026-11-25').map(({ title }) => title)

it('reads filters from the address and ignores unknown values', () => {
  expect(read('')).toMatchObject({ statut: '', periode: '', tri: 'depart', sens: 'desc' })
  expect(read('tri=nom')).toMatchObject({ tri: 'nom', sens: 'asc' })
  expect(read('tri=x&sens=y&periode=z')).toMatchObject({ tri: 'depart', sens: 'desc', periode: '' })
})

it('searches name, leader, orchard and mill without accents', () => {
  expect(names('q=ete')).toEqual(['Été'])
  expect(names('q=nord')).toEqual(['Bord'])
  expect(names('q=GREMDA')).toEqual(['Ancienne'])
  expect(names('q=sami')).toEqual(['Été', 'Ancienne', 'Sans date'])
})

it('filters by period in Tunis time', () => {
  expect(names('periode=aujourd-hui')).toEqual(['Été'])
  expect(names('periode=a-venir')).toEqual(['Été', 'Bord'])
  expect(names('periode=passees')).toEqual(['Ancienne'])
  expect(names('periode=dates&du=2026-11-20&au=2026-11-25')).toEqual(['Été', 'Ancienne'])
  expect(names('periode=dates&du=2026-11-26')).toEqual(['Bord'])
  expect(tunisDay('2026-11-24T23:30:00Z')).toBe('2026-11-25')
})

it('counts by status and sorts both ways without changing the list', () => {
  expect(countByStatus(tours)).toEqual({ PLANNED: 1, CONFIRMED: 2, CANCELLED: 1 })
  const sorted = (query) => sortTours(tours, read(query)).map(({ title }) => title)
  expect(sorted('')).toEqual(['Bord', 'Été', 'Ancienne', 'Sans date'])
  expect(sorted('tri=nom')).toEqual(['Ancienne', 'Bord', 'Été', 'Sans date'])
  expect(sorted('tri=quantite&sens=desc')[0]).toBe('Été')
  expect(sorted('tri=arrets&sens=desc')[0]).toBe('Bord')
  expect(sorted('tri=statut')[0]).toBe('Été')
  expect(tours[0].title).toBe('Été')
})
