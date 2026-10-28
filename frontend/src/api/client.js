const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const SESSION_KEY = 'form-fit-auth-session'

function getStoredSession() {
  try {
    return JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null')
  } catch {
    return null
  }
}

export function saveSession(session) {
  if (!session?.token || !['ADMIN', 'MEMBER', 'TRAINER'].includes(session.role)) {
    throw new ApiError('The authentication service returned an invalid session.')
  }
  window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function getSession() {
  return getStoredSession()
}

export function clearSession() {
  try {
    window.sessionStorage.removeItem(SESSION_KEY)
  } catch {
    // The UI can still return to the sign-in screen if storage is unavailable.
  }
}

export class ApiError extends Error {
  constructor(message, { status = 0, details = null, cause } = {}) {
    super(message, { cause })
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

function getErrorMessage(payload, fallback) {
  if (typeof payload === 'string' && payload.trim()) return payload
  if (!payload || typeof payload !== 'object') return fallback

  const directMessage = payload.message || payload.detail || payload.error
  if (typeof directMessage === 'string' && directMessage.trim()) return directMessage

  const fieldErrors = Object.entries(payload)
    .filter(([, value]) => typeof value === 'string')
    .map(([field, message]) => `${field}: ${message}`)

  return fieldErrors.length ? fieldErrors.join('; ') : fallback
}

export async function apiRequest(path, { method = 'GET', body, signal, headers = {} } = {}) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = `${API_BASE_URL}${normalizedPath}`
  const requestHeaders = new Headers(headers)
  requestHeaders.set('Accept', 'application/json')
  const session = getStoredSession()
  if (session?.token && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', 'Bearer ' + session.token)
  }

  const options = { method, headers: requestHeaders, signal }
  if (body !== undefined) {
    requestHeaders.set('Content-Type', 'application/json')
    options.body = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(url, options)
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    throw new ApiError('Could not reach the API Gateway. Check that it is running and try again.', { cause: error })
  }

  const responseText = await response.text()
  let payload = null
  if (responseText) {
    try {
      payload = JSON.parse(responseText)
    } catch (error) {
      throw new ApiError(
        response.ok ? 'The server returned an invalid response.' : `Request failed with status ${response.status}.`,
        { status: response.status, details: responseText, cause: error },
      )
    }
  }

  if (!response.ok) {
    if (response.status === 401 && !normalizedPath.startsWith('/auth/login')) {
      clearSession()
      window.dispatchEvent(new Event('gym-auth-expired'))
    }
    throw new ApiError(
      getErrorMessage(payload, `Request failed with status ${response.status}.`),
      { status: response.status, details: payload },
    )
  }

  return payload
}
