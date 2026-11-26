import { formatKg } from './labels.js'
import { addDays } from './dashboard.js'

const DAY = /^\d{4}-\d\d-\d\d$/
const TIME = /^\d\d:\d\d$/

// ?jour=2026-11-26&de=07:00&a=17:00, Tunis time. By default tomorrow, over the whole tour hours
export function readPeriod(params, today) {
  const pick = (key, pattern, fallback) => (pattern.test(params.get(key) ?? '') ? params.get(key) : fallback)
  return { jour: pick('jour', DAY, addDays(today, 1)), de: pick('de', TIME, '07:00'), a: pick('a', TIME, '17:00') }
}

// { start, end } for the backend, or null when the period is empty. Tunisia is UTC+1 all year
export function periodWindow({ jour, de, a }) {
  const start = new Date(`${jour}T${de}:00+01:00`)
  const end = new Date(`${jour}T${a}:00+01:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start >= end) return null
  return { start: start.toISOString(), end: end.toISOString() }
}

const ALWAYS = ['BIN', 'TRACTOR'] // shown even with none free: they decide whether a tour is possible
const named = (a, b) => a.name.localeCompare(b.name, 'fr', { numeric: true }) // Benne 2 before Benne 10

// One tab per kind of resource free during the period: { key, label, count, items: [{ id, name, detail, phone }] }
export function resourceTabs({ leaders, workers, equipment }, labelOf, kgOf) {
  const types = [...new Set([...ALWAYS, ...equipment.map(({ type }) => type)])]
  const rank = (type) => (ALWAYS.includes(type) ? ALWAYS.indexOf(type) : ALWAYS.length)
  types.sort((a, b) => rank(a) - rank(b) || labelOf(a).localeCompare(labelOf(b), 'fr'))
  return [
    { key: 'chefs', label: 'Chefs d’équipe', items: leaders.map(({ id, fullName }) => ({ id, name: fullName ?? 'Non renseigné' })) },
    { key: 'ouvriers', label: 'Ouvriers', items: workers.map(({ id, fullName, phone }) => ({ id, name: fullName ?? 'Non renseigné', phone })) },
    ...types.map((type) => {
      const items = equipment.filter((item) => item.type === type)
      return {
        key: type, label: labelOf(type), capacity: type === 'BIN' ? kgOf(items) : null,
        items: items.map((item) => ({ id: item.id, name: item.name || item.reference, detail: [item.name && item.reference, item.registrationNumber, item.capacityKg != null && formatKg(item.capacityKg)].filter(Boolean).join(', ') })),
      }
    }),
  ].map((tab) => ({ ...tab, count: tab.items.length, items: [...tab.items].sort(named) }))
}

// ?voir=ouvriers; an unknown or vanished tab falls back to the workers
export const readTab = (params, tabs) => (tabs.some(({ key }) => key === params.get('voir')) ? params.get('voir') : 'ouvriers')
