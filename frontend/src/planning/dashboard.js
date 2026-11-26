import { bookedByMill } from './tour.js'
import { tunisDay } from './tours.js'

// Days are plain dates (2026-11-25), so adding never meets a time zone
export const addDays = (day, count) => new Date(Date.parse(`${day}T00:00:00Z`) + count * 86400_000).toISOString().slice(0, 10)
const dayOf = (tour) => (tour.plannedStartTime ? tunisDay(tour.plannedStartTime) : null)
const byStart = (a, b) => Date.parse(a.plannedStartTime) - Date.parse(b.plannedStartTime)

export const countPending = (requests) => requests.filter(({ status }) => status === 'PENDING').length
export const countUrgent = (requests) => requests.filter(({ status, requestType }) => status === 'PENDING' && requestType === 'URGENT').length
export const countReady = (rows) => rows.filter(({ state }) => state === 'eligible').length
export const countDrafts = (tours) => tours.filter(({ status }) => status === 'PLANNED').length
export const countLateDrafts = (tours, today) => tours.filter((tour) => tour.status === 'PLANNED' && dayOf(tour) !== null && dayOf(tour) < today).length
// Same as the list the number opens
export const countToExamine = (problems) => problems.filter(({ status }) => status === 'PENDING_REVIEW').length

// Today, then the 7 following days. Cancelled tours don't happen
export function coming(tours, today) {
  const kept = tours.filter((tour) => tour.status !== 'CANCELLED' && tour.plannedStartTime).sort(byStart)
  return {
    today: kept.filter((tour) => dayOf(tour) === today),
    next: kept.filter((tour) => dayOf(tour) > today && dayOf(tour) <= addDays(today, 7)),
  }
}

// level: ok below 80 %, high from 80 %, full at 100 %, over beyond
export function millLoads(mills, tours, day) {
  const booked = bookedByMill(tours, day)
  return mills.map((mill) => {
    const kg = booked[mill.id] ?? 0
    const share = mill.dailyCapacityKg > 0 ? kg / mill.dailyCapacityKg : kg > 0 ? Infinity : 0
    const level = share > 1 ? 'over' : share === 1 ? 'full' : share >= 0.8 ? 'high' : 'ok'
    return { mill, kg, share, level }
  }).sort((a, b) => b.share - a.share || a.mill.name.localeCompare(b.mill.name, 'fr'))
}
