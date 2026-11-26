import { describe, expect, it, vi } from 'vitest'
import { absences, dayEntries, daysKey, monday, readCalendarFilters, searchCalendar, statusAt, statuses, toInstant, toLocal, toursOf } from './calendar.js'
import { checkEquipment, checkPeriod, checkWorker, equipmentBody } from './forms.js'
import { freeByType, readEquipmentFilters, searchEquipment, sortResources } from './lists.js'
import { CHANGED, GONE, logisticsError, sameFields, saveChange, saveNew, saveRemoval } from './save.js'

const at = (text) => Date.parse(text)
const entry = (kind, start, end, more) => ({ kind, start, end, ...more })
const tour = (start, end, tourStatus = 'CONFIRMED', tourId = 't') => entry('TOUR', start, end, { tourStatus, tourId, tourTitle: 'Nord' })
const params = (query) => new URLSearchParams(query)

describe('status now: same order as the backend', () => {
  const now = at('2026-11-25T10:00:00Z')
  const [from, to] = ['2026-11-25T09:00:00Z', '2026-11-25T11:00:00Z']
  it.each([
    [[], 'AVAILABLE'],
    [[tour(from, to)], 'IN_USE'],
    [[tour(from, to, 'COMPLETED')], 'AVAILABLE'],
    [[tour(from, to), entry('UNAVAILABLE', from, to)], 'UNAVAILABLE'],
    [[entry('UNAVAILABLE', from, to), entry('MAINTENANCE', from, to)], 'UNDER_MAINTENANCE'],
    [[entry('MAINTENANCE', from, '2026-11-25T10:00:00Z')], 'AVAILABLE'], // the end is not included
    [[entry('MAINTENANCE', '2026-11-25T10:00:00Z', to)], 'UNDER_MAINTENANCE'], // the start is
  ])('%#', (entries, status) => expect(statusAt({ active: true, entries }, now)).toBe(status))

  it('never guesses without the calendar', () => {
    expect(statuses([{ id: 'a', active: true }, { id: 'b', active: false }], null, now)).toEqual({ a: 'UNKNOWN', b: 'INACTIVE' })
  })
})

describe('calendar', () => {
  it('works in Tunis time', () => {
    expect(daysKey('2026-11-25', 1)).toBe('2026-11-24T23:00:00.000Z|2026-11-25T23:00:00.000Z')
    expect(toLocal(toInstant('2026-11-25T07:30').toISOString())).toBe('2026-11-25T07:30')
    expect(monday('2026-11-25')).toBe('2026-11-23')
    expect(monday('2026-11-29')).toBe('2026-11-23')
    expect(readCalendarFilters(params('semaine=oups'), '2026-11-25').semaine).toBe('2026-11-23')
    expect(readCalendarFilters(params('semaine=2026-12-03'), '2026-11-25').semaine).toBe('2026-11-30')
  })

  it('cuts a long period by day', () => {
    const entries = [entry('UNAVAILABLE', '2026-11-25T13:00:00Z', '2026-11-27T11:00:00Z')]
    expect(['2026-11-24', '2026-11-25', '2026-11-26', '2026-11-27'].map((day) => dayEntries(entries, day)[0]?.hours)).toEqual([undefined, 'Dès 14:00', 'Toute la journée', 'Jusqu’à 12:00'])
  })

  const calendar = [
    { resourceType: 'EQUIPMENT', id: 'e', name: 'Benne', reference: 'BN', equipmentType: 'BIN', active: true, entries: [tour('2026-11-26T06:00:00Z', '2026-11-26T10:00:00Z')] },
    { resourceType: 'WORKER', id: 'w', name: 'Zied', active: true, entries: [tour('2026-11-26T06:00:00Z', '2026-11-26T10:00:00Z'), entry('UNAVAILABLE', '2026-11-20T06:00:00Z', '2026-11-21T06:00:00Z')] },
    { resourceType: 'WORKER', id: 'x', name: 'Ali', active: false, entries: [] },
  ]
  it('lists each tour once, hides unused deactivated ones, puts workers first', () => {
    expect(toursOf(calendar)).toMatchObject([{ id: 't', workers: 1, equipment: 1 }])
    expect(searchCalendar(calendar, { q: '', type: '' }).map(({ id }) => id)).toEqual(['w', 'e'])
    expect(searchCalendar(calendar, { q: 'bn', type: 'BIN' }).map(({ id }) => id)).toEqual(['e'])
    expect(absences(calendar, at('2026-11-25T00:00:00Z'))).toEqual([])
    expect(freeByType(calendar, () => 'AVAILABLE')).toMatchObject([{ key: 'WORKER', free: 1, total: 1 }, { key: 'BIN', free: 1, total: 1 }])
  })
})

