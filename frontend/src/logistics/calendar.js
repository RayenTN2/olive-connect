import { addDays } from '../planning/dashboard.js'
import { normalize } from '../planning/orchards.js'
import { sortWith } from '../planning/sort.js'

export const STATUSES = { AVAILABLE: 'Disponible', IN_USE: 'En tournée', UNAVAILABLE: 'Indisponible', UNDER_MAINTENANCE: 'En maintenance', INACTIVE: 'Désactivé', UNKNOWN: 'Non vérifié' }
export const KINDS = { TOUR: 'Tournée', UNAVAILABLE: 'Indisponible', MAINTENANCE: 'Maintenance' }
export const TYPES = { BIN: 'Benne', TRACTOR: 'Tracteur', NET: 'Filet', VIBRATOR: 'Vibreur' }
const BUILT_IN = Object.keys(TYPES)
// Custom types have the label the Logistics Manager typed
export const typeLabel = (code, types) => TYPES[code] ?? types?.find((type) => type.code === code)?.label ?? code

// Tunisia is UTC+1 all year. "2026-11-25T07:00" is what a datetime-local field holds
export const toInstant = (local) => new Date(`${local}:00+01:00`)
export const toLocal = (instant) => new Date(Date.parse(instant) + 3600_000).toISOString().slice(0, 16)
const midnight = (day) => toInstant(`${day}T00:00`).toISOString()
export const daysKey = (from, count) => `${midnight(from)}|${midnight(addDays(from, count))}`

export const monday = (day) => addDays(day, -((new Date(`${day}T00:00:00Z`).getUTCDay() + 6) % 7))

const overlaps = (entry, start, end) => Date.parse(entry.start) < end && Date.parse(entry.end) > start
const holds = (tourStatus) => tourStatus === 'CONFIRMED' || tourStatus === 'IN_PROGRESS'

// Same order as the backend: maintenance, then unavailable, then running tour. Deactivated is told apart from a period
export function statusAt({ active, entries }, at) {
  if (!active) return 'INACTIVE'
  const now = entries.filter((entry) => overlaps(entry, at, at + 1))
  if (now.some(({ kind }) => kind === 'MAINTENANCE')) return 'UNDER_MAINTENANCE'
  if (now.some(({ kind }) => kind === 'UNAVAILABLE')) return 'UNAVAILABLE'
  return now.some(({ kind, tourStatus }) => kind === 'TOUR' && holds(tourStatus)) ? 'IN_USE' : 'AVAILABLE'
}

// { id: status } for a list. Without the calendar only deactivated ones are known
export function statuses(items, calendar, at) {
  const rows = new Map((calendar ?? []).map((row) => [row.id, row]))
  return Object.fromEntries(items.map(({ id, active }) => {
    const row = rows.get(id)
    return [id, !active ? 'INACTIVE' : row ? statusAt({ ...row, active }, at) : 'UNKNOWN']
  }))
}

// Entries of one day, with the hours they cover that day
export function dayEntries(entries, day) {
  const [start, end] = daysKey(day, 1).split('|').map((value) => Date.parse(value))
  return entries.filter((entry) => overlaps(entry, start, end)).map((entry) => {
    const from = Date.parse(entry.start) > start ? toLocal(entry.start).slice(11) : null
    const to = Date.parse(entry.end) < end ? toLocal(entry.end).slice(11) : null
    return { entry, hours: from && to ? `${from} à ${to}` : from ? `Dès ${from}` : to ? `Jusqu’à ${to}` : 'Toute la journée' }
  })
}

// The tours of a calendar, each once, with what it uses. Started ones first
export function toursOf(calendar) {
  const tours = new Map()
  for (const row of calendar) {
    for (const entry of row.entries) {
      if (entry.kind !== 'TOUR') continue
      const tour = tours.get(entry.tourId) ?? { id: entry.tourId, title: entry.tourTitle, status: entry.tourStatus, start: entry.start, end: entry.end, workers: 0, equipment: 0, }
      tour[row.resourceType === 'WORKER' ? 'workers' : 'equipment'] += 1
      tours.set(entry.tourId, tour)
    }
  }
  return [...tours.values()].sort((a, b) => Date.parse(a.start) - Date.parse(b.start))
}

// Absences and maintenances not over yet, soonest first
export const absences = (calendar, at) => calendar
  .flatMap((row) => row.entries.filter((entry) => entry.kind !== 'TOUR' && Date.parse(entry.end) > at).map((entry) => ({ row, entry })))
  .sort((a, b) => Date.parse(a.entry.start) - Date.parse(b.entry.start))

export const resourcePath = ({ resourceType, id }) => `/logistique/${resourceType === 'WORKER' ? 'ouvriers' : 'equipements'}/${encodeURIComponent(id)}`

const rank = (type) => (BUILT_IN.includes(type) ? BUILT_IN.indexOf(type) : BUILT_IN.length)
// Workers first, then equipment by type
export const typeRank = ({ resourceType, equipmentType }) => (resourceType === 'WORKER' ? -1 : rank(equipmentType))
export const byType = (a, b) => rank(a) - rank(b) || a.localeCompare(b, 'fr')

// ?semaine=2026-11-23&q=&type=WORKER|BIN...&etat=TOUR|UNAVAILABLE|MAINTENANCE
export function readCalendarFilters(params, today) {
  const week = params.get('semaine') ?? ''
  const valid = /^\d{4}-\d\d-\d\d$/.test(week) && !Number.isNaN(Date.parse(`${week}T00:00:00Z`))
  return { semaine: monday(valid ? week : today), q: params.get('q') ?? '', type: params.get('type') ?? '', etat: params.get('etat') ?? '' }
}

export const ROW_TESTS = Object.fromEntries(Object.keys(KINDS).map((kind) => [kind, ({ entries }) => entries.some((entry) => entry.kind === kind)]))

// Deactivated resources are shown only while something still uses them
export function searchCalendar(calendar, { q, type }) {
  const needle = normalize(q.trim())
  const found = calendar.filter((row) => (row.active || row.entries.length > 0)
    && (!type || (type === 'WORKER' ? row.resourceType === 'WORKER' : row.equipmentType === type))
    && (!needle || [row.name, row.reference].some((text) => text && normalize(text).includes(needle))))
  return sortWith(sortWith(found, ({ name }) => name, 'asc', () => 0), typeRank, 'asc', () => 0)
}
