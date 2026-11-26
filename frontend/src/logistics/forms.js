// Each check answers with { field: message }; an empty object means it can be sent

const need = (values, labels) => Object.fromEntries(Object.entries(labels).filter(([key]) => !String(values[key] ?? '').trim()).map(([key, text]) => [key, text]))
const trimmed = (values, keys) => Object.fromEntries(keys.map((key) => [key, String(values[key] ?? '').trim()]))
// "12,5" is how a French user types it
const toNumber = (text) => { const value = String(text ?? '').trim().replace(',', '.'); return value === '' ? NaN : Number(value) }

export const workerBody = (values) => trimmed(values, ['fullName', 'phone'])
export const checkWorker = (values) => need(values, { fullName: 'Saisissez le nom complet.', phone: 'Saisissez le numéro de téléphone.' })

const MAX_INT = 2147483647
export const hasField = (type, name) => type?.fields.some((field) => field.name === name) ?? false
// Fields of another type are never sent: the backend refuses them
export function equipmentBody(values, type) {
  return {
    ...trimmed(values, ['name', 'reference', 'type', 'brand', 'model', 'serialNumber']),
    registrationNumber: hasField(type, 'registrationNumber') ? String(values.registrationNumber ?? '').trim() : null,
    capacityKg: hasField(type, 'capacityKg') && String(values.capacityKg ?? '').trim() !== '' ? toNumber(values.capacityKg) : null,
  }
}
export function checkEquipment(values, type) {
  const errors = need(values, { name: 'Saisissez un nom.', reference: 'Saisissez une référence.', type: 'Choisissez un type.' })
  for (const { name, required } of type?.fields ?? []) {
    const filled = String(values[name] ?? '').trim() !== ''
    if (required && !filled) errors[name] = name === 'capacityKg' ? 'Saisissez la capacité de la benne.' : 'Ce champ est obligatoire.'
    else if (name === 'capacityKg' && filled) {
      const capacity = toNumber(values.capacityKg)
      if (!Number.isInteger(capacity) || capacity <= 0 || capacity > MAX_INT) errors.capacityKg = 'La capacité est un nombre entier de kg, supérieur à 0.'
    }
  }
  return errors
}

// start and end are "2026-11-25T07:00", so they compare as text
export function checkPeriod({ start, end, reason }) {
  const errors = need({ start, end, reason }, { start: 'Choisissez le début.', end: 'Choisissez la fin.', reason: 'Saisissez le motif.' })
  if (start && end && start >= end) errors.end = 'La fin doit être après le début.'
  return errors
}
