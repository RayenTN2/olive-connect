import { request } from '../auth/api.js'

const BASE = '/api/logistics'
const path = (kind, id) => `${BASE}/${kind}/${encodeURIComponent(id)}`

// kind: 'workers' | 'equipment', which share the same routes
export const getResources = (kind) => request(`${BASE}/${kind}`)
export const createResource = (kind, body) => request(`${BASE}/${kind}`, { method: 'POST', body })
export const updateResource = (kind, id, body) => request(path(kind, id), { method: 'PUT', body })
export const setResourceActive = (kind, id, active) => request(`${path(kind, id)}/status`, { method: 'PATCH', body: { active } })

export const getPeriods = (kind, id) => request(`${path(kind, id)}/unavailabilities`)
export const createPeriod = (kind, id, body) => request(`${path(kind, id)}/unavailabilities`, { method: 'POST', body })
export const updatePeriod = (kind, id, periodId, body) => request(`${path(kind, id)}/unavailabilities/${encodeURIComponent(periodId)}`, { method: 'PUT', body })
export const deletePeriod = (kind, id, periodId) => request(`${path(kind, id)}/unavailabilities/${encodeURIComponent(periodId)}`, { method: 'DELETE' })

export const getEquipmentItem = (id) => request(path('equipment', id))
export const getEquipmentStatus = (id) => request(`${path('equipment', id)}/availability`)
export const getEquipmentTypes = () => request(`${BASE}/equipment/types`)
export const createEquipmentType = (label) => request(`${BASE}/equipment/types`, { method: 'POST', body: { label } })
export const getCalendar = (start, end) => request(`${BASE}/calendar?${new URLSearchParams({ start, end })}`)
