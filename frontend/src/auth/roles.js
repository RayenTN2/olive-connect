// One space per backend role (entity/Role). VISITOR has no space: it can only request registration.
export const ROLE_SPACES = {
  FARMER: { path: '/agriculteur', label: 'Espace agriculteur', role: 'Agriculteur' },
  COLLECTION_MANAGER: { path: '/collecte', label: 'Espace collecte', role: 'Responsable de collecte' },
  LOGISTICS_MANAGER: { path: '/logistique', label: 'Espace logistique', role: 'Responsable logistique' },
  TEAM_LEADER: { path: '/chef-equipe', label: "Espace chef d'équipe", role: "Chef d'équipe de récolte" },
  MILL_MANAGER: { path: '/pressoir', label: 'Espace pressoir', role: 'Responsable du pressoir' },
  ADMIN: { path: '/admin', label: 'Administration', role: 'Administrateur' },
}

export function homeFor(role) {
  return ROLE_SPACES[role]?.path ?? '/acces-refuse'
}

// The role whose space owns this path (or a sub-path of it), null otherwise
export function spaceRoleFor(path) {
  const entry = Object.entries(ROLE_SPACES).find(([, space]) => path === space.path || path.startsWith(`${space.path}/`))
  return entry ? entry[0] : null
}

export function roleLabel(role) {
  return ROLE_SPACES[role]?.role ?? role
}

// Mirrors SecurityConfig: each namespace admits its role plus ADMIN. The backend still decides; this only routes.
export function canEnter(role, spaceRole) {
  return role === spaceRole || (role === 'ADMIN' && spaceRole in ROLE_SPACES)
}

// Same roles as the backend's /api/logistics endpoints
export function canManageResources(role) {
  return role === 'LOGISTICS_MANAGER' || role === 'ADMIN'
}

// Same roles as the backend's tour endpoints
export function canPlan(role) {
  return role === 'COLLECTION_MANAGER' || role === 'ADMIN'
}
