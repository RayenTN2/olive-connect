import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { clearCache } from '../planning/cache.js'
import * as api from './api.js'

const AuthContext = createContext(null)
const PROFILE_KEY = 'oliveconnect.profile'

// shortcut: the profile (email, name, role) is cached in localStorage because the backend has no
// "current user" endpoint and the refresh response carries only a token. It only chooses which
// screens to show; every API call is still authorised by the backend. Add GET /api/auth/me to drop it.
function readProfile() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY))
  } catch {
    return null
  }
}

function writeProfile(profile) {
  try {
    if (profile) localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
    else localStorage.removeItem(PROFILE_KEY)
  } catch {
    // storage unavailable (private mode): the session simply won't survive a reload
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readProfile) // known early, so the menu can show while the session is restored
  // loading (a saved session is being restored) | anonymous | authenticated
  const [status, setStatus] = useState(() => (readProfile() ? 'loading' : 'anonymous'))
  const [notice, setNotice] = useState(null) // message for the login page (e.g. session expired)

  const clear = useCallback((message = null) => {
    api.setAccessToken(null)
    clearCache() // nothing of this account may remain for the next one
    writeProfile(null)
    setUser(null)
    setStatus('anonymous')
    setNotice(message)
  }, [])

  // Restore the session after a reload: the refresh cookie gives a new access token.
  useEffect(() => {
    const saved = readProfile()
    if (!saved) return
    api.refreshAccessToken()
      .then(() => {
        setUser(saved)
        setStatus('authenticated')
      })
      .catch(() => clear())
  }, [clear])

  useEffect(() => {
    api.onSessionExpired(() => clear('expired'))
  }, [clear])

  const login = useCallback(async (email, password) => {
    const data = await api.login(email, password)
    clearCache()
    api.setAccessToken(data.token)
    const profile = { email: data.email, fullName: data.fullName, role: data.role }
    writeProfile(profile)
    setUser(profile)
    setNotice(null)
    setStatus('authenticated')
    return profile
  }, [])

  const logout = useCallback(async () => {
    // Clear locally even if the server can't be reached: the user asked to leave.
    await api.logout().catch(() => {})
    clear()
  }, [clear])

  const value = useMemo(() => ({ user, status, notice, login, logout }), [user, status, notice, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react/only-export-components
export function useAuth() {
  return useContext(AuthContext)
}
