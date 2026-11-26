// The tour being prepared. It lives in the browser until it is saved as a draft.

export const EMPTY_TOUR = {
  title: '', notes: '', date: '', time: '', leader: null,
  stopIds: [], // orchard ids, in visit order
  workers: [], equipment: [], // selected resources, kept whole so one that becomes unavailable can still be named
  dropOffs: [], // { mill, kg } where kg is the text typed
}

const toggle = (list, item) => (list.some(({ id }) => id === item.id) ? list.filter(({ id }) => id !== item.id) : [...list, item])

export function tourReducer(tour, action) {
  switch (action.type) {
    case 'load':
      return action.tour
    case 'discard':
      return null
    case 'addStop': {
      const base = tour ?? EMPTY_TOUR
      return base.stopIds.includes(action.id) ? base : { ...base, stopIds: [...base.stopIds, action.id] }
    }
    case 'removeStop':
      return { ...tour, stopIds: tour.stopIds.filter((id) => id !== action.id) }
    case 'moveStop': {
      const from = tour.stopIds.indexOf(action.id)
      const to = from + action.by
      if (from < 0 || to < 0 || to >= tour.stopIds.length) return tour
      const stopIds = [...tour.stopIds]
      stopIds.splice(to, 0, ...stopIds.splice(from, 1))
      return { ...tour, stopIds }
    }
    case 'set':
      return { ...tour, [action.field]: action.value }
    case 'toggleWorker':
      return { ...tour, workers: toggle(tour.workers, action.item) }
    case 'toggleEquipment':
      return { ...tour, equipment: toggle(tour.equipment, action.item) }
    case 'addMill': {
      const base = tour ?? EMPTY_TOUR
      const present = base.dropOffs.some(({ mill }) => mill.id === action.mill.id)
      return present ? base : { ...base, dropOffs: [...base.dropOffs, { mill: action.mill, kg: action.kg }] }
    }
    case 'setMillKg':
      return { ...tour, dropOffs: tour.dropOffs.map((item) => (item.mill.id === action.id ? { ...item, kg: action.kg } : item)) }
    case 'removeMill':
      return { ...tour, dropOffs: tour.dropOffs.filter(({ mill }) => mill.id !== action.id) }
    default:
      return tour
  }
}

// The backend needs the orchard's request approved and its coordinates known
export const canAddStop = (row) => row.state === 'eligible' && row.position !== null

// Body of the backend's tour creation. Call it only when no rule is left to complete
export function tourPayload(tour, stops) {
  return {
    title: tour.title.trim(),
    notes: tour.notes.trim(),
    teamLeaderId: tour.leader.id,
    plannedStartTime: startInstant(tour).toISOString(),
    harvestRequestIds: stops.map((row) => row.harvest.id),
    dropOffs: tour.dropOffs.map(({ mill, kg }) => ({ oilMillId: mill.id, allocatedQuantityKg: parseKg(kg) })),
    workerIds: tour.workers.map(({ id }) => id),
    equipmentIds: tour.equipment.map(({ id }) => id),
  }
}

// A saved tour back in the builder. Mills are the active ones: one that was deactivated leaves its quantity to reassign
export function toBuilder(saved, mills) {
  const tunis = new Date(Date.parse(saved.plannedStartTime) + 3600_000).toISOString()
  return {
    title: saved.title, notes: saved.notes ?? '', date: tunis.slice(0, 10), time: tunis.slice(11, 16),
    leader: saved.teamLeaderId ? { id: saved.teamLeaderId, fullName: saved.teamLeaderName ?? 'Chef d’équipe inconnu' } : null,
    stopIds: saved.stops.map(({ orchardId }) => orchardId),
    workers: saved.assignedWorkers, equipment: saved.assignedEquipment,
    dropOffs: saved.dropOffs.flatMap(({ oilMillId, allocatedQuantityKg }) => {
      const mill = mills.find(({ id }) => id === oilMillId)
      return mill ? [{ mill, kg: kgText(allocatedQuantityKg) }] : []
    }),
  }
}

const ACTIONS = { PLANNED: ['confirm', 'edit', 'leader', 'delete'], CONFIRMED: ['cancel'], IN_PROGRESS: ['cancel'] }
export const actionsFor = (status) => ACTIONS[status] ?? []

