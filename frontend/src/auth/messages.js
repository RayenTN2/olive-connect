// The backend answers in English; users only ever see French.
const LOGIN_MESSAGES = [
  ['Invalid email or password', 'Adresse e-mail ou mot de passe incorrect.'],
  ['verify your email', "Votre adresse e-mail n'est pas encore vérifiée. Confirmez-la avant de vous connecter."],
  ['pending administrator approval', "Votre compte attend la validation d'un administrateur. Vous pourrez vous connecter dès qu'il sera approuvé."],
  ['suspended', 'Votre compte a été suspendu par un administrateur. Contactez la coopérative.'],
]

export const NETWORK_ERROR = 'Impossible de joindre le serveur. Vérifiez votre connexion puis réessayez.'
export const SERVER_ERROR = 'Le serveur a rencontré un problème. Réessayez dans un instant.'
export const TIMEOUT_ERROR = 'Le serveur met trop de temps à répondre. Réessayez dans un instant.'
export const SESSION_EXPIRED = 'Votre session a expiré. Reconnectez-vous.'

const REJECTED_PREFIX = 'Your account registration was rejected. Reason: '

// status: HTTP status of the failed login (0 = no response); message: the backend's `message` field
export function loginErrorMessage(status, message = '') {
  if (status === 0) return NETWORK_ERROR
  if (status === 408) return TIMEOUT_ERROR
  if (status >= 500) return SERVER_ERROR

  if (message.startsWith(REJECTED_PREFIX)) {
    const reason = message.slice(REJECTED_PREFIX.length).trim()
    const shownReason = reason && reason !== 'No reason provided.' ? ` Motif : ${reason}` : ''
    return `Votre demande d'inscription a été refusée.${shownReason}`
  }
  const known = LOGIN_MESSAGES.find(([needle]) => message.includes(needle))
  if (known) return known[1]
  // Anything else is not the user's doing: the form validates its fields before sending, and the
  // backend reports infrastructure failures (e.g. database unreachable) as 400 with a raw message.
  return SERVER_ERROR
}

// Loading planning data. Backend texts are English, so only the status is used.
export function loadErrorMessage(status) {
  if (status === 0) return NETWORK_ERROR
  if (status === 408) return TIMEOUT_ERROR
  if (status === 403) return "Votre compte n'a pas accès à ces données."
  return SERVER_ERROR
}
