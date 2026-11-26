import { MATURITIES, PROBLEMS } from './labels.js'
import { normalize } from './orchards.js'
import { readSort, sortWith, time } from './sort.js'

export const DESCENDING_FIRST = ['recu', 'maturite']
const matches = (texts, q) => {
  const needle = normalize(q.trim())
  return !needle || texts.some((text) => text && normalize(text).includes(needle))
}

// --- problems: ?statut=RESOLVED&type=DISEASE&q=...&tri=verger&sens=asc. "toutes" shows every status

export function readProblemFilters(params) {
  const statut = params.get('statut') ?? 'PENDING_REVIEW'
  return { q: params.get('q') ?? '', statut: statut === 'toutes' ? '' : statut, type: params.get('type') ?? '', ...readSort(params, ['verger', 'type', 'recu', 'statut'], 'recu', DESCENDING_FIRST) }
}

export const searchProblems = (reports, { q, type }) => reports.filter((report) => (!type || report.problemType === type) && matches([report.orchardName, report.description], q))

const STATUS_ORDER = ['PENDING_REVIEW', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED']
const PROBLEM_VALUE = {
  verger: ({ orchardName }) => orchardName,
  type: ({ problemType }) => PROBLEMS[problemType] ?? problemType,
  recu: ({ reportedAt }) => time(reportedAt),
  statut: ({ status }) => STATUS_ORDER.indexOf(status),
}
export const sortProblems = (reports, { tri, sens }) => sortWith(reports, PROBLEM_VALUE[tri], sens, (report) => -(time(report.reportedAt) ?? 0))

// The backend would accept reopening; a closed report stays closed here
const NEXT = { PENDING_REVIEW: ['UNDER_REVIEW', 'RESOLVED', 'REJECTED'], UNDER_REVIEW: ['RESOLVED', 'REJECTED'] }
export const nextStatuses = (status) => NEXT[status] ?? []

// --- maturity reports: ?onglet=maturite&maturite=OPTIMAL&q=...

export function readMaturityFilters(params) {
  return { q: params.get('q') ?? '', maturite: params.get('maturite') ?? '', ...readSort(params, ['verger', 'maturite', 'recu'], 'recu', DESCENDING_FIRST) }
}

export const searchMaturity = (reports, { q, maturite }) => reports.filter((report) => (!maturite || report.maturityLevel === maturite) && matches([report.orchardName, report.notes], q))

const MATURITY_VALUE = {
  verger: ({ orchardName }) => orchardName,
  maturite: ({ maturityLevel }) => (maturityLevel in MATURITIES ? Object.keys(MATURITIES).indexOf(maturityLevel) : null),
  recu: ({ reportedAt }) => time(reportedAt),
}
export const sortMaturity = (reports, { tri, sens }) => sortWith(reports, MATURITY_VALUE[tri], sens, (report) => -(time(report.reportedAt) ?? 0))