// Same rule as the backend: the estimated end, or start + 8 h without one
export function tourWindow({ plannedStartTime, estimatedEndTime }) {
  if (estimatedEndTime && Date.parse(estimatedEndTime) > Date.parse(plannedStartTime)) return { start: plannedStartTime, end: estimatedEndTime }
  return resourceWindow(new Date(plannedStartTime))
}

// Is this saved draft the one sent? Used when the server's answer was lost
export function matchesPayload(draft, payload) {
  const same = (a, b) => a.length === b.length && a.every((id, index) => id === b[index])
  return draft.title === payload.title && draft.teamLeaderId === payload.teamLeaderId &&
    Date.parse(draft.plannedStartTime) === Date.parse(payload.plannedStartTime) &&
    same(draft.stops.map(({ harvestRequestId }) => harvestRequestId), payload.harvestRequestIds) &&
    same(draft.dropOffs.map(({ oilMillId }) => oilMillId), payload.dropOffs.map(({ oilMillId }) => oilMillId)) &&
    same(draft.assignedWorkerIds, payload.workerIds) && same(draft.assignedEquipmentIds, payload.equipmentIds)
}

// Tunisia is UTC+1 all year
export function startInstant({ date, time }) {
  if (!date || !time) return null
  const start = new Date(`${date}T${time}:00+01:00`)
  return Number.isNaN(start.getTime()) ? null : start
}

// Before a tour has an estimated end, the backend checks resources over start + 8 h
export function resourceWindow(start) {
  return { start: start.toISOString(), end: new Date(start.getTime() + 8 * 3600_000).toISOString() }
}

// "1 250,5" → 1250.5; anything that isn't a plain number → NaN
export function parseKg(text) {
  const clean = text.replace(/\s/g, '').replace(',', '.')
  return /^\d+(\.\d+)?$/.test(clean) ? Number(clean) : NaN
}

export const totalKg = (stops) => stops.reduce((sum, row) => sum + (row.harvest.estimatedQuantityKg ?? 0), 0)

export function allocatedKg(dropOffs) {
  return dropOffs.reduce((sum, { kg }) => sum + (parseKg(kg) || 0), 0)
}

// Kilograms as the user would type them
export const kgText = (kg) => String(Math.round(kg * 1000) / 1000).replace('.', ',')

// What is left to allocate, ready to fill in a mill being added
export function remainingText(tour, stops) {
  const left = totalKg(stops) - allocatedKg(tour?.dropOffs ?? [])
  return left > 0 ? kgText(left) : ''
}

// The backend groups the mills' load by UTC day
export const utcDay = (instant) => new Date(instant).toISOString().slice(0, 10)

// Kilograms already promised to each mill that day: { millId: kg }. Drafts and cancelled tours reserve nothing
const RESERVING = ['CONFIRMED', 'IN_PROGRESS', 'COMPLETED']
export function bookedByMill(tours, day) {
  const booked = {}
  for (const tour of tours) {
    if (!RESERVING.includes(tour.status) || !tour.plannedStartTime || utcDay(tour.plannedStartTime) !== day) continue
    for (const { oilMillId, allocatedQuantityKg } of tour.dropOffs) booked[oilMillId] = (booked[oilMillId] ?? 0) + allocatedQuantityKg
  }
  return booked
}

export const millLeftKg = (mill, booked) => Math.max(0, mill.dailyCapacityKg - (booked[mill.id] ?? 0))

export function binCapacityKg(equipment) {
  return equipment.filter(({ type }) => type === 'BIN').reduce((sum, { capacityKg }) => sum + (capacityKg ?? 0), 0)
}

// Same formula as the backend's HarvestingDurationEstimator (25 kg per worker per hour, 15 min setup, 2 workers if none)
// shortcut: a copy, so it must follow the backend if the team changes the rate; the saved tour's times replace it
export function harvestMinutes(stops, workerCount) {
  const kgPerHour = (workerCount || 2) * 25
  return stops.reduce((sum, row) => {
    const kg = row.harvest.estimatedQuantityKg ?? 0
    return sum + (kg > 0 ? Math.max(15, Math.round((kg / kgPerHour) * 60) + 15) : 0)
  }, 0)
}
