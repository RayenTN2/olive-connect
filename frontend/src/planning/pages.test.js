import { describe, expect, it } from 'vitest'
import { equipmentTypeLabel, formatKg } from './labels.js'
import { readMillFilters, searchMills, sortMills, toursOfMill } from './mills.js'
import { nextStatuses, readMaturityFilters, readProblemFilters, searchMaturity, searchProblems, sortMaturity, sortProblems } from './reports.js'
import { periodWindow, readPeriod, readTab, resourceTabs } from './resources.js'
import { problemError } from './saveErrors.js'

const params = (query) => new URLSearchParams(query)
const ids = (list) => list.map(({ id }) => id).join('')

describe('reports', () => {
  const problems = [
    { id: 'a', orchardName: 'Nord', problemType: 'DISEASE', description: 'Feuilles', status: 'PENDING_REVIEW', reportedAt: '2026-11-20T08:00:00Z' },
    { id: 'b', orchardName: 'Été', problemType: 'OTHER', description: null, status: 'RESOLVED', reportedAt: '2026-11-22T08:00:00Z' },
    { id: 'c', orchardName: null, problemType: 'DISEASE', description: 'Branche', status: 'UNDER_REVIEW', reportedAt: null },
  ]

  it('opens on what must be examined, newest first', () => {
    expect(readProblemFilters(params(''))).toMatchObject({ statut: 'PENDING_REVIEW', tri: 'recu', sens: 'desc' })
    expect(readProblemFilters(params('statut=toutes')).statut).toBe('')
    expect(ids(sortProblems(problems, readProblemFilters(params(''))))).toBe('bac')
    expect(ids(sortProblems(problems, readProblemFilters(params('tri=verger'))))).toBe('bac')
    expect(ids(sortProblems(problems, readProblemFilters(params('tri=statut'))))).toBe('acb')
  })

  it('searches orchard and description', () => {
    expect(ids(searchProblems(problems, { q: 'ete', type: '' }))).toBe('b')
    expect(ids(searchProblems(problems, { q: 'bran', type: 'DISEASE' }))).toBe('c')
  })

  it('never reopens a closed report', () => {
    expect(nextStatuses('PENDING_REVIEW')).toEqual(['UNDER_REVIEW', 'RESOLVED', 'REJECTED'])
    expect(nextStatuses('UNDER_REVIEW')).toEqual(['RESOLVED', 'REJECTED'])
    expect(nextStatuses('RESOLVED')).toEqual([])
    expect(nextStatuses('REJECTED')).toEqual([])
  })

  it('filters and sorts maturity reports', () => {
    const reports = [{ id: 'a', orchardName: 'Nord', maturityLevel: 'GREEN', reportedAt: '2026-11-22T08:00:00Z' }, { id: 'b', orchardName: 'Sud', maturityLevel: 'OVERRIPE', notes: 'vu', reportedAt: '2026-11-20T08:00:00Z' }, { id: 'c', orchardName: 'Sud', maturityLevel: null, reportedAt: '2026-11-21T08:00:00Z' }]
    expect(ids(searchMaturity(reports, { q: 'VU', maturite: '' }))).toBe('b')
    expect(ids(searchMaturity(reports, { q: '', maturite: 'GREEN' }))).toBe('a')
    expect(ids(sortMaturity(reports, readMaturityFilters(params('tri=maturite'))))).toBe('bac')
    expect(ids(sortMaturity(reports, readMaturityFilters(params(''))))).toBe('acb')
  })

  it('explains refusals in French', () => {
    expect(problemError(400, 'Problem report not found with id: x')).toContain('n’existe plus')
    expect(problemError(403, '')).toContain('compte')
    expect(problemError(400, 'raw')).toContain('a refusé')
  })
})

