import { useEffect, useState } from 'react'
import { matchPath, Navigate, Outlet, Route, Routes } from 'react-router'
import { useAuth } from './auth/AuthContext.jsx'
import { canManageResources, canPlan } from './auth/roles.js'
import AppShell from './components/AppShell.jsx'
import PageLoading from './components/PageLoading.jsx'
import { PlanningProvider } from './planning/PlanningContext.jsx'
import LoginPage from './pages/LoginPage.jsx'
import SignedInPage from './pages/SignedInPage.jsx'

// Loaded on demand so the login page doesn't carry the map library
const PLANNING_PAGES = {
  '/tableau-de-bord': () => import('./pages/DashboardPage.jsx'),
  '/planification': () => import('./pages/PlanningPage.jsx'),
  '/tournees': () => import('./pages/ToursPage.jsx'),
  '/tournees/:id': () => import('./pages/TourPage.jsx'),
  '/demandes': () => import('./pages/RequestsPage.jsx'),
  '/demandes/:id': () => import('./pages/RequestPage.jsx'),
  '/signalements': () => import('./pages/ReportsPage.jsx'),
  '/signalements/:id': () => import('./pages/ReportPage.jsx'),
  '/ressources': () => import('./pages/ResourcesPage.jsx'),
  '/pressoirs': () => import('./pages/MillsPage.jsx'),
  '/pressoirs/:id': () => import('./pages/MillPage.jsx'),
}
const LOGISTICS_PAGES = {
  '/logistique/tableau-de-bord': () => import('./pages/logistics/LogisticsDashboardPage.jsx'),
  '/logistique/calendrier': () => import('./pages/logistics/CalendarPage.jsx'),
  '/logistique/ouvriers': () => import('./pages/logistics/WorkersPage.jsx'),
  '/logistique/ouvriers/:id': () => import('./pages/logistics/WorkerPage.jsx'),
  '/logistique/equipements': () => import('./pages/logistics/EquipmentPage.jsx'),
  '/logistique/equipements/:id': () => import('./pages/logistics/EquipmentItemPage.jsx'),
}
const PAGES = { ...PLANNING_PAGES, ...LOGISTICS_PAGES }
const pagesFor = (role) => Object.keys({ ...(canPlan(role) && PLANNING_PAGES), ...(canManageResources(role) && LOGISTICS_PAGES) })
const loaded = {} // path → page already fetched
const fetchPage = (path) => PAGES[path]().then((module) => { loaded[path] = module.default })

// The page being opened is fetched while the session is restored
const opened = Object.keys(PAGES).find((path) => matchPath(path, window.location.pathname))
if (opened) fetchPage(opened).catch(() => {})

const loading = <main className="page"><PageLoading /></main>

// Not React.lazy: Suspense keeps its fallback at least 0.3 s, which also delays the page's data
function Page({ path }) {
  const [, setReady] = useState(false)
  const [failed, setFailed] = useState(false)
  const Content = loaded[path]
  useEffect(() => {
    if (loaded[path]) return undefined
    let left = false
    fetchPage(path).then(() => !left && setReady(true), () => !left && setFailed(true))
    return () => { left = true }
  }, [path])
  if (Content) return <Content />
  if (!failed) return loading
  return <main className="page"><div className="page-loading" role="alert">La page ne se charge pas.<button className="button-quiet" type="button" onClick={() => window.location.reload()}>Réessayer</button></div></main>
}

// The menu stays while pages change or load
function Layout() {
  return <AppShell><Outlet /></AppShell>
}

export default function App() {
  const { status, user } = useAuth()
  const signedIn = status === 'authenticated'
  const role = user !== null && status !== 'anonymous' ? user.role : null
  const planner = canPlan(role)
  const paths = pagesFor(role)

  // Once signed in, fetch the pages' code in the background, so opening one doesn't wait for it
  useEffect(() => {
    if (!signedIn) return undefined
    const timer = setTimeout(() => pagesFor(role).filter((path) => !loaded[path]).forEach((path) => fetchPage(path).catch(() => {})), 1500)
    return () => clearTimeout(timer)
  }, [signedIn, role])

  // A saved session is being restored: the menu is known, the data isn't
  if (status === 'loading') {
    const shell = <AppShell>{loading}</AppShell>
    return planner ? <PlanningProvider>{shell}</PlanningProvider> : paths.length > 0 ? shell : null
  }

  let home = <Navigate to="/connexion" replace />
  if (planner) home = <Navigate to="/tableau-de-bord" replace />
  else if (paths.length > 0) home = <Navigate to="/logistique/tableau-de-bord" replace />
  else if (signedIn) home = <SignedInPage />

  const pages = (
    <Routes>
      <Route path="/connexion" element={signedIn ? <Navigate to="/" replace /> : <LoginPage />} />
      {paths.length > 0 && <Route element={<Layout />}>{paths.map((path) => <Route key={path} path={path} element={<Page key={path} path={path} />} />)}</Route>}
      {canManageResources(role) && <Route path="/logistique" element={<Navigate to="/logistique/tableau-de-bord" replace />} />}
      <Route path="/" element={home} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
  // Signing out unmounts the provider: the next account never sees this one's unsaved tour
  return planner ? <PlanningProvider>{pages}</PlanningProvider> : pages
}
