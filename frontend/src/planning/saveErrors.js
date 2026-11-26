import { SESSION_EXPIRED } from '../auth/messages.js'
import { formatKg } from './labels.js'

// The draft may have been saved although no usable answer came back
export const isUncertain = (status) => status === 0 || status === 408 || status >= 500

export const UNCERTAIN_SAVE = 'Le serveur n’a pas confirmé l’enregistrement : le brouillon existe peut-être déjà. Réessayez, l’application vérifiera avant d’enregistrer de nouveau.'
export const UNVERIFIED_SAVE = 'Impossible de vérifier si le brouillon existe déjà. Rien n’a été renvoyé. Réessayez dans un instant.'

const nameOf = (list, id, key) => list.find((item) => item.id === id)?.[key]

// The backend's English refusals → [section to open, French text]. Built from the match and the tour sent.
const REFUSALS = [
  [/^Cannot confirm: orchard '(.+)' is already in the confirmed tour '(.+)'/, ([, orchard, other]) => ['stops', `${orchard} est déjà dans la tournée « ${other} ». Retirez ce verger ou annulez l’autre tournée.`, true]],
  [/^Cannot confirm: the harvest request for orchard '(.+)' is/, ([, orchard]) => ['stops', `La demande de récolte de ${orchard} n’est plus à planifier. Retirez ce verger de la tournée.`, true]],
  [/^Cannot confirm: harvest request/, () => ['stops', 'La demande de récolte d’un arrêt n’existe plus. Retirez-le de la tournée.', true]],
  [/^Cannot confirm a tour with no/, () => [null, 'La tournée doit avoir au moins un arrêt et un pressoir.']],
  [/^Team leader '(.+)' is already assigned/, ([, name]) => ['organisation', `${name} dirige déjà une autre tournée à cet horaire. Choisissez un autre chef d’équipe ou changez le départ.`]],
  [/^Team leader|TEAM_LEADER|^teamLeaderId/, () => ['organisation', 'Ce chef d’équipe n’est plus disponible. Choisissez-en un autre.']],
  [/^Worker '(.+)' \(/, ([, name]) => ['resources', `${name} n’est pas disponible à cet horaire. Choisissez un autre ouvrier.`]],
  [/^Worker (\S+) is already assigned/, ([, id], tour) => ['resources', `${nameOf(tour.workers, id, 'fullName') ?? 'Un ouvrier choisi'} participe déjà à une autre tournée à cet horaire.`]],
  [/^Equipment '(.+)' \(/, ([, reference]) => ['resources', `L’équipement ${reference} n’est pas disponible à cet horaire. Choisissez-en un autre.`]],
  [/^Equipment (\S+) is already assigned/, ([, id], tour) => ['resources', `L’équipement ${nameOf(tour.equipment, id, 'reference') ?? 'choisi'} est déjà utilisé par une autre tournée à cet horaire.`]],
  [/^(Worker|Equipment) not found/, () => ['resources', 'Une ressource choisie n’existe plus. Choisissez-en une autre.']],
  [/seasonal workers/, () => ['resources', 'Choisissez au moins 2 ouvriers.']],
  [/^Insufficient bin capacity/, () => ['resources', 'La capacité des bennes est inférieure à la quantité à récolter. Ajoutez des bennes.']],
  [/must equal the estimated harvest/, () => ['organisation', 'La quantité affectée aux pressoirs doit être égale à la quantité à récolter.']],
  [/cannot start before 07:00/, () => ['organisation', 'Une tournée ne peut pas partir avant 07:00.']],
  [/must finish on the same day/, (match) => {
    const end = match.input.match(/Start: (\d{4}-\d\d-\d\d)T.*Estimated end: \1T(\d\d:\d\d)/)
    return ['organisation', `La tournée se terminerait ${end ? `à ${end[2]}` : 'après 17:00'}, au-delà de la limite de 17:00. Partez plus tôt, ajoutez des ouvriers ou retirez un arrêt.`]
  }],
  [/^Daily capacity exceeded for oil mill '(.+)' on .*already booked ([\d.E]+) kg > capacity ([\d.E]+) kg/, ([, name, booked, capacity]) => ['organisation', `${name} ne peut plus recevoir que ${formatKg(Math.max(0, capacity - booked))} ce jour-là. Réduisez sa quantité ou ajoutez un autre pressoir.`]],
  [/^Oil mill is inactive: (.+)/, ([, name]) => ['organisation', `${name} n’est plus actif. Choisissez un autre pressoir.`]],
  [/^Oil mill not found/, () => ['organisation', 'Un pressoir choisi n’existe plus. Choisissez-en un autre.']],
  [/location coordinates/, () => ['stops', 'Un verger n’a pas de coordonnées. Retirez-le de la tournée.']],
  [/^Only APPROVED|^Harvest request not found|^Orchard not found/, () => ['stops', 'Un arrêt n’est plus à planifier. Les vergers ont été actualisés : retirez l’arrêt signalé dans les contrôles.', true]],
]

// section: to open (or null); stale: the orchards must be reloaded
export function saveError(status, message, tour) {
  if (isUncertain(status)) return { section: null, text: UNCERTAIN_SAVE, stale: false }
  if (status === 401) return { section: null, text: SESSION_EXPIRED, stale: false, known: true }
  if (status === 403) return { section: null, text: 'Votre compte ne peut pas enregistrer de tournée.', stale: false }
  for (const [pattern, explain] of REFUSALS) {
    const match = message.match(pattern)
    if (match) {
      const [section, text, stale = false] = explain(match, tour)
      return { section, text, stale, known: true }
    }
  }
  return { section: null, text: 'Le serveur a refusé la tournée. Vérifiez chaque section puis réessayez.', stale: false }
}

// The tour isn't in the state shown on screen any more
export const isStateChange = (status, message) => status === 404 || /^Only |^Tour is already|^Completed tours/.test(message)

export const ACTION_NOT_DONE = 'Le serveur n’a pas répondu. L’action n’est pas enregistrée pour l’instant : vérifiez l’état affiché puis réessayez.'
export const ACTION_UNVERIFIED = 'Le serveur n’a pas répondu et l’état actuel n’a pas pu être vérifié. Réessayez dans un instant.'
export const TOUR_GONE = 'Cette tournée n’existe plus.'

// Confirming, cancelling, deleting or changing the leader of a saved tour
export function actionError(status, message, tour) {
  if (status === 403) return 'Votre compte ne peut pas faire cette action.'
  if (status === 404 && /^Collection tour not found/.test(message)) return TOUR_GONE
  const error = saveError(status, message, { workers: tour.assignedWorkers, equipment: tour.assignedEquipment })
  if (error.known) return error.text
  return isStateChange(status, message) ? 'L’état de la tournée a changé entre-temps. Vérifiez l’état affiché.' : 'Le serveur a refusé cette action. Réessayez dans un instant.'
}

export const REQUEST_GONE = 'Cette demande n’existe plus.'
// The backend answers 400, not 404, for an unknown request
export const isRequestGone = (status, message) => status === 404 || /^Harvest request not found/.test(message)

// Accepting or refusing a harvest request
export function requestError(status, message) {
  if (status === 401) return SESSION_EXPIRED
  if (status === 403) return 'Votre compte ne peut pas décider de cette demande.'
  if (isRequestGone(status, message)) return REQUEST_GONE
  if (/^Only PENDING/.test(message)) return 'Cette demande a déjà été traitée. Vérifiez la décision affichée.'
  return 'Le serveur a refusé cette décision. Réessayez dans un instant.'
}

export const PROBLEM_GONE = 'Ce signalement n’existe plus.'
export const isProblemGone = (status, message) => status === 404 || /^Problem report not found/.test(message)

// Changing the status of a problem report
export function problemError(status, message) {
  if (status === 401) return SESSION_EXPIRED
  if (status === 403) return 'Votre compte ne peut pas traiter ce signalement.'
  if (isProblemGone(status, message)) return PROBLEM_GONE
  return 'Le serveur a refusé ce changement. Réessayez dans un instant.'
}
