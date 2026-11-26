import { describe, expect, it } from 'vitest'
import { checkTour, unavailableIds } from './rules.js'
import { canAddStop, EMPTY_TOUR, harvestMinutes, parseKg, resourceWindow, startInstant, tourReducer } from './tour.js'
import { formatMinutes } from './labels.js'

const stop = (id, kg) => ({ id, state: 'eligible', position: [34, 10], harvest: { estimatedQuantityKg: kg } })
const worker = (id) => ({ id, fullName: id })
const bin = (id, capacityKg) => ({ id, type: 'BIN', capacityKg })
const mill = (id, dailyCapacityKg = 5000) => ({ id, name: `Pressoir ${id}`, dailyCapacityKg })
const ready = {
  ...EMPTY_TOUR, title: 'Nord', date: '2026-11-25', time: '07:00', leader: { id: 'l1', fullName: 'Leïla' }, stopIds: ['a'],
  workers: [worker('w1'), worker('w2')], equipment: [bin('b1', 500)], dropOffs: [{ mill: mill('m1'), kg: '300' }],
}
const free = { workers: ready.workers, equipment: ready.equipment, leaders: [ready.leader] }
const check = (changes = {}, { stops = [stop('a', 300)], availability = free } = {}) =>
  Object.fromEntries(checkTour({ tour: { ...ready, ...changes }, stops, availability }).map((rule) => [rule.label, rule]))

describe('checkTour', () => {
  it('leaves only server checks on a complete tour', () => {
    const rules = check()
    const status = Object.fromEntries(Object.values(rules).map((rule) => [rule.label, rule.status]))
    expect(status).toEqual({
      'Arrêts': 'ok', 'Nom de la tournée': 'ok', 'Chef d’équipe': 'ok', 'Départ': 'ok', 'Fin avant 17:00': 'server',
      'Pressoirs': 'server', 'Ouvriers': 'ok', 'Bennes': 'ok', 'Disponibilité': 'ok',
    })
  })

  it('requires stops, a title and a departure', () => {
    expect(check({}, { stops: [] })['Arrêts'].status).toBe('todo')
    expect(check({ title: '   ' })['Nom de la tournée'].status).toBe('todo')
    expect(check({ time: '' })['Départ'].status).toBe('todo')
    expect(check({ time: '06:59' })['Départ'].detail).toContain('07:00')
  })

  it('requires a free team leader', () => {
    expect(check({ leader: null })['Chef d’équipe'].status).toBe('todo')
    expect(check({}, { availability: { ...free, leaders: [] } })['Chef d’équipe'].detail).toContain('Leïla')
    expect(check({}, { availability: null })['Chef d’équipe'].status).toBe('server')
  })

  it('refuses a stop that is no longer to be planned', () => {
    const rule = check({}, { stops: [{ ...stop('a', 300), name: 'Verger A', state: 'scheduled' }] })['Arrêts']
    expect(rule.status).toBe('todo')
    expect(rule.detail).toContain('Verger A')
  })

  it('warns about unknown quantities instead of inventing them', () => {
    expect(check({}, { stops: [stop('a', 300), stop('b', null)] })['Arrêts'].status).toBe('warn')
  })

  it('refuses a harvest that alone passes 17:00, at the exact limit', () => {
    // 2 workers, 487.5 kg: 585 + 15 min = 10 h, exactly 17:00
    expect(check({}, { stops: [stop('a', 487.5)] })['Fin avant 17:00'].status).toBe('server')
    expect(check({}, { stops: [stop('a', 500)] })['Fin avant 17:00'].status).toBe('todo')
    expect(check({ workers: ['1', '2', '3'].map(worker) }, { stops: [stop('a', 500)] })['Fin avant 17:00'].status).toBe('server')
  })

  it.each([
    ['', 'supérieure à 0'], ['0', 'supérieure à 0'], ['abc', 'supérieure à 0'],
    ['200', 'reste 100'], ['350', 'en trop'],
  ])('mill amount "%s" is refused', (kg, text) => {
    const rule = check({ dropOffs: [{ mill: mill('m1'), kg }] })['Pressoirs']
    expect(rule.status).toBe('todo')
    expect(rule.detail.replace(/ /g, ' ')).toContain(text)
  })

  it('checks mill capacity and adds several mills', () => {
    expect(check({ dropOffs: [{ mill: mill('m1', 200), kg: '300' }] })['Pressoirs'].detail).toContain('au plus')
    expect(check({ dropOffs: [{ mill: mill('m1'), kg: '0,1' }, { mill: mill('m2'), kg: '299,9' }] })['Pressoirs'].status).toBe('server')
  })

  it('warns, without blocking, when a mill has too little left that day', () => {
    const pressoirs = (booked) => Object.fromEntries(checkTour({ tour: ready, stops: [stop('a', 300)], availability: free, booked }).map((rule) => [rule.label, rule]))['Pressoirs']
    expect(pressoirs({ m1: 4800 }).status).toBe('warn')
    expect(pressoirs({ m1: 4700 }).status).toBe('server')
    expect(pressoirs(null).status).toBe('server')
  })

  it('requires two workers and enough bins, counting only bins', () => {
    expect(check({ workers: [worker('w1')] })['Ouvriers'].status).toBe('todo')
    expect(check({ equipment: [bin('b1', 299)] })['Bennes'].status).toBe('todo')
    expect(check({ equipment: [{ id: 't', type: 'TRACTOR', capacityKg: 900 }] })['Bennes'].status).toBe('todo')
  })

  it('reports resources lost after a change of time, and says when it could not check', () => {
    expect(check({}, { availability: { ...free, workers: [worker('w1')] } })['Disponibilité'].detail).toContain('1 ressource indisponible')
    expect(check({}, { availability: null })['Disponibilité'].status).toBe('server')
    expect([...unavailableIds(ready, { workers: [], equipment: [] })]).toEqual(['w1', 'w2', 'b1'])
  })
})

