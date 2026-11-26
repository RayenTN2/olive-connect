import { expect, it } from 'vitest'
import { actionsFor, bookedByMill, EMPTY_TOUR, matchesPayload, millLeftKg, remainingText, toBuilder, tourPayload, tourReducer, tourWindow, utcDay } from './tour.js'

const stops = [{ id: 'o2', harvest: { id: 'r2' } }, { id: 'o1', harvest: { id: 'r1' } }]
const tour = {
  ...EMPTY_TOUR, title: ' Nord ', date: '2026-11-25', time: '07:30', leader: { id: 'l1' },
  workers: [{ id: 'w1' }, { id: 'w2' }], equipment: [{ id: 'b1' }], dropOffs: [{ mill: { id: 'm1' }, kg: '1 250,5' }],
}
const payload = tourPayload(tour, stops)

it('sends request ids in visit order, numbers and a UTC start', () => {
  expect(payload).toEqual({
    title: 'Nord', notes: '', teamLeaderId: 'l1', plannedStartTime: '2026-11-25T06:30:00.000Z',
    harvestRequestIds: ['r2', 'r1'], dropOffs: [{ oilMillId: 'm1', allocatedQuantityKg: 1250.5 }], workerIds: ['w1', 'w2'], equipmentIds: ['b1'],
  })
})

it('recognises the draft whose answer was lost, and only that one', () => {
  // The backend writes the instant without milliseconds
  const draft = {
    title: 'Nord', teamLeaderId: 'l1', plannedStartTime: '2026-11-25T06:30:00Z', stops: [{ harvestRequestId: 'r2' }, { harvestRequestId: 'r1' }],
    dropOffs: [{ oilMillId: 'm1' }], assignedWorkerIds: ['w1', 'w2'], assignedEquipmentIds: ['b1'],
  }
  expect(matchesPayload(draft, payload)).toBe(true)
  for (const change of [{ title: 'Sud' }, { teamLeaderId: 'l2' }, { plannedStartTime: '2026-11-25T07:30:00Z' }, { stops: [...draft.stops].reverse() }, { dropOffs: [] }, { assignedWorkerIds: ['w1'] }, { assignedEquipmentIds: [] }]) {
    expect(matchesPayload({ ...draft, ...change }, payload)).toBe(false)
  }
})

it('puts a saved tour back in the builder and sends the same tour again', () => {
  const saved = {
    title: 'Nord', notes: null, plannedStartTime: '2026-11-25T06:30:00Z', teamLeaderId: 'l1', teamLeaderName: 'Leïla',
    stops: [{ orchardId: 'o2' }, { orchardId: 'o1' }], dropOffs: [{ oilMillId: 'm1', allocatedQuantityKg: 1250.5 }, { oilMillId: 'closed', allocatedQuantityKg: 10 }],
    assignedWorkers: [{ id: 'w1' }, { id: 'w2' }], assignedEquipment: [{ id: 'b1' }],
  }
  const back = toBuilder(saved, [{ id: 'm1' }])
  expect(back).toMatchObject({ date: '2026-11-25', time: '07:30', leader: { id: 'l1', fullName: 'Leïla' }, stopIds: ['o2', 'o1'] })
  expect(Object.keys(back).sort()).toEqual(Object.keys(EMPTY_TOUR).sort())
  expect(tourPayload(back, stops)).toEqual(payload)
})

it('allows only the actions the backend accepts for a status', () => {
  expect(actionsFor('PLANNED')).toEqual(['confirm', 'edit', 'leader', 'delete'])
  expect(actionsFor('CONFIRMED')).toEqual(['cancel'])
  expect(actionsFor('IN_PROGRESS')).toEqual(['cancel'])
  expect(actionsFor('COMPLETED')).toEqual([])
  expect(actionsFor('CANCELLED')).toEqual([])
})

it('uses the real window of a saved tour, or 8 hours without an end', () => {
  const start = '2026-11-25T06:30:00Z'
  expect(tourWindow({ plannedStartTime: start, estimatedEndTime: '2026-11-25T12:00:00Z' }).end).toBe('2026-11-25T12:00:00Z')
  expect(tourWindow({ plannedStartTime: start, estimatedEndTime: null }).end).toBe('2026-11-25T14:30:00.000Z')
})

it('starts a tour from a mill and never adds one twice', () => {
  const add = { type: 'addMill', mill: { id: 'm1' }, kg: '' }
  const tourFromMill = tourReducer(null, add)
  expect(tourFromMill.dropOffs).toHaveLength(1)
  expect(tourFromMill.stopIds).toEqual([])
  expect(tourReducer(tourFromMill, add).dropOffs).toHaveLength(1)
})

it('fills in the quantity still to allocate', () => {
  const rows = [{ harvest: { estimatedQuantityKg: 300.25 } }, { harvest: { estimatedQuantityKg: null } }]
  expect(remainingText(null, rows)).toBe('300,25')
  expect(remainingText({ dropOffs: [{ kg: '100' }] }, rows)).toBe('200,25')
  expect(remainingText({ dropOffs: [{ kg: '400' }] }, rows)).toBe('')
})

it('counts only the tours that reserve a mill, on the same UTC day', () => {
  const on = (status, plannedStartTime, kg) => ({ status, plannedStartTime, dropOffs: [{ oilMillId: 'm1', allocatedQuantityKg: kg }] })
  const booked = bookedByMill([
    on('CONFIRMED', '2026-11-27T07:00:00Z', 100), on('IN_PROGRESS', '2026-11-27T12:00:00Z', 20), on('COMPLETED', '2026-11-27T06:00:00Z', 3),
    on('PLANNED', '2026-11-27T07:00:00Z', 1000), on('CANCELLED', '2026-11-27T07:00:00Z', 1000), on('CONFIRMED', '2026-11-28T07:00:00Z', 1000), on('CONFIRMED', null, 1000),
  ], '2026-11-27')
  expect(booked).toEqual({ m1: 123 })
  expect(millLeftKg({ id: 'm1', dailyCapacityKg: 200 }, booked)).toBe(77)
  expect(millLeftKg({ id: 'm1', dailyCapacityKg: 100 }, booked)).toBe(0)
  expect(millLeftKg({ id: 'm2', dailyCapacityKg: 100 }, booked)).toBe(100)
  expect(utcDay('2026-11-27T23:30:00+01:00')).toBe('2026-11-27')
})
