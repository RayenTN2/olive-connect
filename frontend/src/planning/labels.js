export const STATES = {
  eligible: 'Prêt à planifier',
  pending: 'Demande à valider',
  scheduled: 'Déjà planifié',
  none: 'Sans demande',
}

export const TOUR_STATUSES = { PLANNED: 'Brouillon', CONFIRMED: 'Confirmée', IN_PROGRESS: 'En cours', COMPLETED: 'Terminée', CANCELLED: 'Annulée' }

export const REQUEST_STATUSES = {
  PENDING: 'À valider', APPROVED: 'Acceptée', SCHEDULED: 'Planifiée', IN_PROGRESS: 'En cours', COMPLETED: 'Terminée', REJECTED: 'Refusée', CANCELLED: 'Annulée',
}

// Checked for colour-blind readers (dataviz palette validator, all checks pass). The name is always written next to it
export const MATURITIES = {
  GREEN: { label: 'Verte', color: '#6f9a2e' },
  VERAISON: { label: 'Véraison', color: '#d0749f' },
  OPTIMAL: { label: 'Optimale', color: '#6b3aa0' },
  OVERRIPE: { label: 'Surmaturité', color: '#a3591c' },
}
const UNKNOWN_MATURITY = { label: 'Non renseignée', color: '#8d8f89' }
export const maturityOf = (level) => MATURITIES[level] ?? UNKNOWN_MATURITY

export const REQUEST_TYPES = { PLANNED: 'Planifiée', URGENT: 'Urgente' }

export const PRIORITIES = {
  CRITICAL: 'Critique',
  HIGH: 'Haute',
  STANDARD: 'Standard',
  URGENT_MISSING_MATURITY: 'Urgente, maturité inconnue',
  MISSING_MATURITY: 'Maturité inconnue',
}

export const PROBLEMS = {
  PEST_INFESTATION: 'Ravageurs',
  DISEASE: 'Maladie',
  HEAVY_RAIN: 'Fortes pluies',
  STRONG_WIND: 'Vent fort',
  WATER_SHORTAGE: 'Manque d’eau',
  EQUIPMENT_DAMAGE: 'Matériel endommagé',
  OTHER: 'Autre problème',
}
export const PROBLEM_STATUSES = { PENDING_REVIEW: 'À examiner', UNDER_REVIEW: 'En cours d’examen', RESOLVED: 'Résolu', REJECTED: 'Rejeté' }
export const isOpenProblem = (report) => report.status === 'PENDING_REVIEW' || report.status === 'UNDER_REVIEW'

const number = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 })
// The font has no visible narrow space (U+202F), which fr-FR uses between thousands
export const formatNumber = (value) => number.format(value).replace(/ /g, ' ')
export const formatKg = (value) => (value == null ? 'Non renseignée' : `${formatNumber(value)} kg`)

const day = { day: 'numeric', month: 'short', year: 'numeric' }
const instantFormat = new Intl.DateTimeFormat('fr-FR', { ...day, timeZone: 'Africa/Tunis' })
const dateFormat = new Intl.DateTimeFormat('fr-FR', { ...day, timeZone: 'UTC' })
// Instants are shown in Tunis time; a plain date (2026-11-25) has no time zone and must not shift
export const formatInstant = (value) => instantFormat.format(new Date(value))
const timeFormat = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit', timeZone: 'Africa/Tunis' })
export const formatTime = (value) => timeFormat.format(new Date(value))
const longDayFormat = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })
export const formatLongDay = (value) => longDayFormat.format(new Date(`${value}T00:00:00Z`))
export const formatDate = (value) => dateFormat.format(new Date(`${value}T00:00:00Z`))

export function formatMinutes(total) {
  const hours = Math.floor(total / 60)
  const minutes = total % 60
  if (hours === 0) return `${minutes} min`
  return minutes === 0 ? `${hours} h` : `${hours} h ${String(minutes).padStart(2, '0')}`
}

const EQUIPMENT_TYPES = { BIN: 'Bennes', TRACTOR: 'Tracteurs', NET: 'Filets', VIBRATOR: 'Vibreurs' }
// Custom types reach the Collection Manager only as a code ("PORTE_BENNE"): read it as "Porte benne"
const readableCode = (code) => {
  const words = code.replaceAll('_', ' ').toLocaleLowerCase('fr')
  return words.charAt(0).toLocaleUpperCase('fr') + words.slice(1)
}
export const equipmentTypeLabel = (type) => EQUIPMENT_TYPES[type] ?? readableCode(type)
// Bins and tractors first, as they decide whether a tour is feasible
export const equipmentTypes = (items) => {
  const known = Object.keys(EQUIPMENT_TYPES)
  const rank = (type) => (known.includes(type) ? known.indexOf(type) : known.length)
  return [...new Set(items.map(({ type }) => type))].sort((a, b) => rank(a) - rank(b) || a.localeCompare(b, 'fr'))
}
