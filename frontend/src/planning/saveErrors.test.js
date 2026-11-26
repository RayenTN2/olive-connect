import { describe, expect, it } from 'vitest'
import { actionError, isStateChange, saveError, UNCERTAIN_SAVE } from './saveErrors.js'

const tour = { workers: [{ id: 'w1', fullName: 'Ali' }], equipment: [{ id: 'e1', reference: 'BN-1' }] }
const refused = (message, status = 400) => saveError(status, message, tour)

describe('saveError', () => {
  it.each([
    ["Team leader 'Sami Ben' is already assigned to an overlapping tour.", 'organisation', 'Sami Ben dirige déjà'],
    ["Team leader 'abc' is not active (status: SUSPENDED).", 'organisation', 'n’est plus disponible'],
    ["User 'abc' does not have role TEAM_LEADER.", 'organisation', 'n’est plus disponible'],
    ['Team leader user not found with id: abc', 'organisation', 'n’est plus disponible'],
    ["Worker 'Ali' (w1) is inactive or unavailable during [a, b).", 'resources', 'Ali n’est pas disponible'],
    ['Worker w1 is already assigned to an overlapping tour.', 'resources', 'Ali participe déjà'],
    ["Equipment 'BN-1' (e1) is inactive, under maintenance, or unavailable during [a, b).", 'resources', 'BN-1 n’est pas disponible'],
    ['Equipment e1 is already assigned to an overlapping tour.', 'resources', 'BN-1 est déjà utilisé'],
    ['Equipment not found with id: e1', 'resources', 'n’existe plus'],
    ['Insufficient bin capacity (100 kg) for estimated harvest quantity (200.0 kg).', 'resources', 'bennes'],
    ['Allocated oil mill drop-off quantity (1.0 kg) must equal the estimated harvest quantity (2.0 kg).', 'organisation', 'égale'],
    ['Tours must finish on the same day before or at 17:00 (Africa/Tunis). Start: 2026-11-25T08:00+01:00[Africa/Tunis], Estimated end: 2026-11-25T17:32+01:00[Africa/Tunis]', 'organisation', 'à 17:32'],
    ['Tours must finish on the same day before or at 17:00 (Africa/Tunis). Start: 2026-11-25T16:00+01:00[Africa/Tunis], Estimated end: 2026-11-26T01:10+01:00[Africa/Tunis]', 'organisation', 'après 17:00'],
    ["Daily capacity exceeded for oil mill 'Pressoir Un' on 2026-11-25: requested 300.0 kg + already booked 4800.0 kg > capacity 5000.0 kg", 'organisation', 'que 200'],
    ['Oil mill is inactive: Pressoir Un', 'organisation', 'Pressoir Un n’est plus actif'],
    ["Orchard 'X' (o1) does not have location coordinates set.", 'stops', 'coordonnées'],
  ])('%s', (message, section, text) => {
    const error = refused(message)
    expect(error.section).toBe(section)
    expect(error.text).toContain(text)
    expect(error.stale).toBe(false)
  })

  it('asks for fresh orchards when a stop changed', () => {
    expect(refused('Only APPROVED harvest requests can be added to a tour. Request r1 has status: SCHEDULED')).toMatchObject({ section: 'stops', stale: true })
    expect(refused('Harvest request not found with id: r1', 404).stale).toBe(true)
  })

  it('never shows unknown English text, and tells an unconfirmed save from a refusal', () => {
    expect(refused('E11000 duplicate key').text).toMatch(/^Le serveur a refusé/)
    for (const status of [0, 408, 500, 504]) expect(refused('', status).text).toBe(UNCERTAIN_SAVE)
    expect(refused('', 401).text).toContain('session')
  })
})

describe('actionError', () => {
  const saved = { assignedWorkers: tour.workers, assignedEquipment: tour.equipment }
  it.each([
    [400, "Cannot confirm: orchard 'Verger Amel' is already in the confirmed tour 'Tournée A'.", 'Verger Amel est déjà dans la tournée « Tournée A »'],
    [400, "Cannot confirm: the harvest request for orchard 'Verger Amel' is SCHEDULED, not APPROVED.", 'Verger Amel n’est plus à planifier'],
    [400, 'Cannot confirm: harvest request r1 no longer exists.', 'n’existe plus'],
    [400, "Team leader 'Sami' is already assigned to an overlapping tour.", 'Sami dirige déjà'],
    [400, 'Worker w1 is already assigned to an overlapping tour.', 'Ali participe déjà'],
    [400, 'Only PLANNED tours can be confirmed. Current status: CANCELLED', 'a changé'],
    [400, 'Tour is already cancelled.', 'a changé'],
    [404, 'Collection tour not found with id: t1', 'n’existe plus'],
    [403, 'Access Denied', 'ne peut pas faire'],
    [400, 'Raw English', 'a refusé cette action'],
  ])('%i %s', (status, message, text) => expect(actionError(status, message, saved)).toContain(text))

  it('knows when the tour must be read again', () => {
    expect(isStateChange(400, 'Only PLANNED tours can be deleted. Current status: CONFIRMED')).toBe(true)
    expect(isStateChange(400, "Cannot confirm: orchard 'A' is already in the confirmed tour 'B'.")).toBe(false)
  })
})