describe('forms', () => {
  const bin = { code: 'BIN', fields: [{ name: 'capacityKg', required: true }] }
  const tractor = { code: 'TRACTOR', fields: [{ name: 'registrationNumber', required: false }] }
  const values = { name: ' Benne 1 ', reference: 'BN', type: 'BIN', capacityKg: '500', registrationNumber: 'TU 1' }

  it('sends only the fields of the type', () => {
    expect(equipmentBody(values, bin)).toMatchObject({ name: 'Benne 1', capacityKg: 500, registrationNumber: null })
    expect(equipmentBody(values, tractor)).toMatchObject({ capacityKg: null, registrationNumber: 'TU 1' })
    expect(equipmentBody(values, { code: 'NET', fields: [] })).toMatchObject({ capacityKg: null, registrationNumber: null })
  })
  it.each(['', '0', '-3', '1,5', 'abc', '99999999999'])('refuses the capacity "%s"', (capacityKg) => expect(checkEquipment({ ...values, capacityKg }, bin)).toHaveProperty('capacityKg'))
  it('accepts what the backend accepts', () => {
    expect(checkEquipment(values, bin)).toEqual({})
    expect(checkEquipment({ ...values, registrationNumber: '' }, tractor)).toEqual({})
    expect(checkWorker({ fullName: ' ', phone: '1' })).toHaveProperty('fullName')
    expect(checkPeriod({ start: '2026-11-25T07:00', end: '2026-11-25T07:00', reason: 'x' })).toHaveProperty('end')
    expect(checkPeriod({ start: '2026-11-25T07:00', end: '2026-11-25T08:00', reason: 'x' })).toEqual({})
  })
})

describe('lists', () => {
  const items = [{ id: 'a', name: 'B', type: 'NET', capacityKg: null }, { id: 'b', name: 'A', type: 'BIN', capacityKg: 5, serialNumber: 'Été' }]
  it('searches and sorts, missing capacities last', () => {
    expect(searchEquipment(items, { q: 'ete', type: '' })).toHaveLength(1)
    expect(sortResources(items, readEquipmentFilters(params('tri=capacite')), {}).map(({ id }) => id)).toEqual(['b', 'a'])
    expect(sortResources(items, readEquipmentFilters(params('tri=statut')), { a: 'AVAILABLE', b: 'INACTIVE' }).map(({ id }) => id)).toEqual(['a', 'b'])
  })
})

describe('saving', () => {
  const lost = { status: 0, message: '' }
  const base = { id: 'a', name: 'x', updatedAt: '2026-11-25T10:00:00.123456Z' }
  const stored = { ...base, updatedAt: '2026-11-25T10:00:00.123Z' }
  const matches = sameFields({ name: 'y' })

  it('never replaces someone else’s change, and says when it is gone', async () => {
    const send = vi.fn()
    await expect(saveChange({ base, read: async () => ({ ...stored, updatedAt: '2026-11-25T11:00:00Z' }), send, matches })).rejects.toMatchObject({ message: CHANGED, current: { id: 'a' } })
    await expect(saveChange({ base, read: async () => null, send, matches })).rejects.toThrow(GONE)
    expect(send).not.toHaveBeenCalled()
  })
  it('reads again when the answer is lost', async () => {
    let item = stored
    const done = async () => { item = { ...stored, name: 'y' }; throw lost }
    expect(await saveChange({ base, read: async () => item, send: done, matches })).toMatchObject({ name: 'y' })
    await expect(saveChange({ base, read: async () => stored, send: async () => { throw lost }, matches })).rejects.toThrow('n’est pas enregistrée')
  })
  it('finds a new element whose answer was lost, not an older twin', async () => {
    const twin = { id: 'old', name: 'y' }
    let items = [twin]
    expect(await saveNew({ list: async () => items, send: async () => { items = [twin, { id: 'new', name: 'y' }]; throw lost }, matches })).toMatchObject({ id: 'new' })
    items = [twin]
    await expect(saveNew({ list: async () => items, send: async () => { throw lost }, matches })).rejects.toThrow('n’est pas enregistrée')
  })
  it('takes an element already removed as removed', async () => {
    await saveRemoval({ list: async () => [], send: async () => { throw { status: 404 } }, id: 'a' })
    await saveRemoval({ list: async () => [], send: async () => { throw lost }, id: 'a' })
    await expect(saveRemoval({ list: async () => [{ id: 'a' }], send: async () => { throw lost }, id: 'a' })).rejects.toThrow()
  })
  it('never shows raw backend text', () => {
    expect(logisticsError({ status: 400, message: 'Reference is already in use: BN' })).toContain('référence')
    expect(logisticsError({ status: 400, message: 'This value is already in use.' })).toContain('déjà utilisée')
    expect(logisticsError({ status: 400, message: 'anything' })).toContain('refusé')
    expect(logisticsError({ status: 403, message: 'Access Denied' })).toContain('compte')
  })
})
