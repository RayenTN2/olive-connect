import { getCalendar, getEquipmentItem, getEquipmentStatus, getEquipmentTypes, getPeriods, getResources } from '../api/logistics.js'
import { rememberItem, rememberList } from '../planning/useLoad.js'

export const loadWorkers = rememberList('workers', () => getResources('workers'))
export const loadEquipment = rememberList('equipment', () => getResources('equipment'))
export const loadTypes = rememberList('equipment-types', () => getEquipmentTypes())
// key: "start|end"
export const loadCalendar = rememberItem('calendar', (key) => getCalendar(...key.split('|')))
// null = it doesn't exist. The official status comes with the item
export const loadEquipmentItem = rememberItem('equipment-item', async (id) => {
  try {
    const [item, status] = await Promise.all([getEquipmentItem(id), getEquipmentStatus(id)])
    return { item, status }
  } catch (failure) {
    if (failure.status === 404) return { item: null }
    throw failure
  }
})
// key: "workers|id" or "equipment|id"
export const loadPeriods = rememberItem('periods', (key) => getPeriods(...key.split('|')))
