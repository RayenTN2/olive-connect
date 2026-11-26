import { SESSION_EXPIRED } from '../auth/messages.js'
import { ACTION_NOT_DONE, ACTION_UNVERIFIED, isUncertain } from '../planning/saveErrors.js'

// A refusal already written for the user. current: the item as it is now, when someone else changed it
export class Refused extends Error {
  constructor(text, current) {
    super(text)
    this.current = current
  }
}

export const GONE = 'Cet élément n’existe plus.'
export const CHANGED = 'Cet élément a été modifié entre-temps. Vérifiez les nouvelles valeurs puis recommencez.'

// An answer holds finer instants than the database, so compare the millisecond
export const sameVersion = (a, b) => Date.parse(a.updatedAt) === Date.parse(b.updatedAt)

async function afterSilence(failure, read, find) {
  if (!isUncertain(failure.status)) throw failure
  let found
  try {
    found = find(await read())
  } catch {
    throw new Refused(ACTION_UNVERIFIED)
  }
  if (!found) throw new Refused(ACTION_NOT_DONE)
  return found
}

// The backend replaces every field without comparing: look first, so someone else's change isn't lost.
// read answers with the item or null
export async function saveChange({ base, read, send, matches }) {
  const before = await read()
  if (!before) throw new Refused(GONE)
  if (!sameVersion(before, base)) throw new Refused(CHANGED, before)
  try {
    return await send()
  } catch (failure) {
    return afterSilence(failure, read, (after) => (after && matches(after) ? after : null))
  }
}

// When the answer is lost, look for a new element before letting the user send it again
export async function saveNew({ list, send, matches }) {
  const known = new Set((await list()).map(({ id, code }) => id ?? code))
  try {
    return await send()
  } catch (failure) {
    return afterSilence(failure, list, (items) => items.find((item) => !known.has(item.id ?? item.code) && matches(item)))
  }
}

// The element is gone, whoever removed it: that's what was wanted
export async function saveRemoval({ list, send, id }) {
  try {
    await send()
  } catch (failure) {
    if (failure.status === 404) return
    await afterSilence(failure, list, (items) => items.every((item) => item.id !== id))
  }
}

export const sameFields = (body) => (item) => Object.entries(body).every(([key, value]) => (item[key] ?? '') === (value ?? ''))

// The backend's English refusals, in French
const REFUSALS = [
  [/^Reference is already in use|^This value is already in use/, 'Cette référence est déjà utilisée par un autre équipement.'],
  [/^Equipment type already exists/, 'Ce type existe déjà.'],
  [/^Label must contain/, 'Le nom du type doit contenir des lettres ou des chiffres.'],
  [/^Unknown equipment type/, 'Ce type d’équipement n’existe plus. Choisissez-en un autre.'],
  [/^Capacity is required|^capacityKg/, 'La capacité de la benne est obligatoire, en kg.'],
  [/^Capacity is only|^Registration number is only/, 'Un champ ne correspond pas au type choisi. Choisissez de nouveau le type.'],
  [/^Start time/, 'La fin doit être après le début.'],
  [/not found with id/, GONE],
]

export function logisticsError(failure) {
  if (failure instanceof Refused) return failure.message
  if (failure.status === 401) return SESSION_EXPIRED
  if (failure.status === 403) return 'Votre compte ne peut pas faire cette action.'
  if (failure.status === 404) return GONE
  if (isUncertain(failure.status)) return 'Le serveur n’a pas répondu. Rien n’a été envoyé. Réessayez dans un instant.'
  return REFUSALS.find(([pattern]) => pattern.test(failure.message))?.[1] ?? 'Le serveur a refusé l’enregistrement. Vérifiez les champs puis réessayez.'
}