describe('tourReducer', () => {
  const run = (...actions) => actions.reduce(tourReducer, null)
  const add = (id) => ({ type: 'addStop', id })

  it('adds once, reorders inside bounds, removes, discards', () => {
    expect(run(add('a'), add('a'), add('b')).stopIds).toEqual(['a', 'b'])
    expect(run(add('a'), add('b'), add('c'), { type: 'moveStop', id: 'c', by: -1 }).stopIds).toEqual(['a', 'c', 'b'])
    expect(run(add('a'), add('b'), { type: 'moveStop', id: 'a', by: -1 }).stopIds).toEqual(['a', 'b'])
    expect(run(add('a'), add('b'), { type: 'moveStop', id: 'b', by: 1 }).stopIds).toEqual(['a', 'b'])
    expect(run(add('a'), { type: 'removeStop', id: 'a' }).stopIds).toEqual([])
    expect(run(add('a'), { type: 'discard' })).toBe(null)
  })

  it('toggles resources and edits mills', () => {
    const tour = run(add('a'), { type: 'toggleWorker', item: worker('w') }, { type: 'addMill', mill: mill('m'), kg: '1' }, { type: 'setMillKg', id: 'm', kg: '2' })
    expect(tour.workers).toHaveLength(1)
    expect(tour.dropOffs[0].kg).toBe('2')
    expect(tourReducer(tour, { type: 'toggleWorker', item: worker('w') }).workers).toEqual([])
    expect(tourReducer(tour, { type: 'removeMill', id: 'm' }).dropOffs).toEqual([])
  })
})

describe('helpers', () => {
  it('only accepts approved, located orchards', () => {
    expect(canAddStop(stop('a', 1))).toBe(true)
    expect(canAddStop({ ...stop('a', 1), position: null })).toBe(false)
    expect(canAddStop({ ...stop('a', 1), state: 'pending' })).toBe(false)
  })

  it('reads Tunis time and the backend window', () => {
    const start = startInstant({ date: '2026-11-25', time: '07:00' })
    expect(resourceWindow(start)).toEqual({ start: '2026-11-25T06:00:00.000Z', end: '2026-11-25T14:00:00.000Z' })
    expect(startInstant({ date: '', time: '07:00' })).toBe(null)
  })

  it('parses typed kilograms and formats durations', () => {
    expect(parseKg('1 250,5')).toBe(1250.5)
    expect(parseKg('1 250')).toBe(1250)
    expect(parseKg('-3')).toBeNaN()
    expect(parseKg('1e3')).toBeNaN()
    expect(harvestMinutes([stop('a', 300)], 2)).toBe(375)
    expect(harvestMinutes([stop('a', null)], 2)).toBe(0)
    expect(formatMinutes(375)).toBe('6 h 15')
    expect(formatMinutes(45)).toBe('45 min')
  })
})
