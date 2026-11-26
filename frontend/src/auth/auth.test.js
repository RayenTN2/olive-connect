import { describe, expect, it } from 'vitest'
import { loginErrorMessage, NETWORK_ERROR, SERVER_ERROR, TIMEOUT_ERROR } from './messages.js'
import { canEnter, homeFor, spaceRoleFor } from './roles.js'

describe('loginErrorMessage: every backend login refusal reads in French', () => {
  it.each([
    ['Invalid email or password.', 'incorrect'],
    ['Please verify your email address before logging in.', 'pas encore vérifiée'],
    ['Your account is pending administrator approval.', "attend la validation"],
    ['Your account has been suspended by the administrator.', 'suspendu'],
  ])('%s', (backend, french) => {
    expect(loginErrorMessage(401, backend)).toContain(french)
  })

  it('keeps the rejection reason, drops the empty placeholder', () => {
    expect(loginErrorMessage(401, 'Your account registration was rejected. Reason: Verger introuvable'))
      .toBe("Votre demande d'inscription a été refusée. Motif : Verger introuvable")
    expect(loginErrorMessage(401, 'Your account registration was rejected. Reason: No reason provided.'))
      .toBe("Votre demande d'inscription a été refusée.")
  })

  it('separates network and server failures from refusals', () => {
    expect(loginErrorMessage(0, '')).toBe(NETWORK_ERROR)
    expect(loginErrorMessage(408, '')).toBe(TIMEOUT_ERROR)
    expect(loginErrorMessage(502, '')).toBe(SERVER_ERROR)
  })

  it('never shows raw backend text, e.g. a database outage reported as 400', () => {
    expect(loginErrorMessage(400, 'Timed out while waiting for a server that matches ReadPreferenceServerSelector'))
      .toBe(SERVER_ERROR)
  })
})

describe('roles: routing follows SecurityConfig', () => {
  it('sends each role to its own space and unknown roles to access denied', () => {
    expect(homeFor('LOGISTICS_MANAGER')).toBe('/logistique')
    expect(homeFor('COLLECTION_MANAGER')).toBe('/collecte')
    expect(homeFor('VISITOR')).toBe('/acces-refuse')
  })

  it('lets a role into its own space only, and the administrator everywhere', () => {
    expect(canEnter('LOGISTICS_MANAGER', 'LOGISTICS_MANAGER')).toBe(true)
    expect(canEnter('LOGISTICS_MANAGER', 'COLLECTION_MANAGER')).toBe(false)
    expect(canEnter('FARMER', 'ADMIN')).toBe(false)
    expect(canEnter('ADMIN', 'MILL_MANAGER')).toBe(true)
    expect(canEnter('VISITOR', 'FARMER')).toBe(false)
  })

  it('maps deep links back to the space that owns them', () => {
    expect(spaceRoleFor('/logistique')).toBe('LOGISTICS_MANAGER')
    expect(spaceRoleFor('/logistique/ouvriers')).toBe('LOGISTICS_MANAGER')
    expect(spaceRoleFor('/logistiques')).toBe(null)
    expect(spaceRoleFor('/inconnu')).toBe(null)
  })
})
