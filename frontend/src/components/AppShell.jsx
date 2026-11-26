import { useSyncExternalStore } from 'react'
import { NavLink, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext.jsx'
import { canManageResources, canPlan, roleLabel } from '../auth/roles.js'
import { retryStale, staleIds, subscribe } from '../planning/cache.js'
import { usePlanning } from '../planning/PlanningContext.jsx'
import Brand from './Brand.jsx'
import './AppShell.css'

// Only pages that exist. icon: paths of a 20 x 20 drawing
const PLANNING_PAGES = [
  { path: '/tableau-de-bord', label: 'Tableau de bord', icon: 'M3 3h6v8H3Zm8 0h6v5h-6Zm0 7h6v7h-6Zm-8 3h6v4H3Z' },
  { path: '/planification', label: 'Planification', icon: 'M7.5 3 2.5 5v12l5-2 5 2 5-2V3l-5 2-5-2Zm0 0v12m5-10v12' },
  { path: '/demandes', label: 'Demandes', icon: 'M6.5 4H4v13.5h12V4h-2.5M7 2.5h6V6H7Zm0 7h6m-6 3.5h4' },
  { path: '/tournees', label: 'Tournées', icon: 'M2 4.5h9.5v9H2Zm9.5 3h3.2l2.8 3v3h-6ZM5.5 16.5a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2Zm9 0a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2Z' },
  { path: '/signalements', label: 'Signalements', icon: 'M10 3 18 16.5H2Zm0 5v4m0 2.2v.1' },
  { path: '/ressources', label: 'Ressources', icon: 'M7 9.5a2.8 2.8 0 1 0 0-5.6 2.8 2.8 0 0 0 0 5.6Zm-5 7c0-3 2-5 5-5s5 2 5 5m1-7.2a2.5 2.5 0 1 0-1.2-4.7M15 11.9c1.9.6 3 2.3 3 4.6' },
  { path: '/pressoirs', label: 'Pressoirs', icon: 'M3 17V8l4.5 3V8l4.5 3V4h5v13Zm3.5-3.5h1m3.5 0h1' },
]
const DASHBOARD = PLANNING_PAGES[0].icon
const LOGISTICS_PAGES = [
  { path: '/logistique/tableau-de-bord', label: 'Tableau de bord', icon: DASHBOARD },
  { path: '/logistique/calendrier', label: 'Calendrier', icon: 'M3 4.5h14V17H3Zm0 4h14M6.5 2.5v4m7-4v4' },
  { path: '/logistique/ouvriers', label: 'Ouvriers', icon: PLANNING_PAGES[5].icon },
  { path: '/logistique/equipements', label: 'Équipements', icon: 'M2.5 12.5h3m5 0h3V8h-3L9 3.5H4.5v9M13.5 8h2.5v2M4.5 16.8a2.4 2.4 0 1 0 0-4.8 2.4 2.4 0 0 0 0 4.8Zm10.5 0a3.4 3.4 0 1 0 0-6.8 3.4 3.4 0 0 0 0 6.8Z' },
]
// The administrator has both spaces
const SWITCH_ICON = 'M3 7h13m-3-3 3 3-3 3M17 13H4m3-3-3 3 3 3'

function StaleNotice() {
  const count = useSyncExternalStore(subscribe, () => staleIds().length)
  if (count === 0) return null
  return <div className="stale" role="status">Données non actualisées.<button className="button-quiet" type="button" onClick={retryStale}>Réessayer</button></div>
}

export default function AppShell({ children }) {
  const { user, logout } = useAuth()
  const tour = usePlanning()?.tour // no planning for the Logistics Manager
  const both = canPlan(user.role) && canManageResources(user.role)
  const inLogistics = useLocation().pathname.startsWith('/logistique') || !canPlan(user.role)
  const pages = [...(inLogistics ? LOGISTICS_PAGES : PLANNING_PAGES), ...(both ? [inLogistics ? { path: '/tableau-de-bord', label: 'Espace collecte', icon: SWITCH_ICON } : { path: '/logistique/tableau-de-bord', label: 'Espace logistique', icon: SWITCH_ICON }] : [])]
  return (
    <div className="shell">
      <header className="menu">
        <Brand />
        <nav aria-label="Navigation principale">
          {pages.map(({ path, label, icon }) => (
            <NavLink key={path} to={path}>
              <svg viewBox="0 0 20 20" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true"><path d={icon} /></svg>
              <span>{label}</span>
              {path === '/planification' && tour && <span className="unsaved"><span className="visually-hidden">, tournée non enregistrée</span></span>}
            </NavLink>
          ))}
        </nav>
        <div className="account">
          <span className="account-name">{user.fullName}</span>
          <span>{roleLabel(user.role)}</span>
        </div>
        <button className="menu-logout" type="button" onClick={logout}>Se déconnecter</button>
      </header>
      {children}
      <StaleNotice />
    </div>
  )
}
