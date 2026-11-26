import { getAllMaturityReports, getAllProblems, getAvailableEquipment, getAvailableTeamLeaders, getAvailableWorkers, getHarvestRequests, getOilMills, getOrchards, getTours } from '../api/planning.js'
import { rememberItem, rememberList } from './useLoad.js'

// Shared by the pages, so each list is loaded once and reused. The names are used by cache.invalidate
export const loadTours = rememberList('tours', () => getTours())
export const loadRequests = rememberList('requests', () => getHarvestRequests())
export const loadOrchards = rememberList('orchards', () => getOrchards())
export const loadMills = rememberList('mills', () => getOilMills())
export const loadProblems = rememberList('problems', () => getAllProblems())
export const loadMaturity = rememberList('maturity', () => getAllMaturityReports())

// Who is free during a period, "start|end". Use it through useFree, which checks it again before any decision
export const loadFree = rememberItem('free', async (key) => {
  const [start, end] = key.split('|')
  const [workers, equipment, leaders] = await Promise.all([getAvailableWorkers(start, end), getAvailableEquipment(start, end), getAvailableTeamLeaders(start, end)])
  return { workers, equipment, leaders }
})

// Several loads seen as one: { data: [..] or null, error, retry }
export function together(...loads) {
  const ready = loads.every(({ data }) => data !== null)
  return {
    data: ready ? loads.map(({ data }) => data) : null,
    error: loads.find(({ error }) => error)?.error ?? '',
    retry: () => loads.forEach(({ retry }) => retry()),
  }
}
