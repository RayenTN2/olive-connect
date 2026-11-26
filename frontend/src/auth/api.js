// HTTP client for the backend. The access token lives in memory only; the refresh token is an
// HttpOnly cookie the browser sends to /api/auth/* (same origin through the Vite / Nginx proxy).

const REQUEST_TIMEOUT_MS = 15000
let accessToken = null
let onSessionLost = () => {}

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status // 0 = no response (network down, backend stopped)
  }
}

export function setAccessToken(token) {
  accessToken = token
}

export function onSessionExpired(callback) {
  onSessionLost = callback
}

async function send(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth && accessToken) headers.Authorization = `Bearer ${accessToken}`

  let response
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: 'same-origin',
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    })
  } catch (error) {
    // 408 = we gave up waiting; 0 = no response at all
    throw new ApiError(error.name === 'TimeoutError' ? 408 : 0, 'Network error')
  }

  const data = await response.json().catch(() => null)
  if (!response.ok) throw new ApiError(response.status, data?.message ?? '')
  return data
}

export function login(email, password) {
  return send('/api/auth/login', { method: 'POST', body: { email, password }, auth: false })
}

let refreshing = null // requests that find the token expired at the same moment share one refresh

export function refreshAccessToken() {
  refreshing ??= send('/api/auth/refresh-token', { method: 'POST', auth: false })
    .then((data) => {
      setAccessToken(data.accessToken)
      return data.accessToken
    })
    .finally(() => { refreshing = null })
  return refreshing
}

export function logout() {
  return send('/api/auth/logout', { method: 'POST', auth: false })
}

// Authenticated call: on 401 (access token expired after 15 min) refresh once and retry.
export async function request(path, options) {
  try {
    return await send(path, options)
  } catch (error) {
    if (error.status !== 401) throw error
    try {
      await refreshAccessToken()
    } catch {
      onSessionLost()
      throw error
    }
    return send(path, options)
  }
}