describe('mills', () => {
  const mills = [{ id: 'a', name: 'Gremda', address: 'Route 1', dailyCapacityKg: 100, active: true }, { id: 'b', name: 'Agareb', address: null, dailyCapacityKg: 300, active: false }, { id: 'c', name: 'Centre', dailyCapacityKg: 200, active: true }]
  const read = (query) => readMillFilters(params(query), '2026-11-25')

  it('searches and sorts; an inactive mill has nothing left to offer', () => {
    expect(read('').jour).toBe('2026-11-25')
    expect(ids(searchMills(mills, { q: 'route' }))).toBe('a')
    expect(ids(sortMills(mills, read(''), {}))).toBe('bca')
    expect(ids(sortMills(mills, read('tri=capacite'), {}))).toBe('bca')
    expect(ids(sortMills(mills, read('tri=reste'), { c: 150 }))).toBe('acb')
    expect(ids(sortMills(mills, read('tri=statut'), null))).toBe('acb')
  })

  it('lists its tours without the cancelled ones', () => {
    const tour = (id, status, start, drops) => ({ id, status, plannedStartTime: start, dropOffs: drops.map(([oilMillId, allocatedQuantityKg]) => ({ oilMillId, allocatedQuantityKg })) })
    const list = toursOfMill([tour('1', 'CONFIRMED', '2026-11-25T07:00:00Z', [['a', 10], ['b', 5]]), tour('2', 'PLANNED', '2026-11-27T07:00:00Z', [['a', 7]]), tour('3', 'CANCELLED', '2026-11-28T07:00:00Z', [['a', 7]]), tour('4', 'PLANNED', null, [['b', 7]])], 'a')
    expect(list.map(({ tour: { id }, kg }) => [id, kg])).toEqual([['2', 7], ['1', 10]])
  })
})

describe('resources', () => {
  it('reads the period in Tunis time, tomorrow by default', () => {
    expect(readPeriod(params(''), '2026-11-30')).toEqual({ jour: '2026-12-01', de: '07:00', a: '17:00' })
    expect(readPeriod(params('jour=x&de=9&a=12:30'), '2026-11-25')).toEqual({ jour: '2026-11-26', de: '07:00', a: '12:30' })
    expect(periodWindow({ jour: '2026-11-25', de: '07:00', a: '17:00' })).toEqual({ start: '2026-11-25T06:00:00.000Z', end: '2026-11-25T16:00:00.000Z' })
  })

  it('refuses an empty or impossible period', () => {
    expect(periodWindow({ jour: '2026-11-25', de: '10:00', a: '10:00' })).toBe(null)
    expect(periodWindow({ jour: '2026-11-25', de: '12:00', a: '10:00' })).toBe(null)
    expect(periodWindow({ jour: '2026-13-45', de: '07:00', a: '10:00' })).toBe(null)
  })
})

describe('free resources: one tab per kind', () => {
  const data = {
    leaders: [{ id: 'l', fullName: 'Leïla' }],
    workers: [{ id: 'b', fullName: 'Zied', phone: '1' }, { id: 'a', fullName: 'Ali' }],
    equipment: [{ id: 'e', type: 'NET', name: 'Filet', reference: 'F-1' }, { id: 'c', type: 'BIN', reference: 'BN-1', capacityKg: 500 }],
  }
  const tabs = resourceTabs(data, (type) => ({ BIN: 'Bennes', TRACTOR: 'Tracteurs', NET: 'Filets' })[type], (items) => items.reduce((sum, { capacityKg }) => sum + capacityKg, 0))

  it('keeps bins and tractors even when none is free, and sorts names', () => {
    expect(tabs.map(({ key, count }) => `${key} ${count}`)).toEqual(['chefs 1', 'ouvriers 2', 'BIN 1', 'TRACTOR 0', 'NET 1'])
    expect(tabs[1].items.map(({ name }) => name)).toEqual(['Ali', 'Zied'])
    expect(tabs[2]).toMatchObject({ capacity: 500, items: [{ name: 'BN-1', detail: formatKg(500) }] })
    expect(tabs[4].items[0].detail).toBe('F-1')
  })

  it('opens the workers unless the address names another tab', () => {
    expect(readTab(params(''), tabs)).toBe('ouvriers')
    expect(readTab(params('voir=BIN'), tabs)).toBe('BIN')
    expect(readTab(params('voir=VIBRATOR'), tabs)).toBe('ouvriers')
  })
})

it('reads a custom equipment type as words', () => {
  expect(equipmentTypeLabel('ESCALIER')).toBe('Escalier')
  expect(equipmentTypeLabel('PORTE_BENNE')).toBe('Porte benne')
  expect(equipmentTypeLabel('ÉCHELLES')).toBe('Échelles')
  expect(equipmentTypeLabel('BIN')).toBe('Bennes')
})
