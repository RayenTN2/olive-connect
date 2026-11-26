import { formatInstant, formatTime } from '../../planning/labels.js'

export const when = (instant) => `${formatInstant(instant)}, ${formatTime(instant)}`
export const initials = (name) => name.split(/\s+/).filter(Boolean).slice(0, 2).map((word) => word[0].toLocaleUpperCase('fr')).join('')
